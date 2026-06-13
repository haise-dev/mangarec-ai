"""MangaDex ETL Ingestion Script.

Extracts manga metadata and statistics from MangaDex,
Transforms it into SQLite relational models and embedding text,
Loads it into Qdrant and SQLite in batches.
"""

import logging
import os
import sys
import time
from datetime import datetime
from typing import Any

import requests
from qdrant_client.http.models import PointStruct
from sentence_transformers import SentenceTransformer
from sqlalchemy.orm import Session

# Ensure we can import app modules
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from tenacity import retry, retry_if_exception_type, stop_after_attempt, wait_exponential

from app.core.config import settings
from app.core.qdrant import COLLECTION_NAME, get_qdrant_client, init_qdrant
from app.db.models import (
    Author,
    EtlCheckpoint,
    Manga,
    MangaAltTitle,
    MangaAuthor,
    MangaTag,
    RelatedManga,
    Tag,
)
from app.db.session import SessionLocal, init_db

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("ingest")

API_BASE_URL = "https://api.mangadex.org"


def send_alert(message: str) -> None:
    """Send alert via Webhook if configured."""
    url = settings.ALERT_WEBHOOK_URL
    if not url:
        return
    try:
        requests.post(url, json={"content": f"🚨 [MangaRec ETL] {message}"}, timeout=10)
    except Exception as e:
        logger.warning(f"Failed to send alert webhook: {e}")


def _log_retry(retry_state):
    logger.warning(f"Fetch failed: {retry_state.outcome.exception()}. Retrying in {retry_state.next_action.sleep}s (Attempt {retry_state.attempt_number}/5)")


@retry(
    stop=stop_after_attempt(5),
    wait=wait_exponential(multiplier=2, min=3, max=60),
    retry=retry_if_exception_type((requests.exceptions.RequestException, ConnectionError)),
    before_sleep=_log_retry,
    reraise=True,
)
def fetch_manga(limit: int = 100, offset: int = 0, order_by: str = "followedCount", order_dir: str = "desc", updated_since: str = None) -> list[dict[str, Any]]:
    """Extract: Fetch a batch of manga with related entities."""
    params = {
        "limit": limit,
        "offset": offset,
        "includes[]": ["author", "artist", "cover_art", "manga"],
        f"order[{order_by}]": order_dir,
        "hasAvailableChapters": "true",
        "originalLanguage[]": ["ja"],
    }
    if updated_since:
        params["updatedAtSince"] = updated_since

    resp = requests.get(f"{API_BASE_URL}/manga", params=params, timeout=30)
    resp.raise_for_status()
    return resp.json().get("data", [])


@retry(
    stop=stop_after_attempt(5),
    wait=wait_exponential(multiplier=2, min=3, max=60),
    retry=retry_if_exception_type((requests.exceptions.RequestException, ConnectionError)),
    before_sleep=_log_retry,
    reraise=True,
)
def fetch_statistics(manga_ids: list[str]) -> dict[str, Any]:
    """Extract: Fetch statistics for a batch of manga IDs."""
    if not manga_ids:
        return {}
    params = {"manga[]": manga_ids}
    resp = requests.get(f"{API_BASE_URL}/statistics/manga", params=params, timeout=30)
    resp.raise_for_status()
    return resp.json().get("statistics", {})


def transform_to_models(
    manga_dict: dict[str, Any], stats_dict: dict[str, Any]
) -> tuple[Manga, list[Tag], list[Author], list[RelatedManga]]:
    """Transform: Parse MangaDex JSON into SQLAlchemy ORM objects."""
    mid = manga_dict["id"]
    attrs = manga_dict.get("attributes", {})

    # 1. Base Manga fields
    title_main = attrs.get("title", {}).get("en") or next(
        iter(attrs.get("title", {}).values()), "Unknown"
    )

    manga = Manga(
        id=mid,
        title_main=title_main,
        original_language=attrs.get("originalLanguage"),
        demographic=attrs.get("publicationDemographic"),
        status=attrs.get("status", "unknown"),
        content_rating=attrs.get("contentRating", "safe"),
        year=attrs.get("year"),
        has_en_translation="en" in attrs.get("availableTranslatedLanguages", []),
        has_vi_translation="vi" in attrs.get("availableTranslatedLanguages", []),
        created_at=datetime.fromisoformat(attrs["createdAt"])
        if attrs.get("createdAt")
        else None,
        updated_at=datetime.fromisoformat(attrs["updatedAt"])
        if attrs.get("updatedAt")
        else None,
        description_en=attrs.get("description", {}).get("en"),
        description_vi=attrs.get("description", {}).get("vi"),
    )

    # Statistics
    stats = stats_dict.get(mid, {})
    rating = stats.get("rating", {})
    manga.rating_bayesian = rating.get("bayesian")
    manga.rating_average = rating.get("average")
    manga.follows = stats.get("follows")

    # Relationships from 'includes'
    tags = []
    authors = []
    related = []

    # Alt Titles
    for alt in attrs.get("altTitles", []):
        for lang, t in alt.items():
            if lang in ("en", "vi", "ja-ro", "ko-ro"):
                manga.alt_titles.append(
                    MangaAltTitle(manga_id=mid, title=t, language=lang)
                )

    # Tags embedded in attributes
    for t in attrs.get("tags", []):
        tid = t["id"]
        tname = t["attributes"]["name"].get("en", "Unknown")
        tgroup = t["attributes"].get("group", "theme")
        tags.append(Tag(id=tid, name_en=tname, group_type=tgroup))
        manga.tags.append(MangaTag(manga_id=mid, tag_id=tid))

    for rel in manga_dict.get("relationships", []):
        rtype = rel["type"]
        rid = rel["id"]
        if rtype in ("author", "artist"):
            name = rel.get("attributes", {}).get("name")
            if name:
                authors.append(Author(id=rid, name=name))
                manga.authors.append(
                    MangaAuthor(manga_id=mid, author_id=rid, role=rtype)
                )
        elif rtype == "cover_art":
            filename = rel.get("attributes", {}).get("fileName")
            if filename:
                manga.cover_url = f"https://uploads.mangadex.org/covers/{mid}/{filename}"
        elif rtype == "manga":
            related_type = rel.get("related", "unknown")
            related.append(
                RelatedManga(
                    manga_id=mid, related_manga_id=rid, relation_type=related_type
                )
            )

    return manga, tags, authors, related


def compose_embedding_text(manga: Manga, tags: list[Tag]) -> str:
    """Transform: Compose rich text representation for semantic embedding."""
    genre_names = [t.name_en for t in tags if t.group_type == "genre"]
    theme_names = [t.name_en for t in tags if t.group_type == "theme"]

    lines = [f"Title: {manga.title_main}"]

    alt_t = [a.title for a in manga.alt_titles]
    if alt_t:
        lines.append(f"Alternative Titles: {', '.join(alt_t)}")

    if genre_names:
        lines.append(f"Genres: {', '.join(genre_names)}")
    if theme_names:
        lines.append(f"Themes: {', '.join(theme_names)}")

    if manga.demographic:
        lines.append(f"Demographic: {manga.demographic}")

    summary = manga.description_en or manga.description_vi
    if summary:
        # Truncate summary to avoid context length limits for all-MiniLM-L6-v2
        summary = summary[:1500]
        lines.append(f"Summary: {summary}")

    return "\n".join(lines)


def load_batch(
    db: Session,
    q_client,
    model: SentenceTransformer,
    manga_data: list[dict],
    stats_data: dict,
):
    """Load: Upsert models to SQLite and vectors to Qdrant."""
    manga_objects = []
    qdrant_points = []

    # Collect unique tags and authors for FK constraint satisfaction
    all_tags = {}
    all_authors = {}

    for item in manga_data:
        manga, tags, authors, related = transform_to_models(item, stats_data)

        for t in tags:
            all_tags[t.id] = t
        for a in authors:
            all_authors[a.id] = a

        manga_objects.append(manga)

        # Vector encode
        text_to_embed = compose_embedding_text(manga, tags)
        vector = model.encode(text_to_embed).tolist()

        payload = {
            "manga_id": manga.id,
            "title_en": manga.title_main,
            "content_rating": manga.content_rating,
            "demographic": manga.demographic,
            "status": manga.status,
            "genres": [t.name_en for t in tags if t.group_type == "genre"],
            "themes": [t.name_en for t in tags if t.group_type == "theme"],
            "has_vi": manga.has_vi_translation,
            "follows": manga.follows or 0,
            "rating_bayesian": manga.rating_bayesian or 0.0,
        }

        qdrant_points.append(PointStruct(id=manga.id, vector=vector, payload=payload))

    # 1. Merge core entities (Tags, Authors)
    for t in all_tags.values():
        db.merge(t)
    for a in all_authors.values():
        db.merge(a)
    db.commit()

    # 2. Merge Mangas (Cascade automatically merges MangaTag, MangaAuthor, MangaAltTitle)
    for m in manga_objects:
        db.merge(m)
    db.commit()

    # 3. Upsert Vectors
    if qdrant_points:
        q_client.upsert(collection_name=COLLECTION_NAME, points=qdrant_points)

    logger.info(f"Successfully loaded batch of {len(manga_objects)} mangas.")


def main():
    import argparse
    parser = argparse.ArgumentParser(description="MangaDex ETL Script")
    parser.add_argument("--mode", type=str, choices=["seed", "daily", "test"], default="test", help="Run mode")
    parser.add_argument("--limit", type=int, default=50, help="Total manga to fetch")
    parser.add_argument("--updated-since", type=str, default=None, help="ISO datetime for delta sync (e.g. 2026-06-13T00:00:00)")
    args = parser.parse_args()

    logger.info("Initializing Database and Qdrant...")
    init_db()
    init_qdrant()

    q_client = get_qdrant_client()
    db = SessionLocal()

    logger.info("Loading embedding model all-MiniLM-L6-v2...")
    model = SentenceTransformer("all-MiniLM-L6-v2")

    try:
        import time
        t0 = time.time()
        
        order_by = "followedCount"
        if args.mode == "daily":
            order_by = "latestUploadedChapter"
            
        logger.info(f"Starting ingestion in {args.mode.upper()} mode. Limit: {args.limit}, Order by: {order_by}, Updated Since: {args.updated_since}")
        
        # Load or Create Checkpoint
        checkpoint = db.query(EtlCheckpoint).filter_by(job_name=args.mode).first()
        if checkpoint and checkpoint.status == "running":
            logger.info(f"Resuming from checkpoint offset={checkpoint.last_offset}")
            fetched_count = checkpoint.last_offset
        else:
            if not checkpoint:
                checkpoint = EtlCheckpoint(job_name=args.mode)
                db.add(checkpoint)
            checkpoint.last_offset = 0
            checkpoint.total_limit = args.limit
            checkpoint.status = "running"
            checkpoint.started_at = datetime.utcnow()
            db.commit()
            fetched_count = 0
            
        batch_size = 100
        
        while fetched_count < args.limit:
            current_limit = min(batch_size, args.limit - fetched_count)
            logger.info(f"Fetching manga batch limit={current_limit}, offset={fetched_count}...")
            
            t_api_start = time.time()
            try:
                batch = fetch_manga(limit=current_limit, offset=fetched_count, order_by=order_by, updated_since=args.updated_since)
            except Exception as e:
                logger.error(f"Fatal error fetching manga after all retries: {e}")
                checkpoint.status = "failed"
                db.commit()
                send_alert(f"Job `{args.mode}` failed at offset {fetched_count}. Reason: {str(e)[:200]}")
                raise
                
            if not batch:
                logger.info(f"No more manga found at offset {fetched_count}. Reached end of results.")
                break
                
            ids = [m["id"] for m in batch]
            try:
                stats = fetch_statistics(ids)
            except Exception as e:
                logger.error(f"Fatal error fetching stats after all retries: {e}")
                checkpoint.status = "failed"
                db.commit()
                send_alert(f"Job `{args.mode}` failed at stats offset {fetched_count}. Reason: {str(e)[:200]}")
                raise
                
            t_api_end = time.time()
            logger.info(f"API Fetch ({len(batch)} manga + stats) took: {t_api_end - t_api_start:.2f}s")
            
            t_load_start = time.time()
            load_batch(db, q_client, model, batch, stats)
            t_load_end = time.time()
            logger.info(f"Transform & Load (SQLite + Qdrant) took: {t_load_end - t_load_start:.2f}s")
            
            fetched_count += len(batch)
            checkpoint.last_offset = fetched_count
            db.commit()
            
            if len(batch) < current_limit:
                logger.info("Batch returned fewer items than requested. End of results.")
                break
                
            time.sleep(3)
            
        checkpoint.status = "completed"
        db.commit()
        logger.info(f"Total Pipeline execution time: {time.time() - t0:.2f}s")
        if args.mode == "seed":
            send_alert(f"Seed sync completed successfully! Total fetched: {fetched_count}")

    except Exception as e:
        logger.error(f"ETL pipeline failed: {e}")
    finally:
        db.close()
        logger.info("Pipeline completed.")


if __name__ == "__main__":
    main()
