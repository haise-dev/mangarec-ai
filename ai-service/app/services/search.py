"""Hybrid Search Service.

Handles semantic search via Qdrant combined with Cross-Encoder reranking
and SQLite metadata retrieval.
"""

import logging
from typing import Any

from qdrant_client import QdrantClient
from qdrant_client.http import models as qmodels
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.ml import MLManager
from app.core.qdrant import COLLECTION_NAME
from app.db.models import Manga

logger = logging.getLogger(__name__)


class SearchService:
    """Core service for searching manga."""

    def __init__(self, db: Session, qdrant: QdrantClient):
        self.db = db
        self.qdrant = qdrant
        self.ml = MLManager.get_instance()

    def _build_filter(self, filters: dict[str, Any]) -> qmodels.Filter:
        """Convert a dictionary of filters into Qdrant FieldConditions."""
        must_conditions = []
        must_not_conditions = []

        if "has_vi" in filters:
            must_conditions.append(
                qmodels.FieldCondition(
                    key="has_vi", match=qmodels.MatchValue(value=filters["has_vi"])
                )
            )

        if "content_rating" in filters:
            must_conditions.append(
                qmodels.FieldCondition(
                    key="content_rating",
                    match=qmodels.MatchValue(value=filters["content_rating"]),
                )
            )

        if "demographic" in filters:
            must_conditions.append(
                qmodels.FieldCondition(
                    key="demographic",
                    match=qmodels.MatchValue(value=filters["demographic"]),
                )
            )

        # Include tags
        if "include_genres" in filters:
            for genre in filters["include_genres"]:
                must_conditions.append(
                    qmodels.FieldCondition(
                        key="genres", match=qmodels.MatchValue(value=genre)
                    )
                )
        if "include_themes" in filters:
            for theme in filters["include_themes"]:
                must_conditions.append(
                    qmodels.FieldCondition(
                        key="themes", match=qmodels.MatchValue(value=theme)
                    )
                )

        # Exclude tags
        if "exclude_genres" in filters:
            for genre in filters["exclude_genres"]:
                must_not_conditions.append(
                    qmodels.FieldCondition(
                        key="genres", match=qmodels.MatchValue(value=genre)
                    )
                )
        if "exclude_themes" in filters:
            for theme in filters["exclude_themes"]:
                must_not_conditions.append(
                    qmodels.FieldCondition(
                        key="themes", match=qmodels.MatchValue(value=theme)
                    )
                )

        return qmodels.Filter(must=must_conditions, must_not=must_not_conditions)

    def hybrid_search(
        self, query: str, filters: dict[str, Any] = None, limit: int = 5
    ) -> list[dict[str, Any]]:
        """Perform semantic search with hard filtering and cross-encoder reranking."""
        filters = filters or {}
        
        # Ensure models are loaded
        if not self.ml.embedding_model or not self.ml.reranker_model:
            raise RuntimeError("ML Models are not loaded. Call MLManager.load_models() first.")

        # 1. Vectorize query
        logger.info(f"Encoding query: '{query}'")
        query_vector = self.ml.embedding_model.encode(query).tolist()

        # 2. Qdrant Retrieval (Get more candidates for reranking)
        candidates_limit = limit * 4
        q_filter = self._build_filter(filters)

        logger.info(f"Querying Qdrant (limit={candidates_limit})...")
        qdrant_results = self.qdrant.search(
            collection_name=COLLECTION_NAME,
            query_vector=query_vector,
            query_filter=q_filter,
            limit=candidates_limit,
            with_payload=True,
        )

        if not qdrant_results:
            return []

        candidate_ids = [str(hit.id) for hit in qdrant_results]

        # 3. Fetch full metadata from SQLite
        # Using select(...).where(Manga.id.in_(candidate_ids))
        stmt = select(Manga).where(Manga.id.in_(candidate_ids))
        mangas = self.db.execute(stmt).scalars().all()
        manga_map = {m.id: m for m in mangas}

        # 4. Reranking Setup
        cross_inp = []
        valid_hits = []

        for hit in qdrant_results:
            m = manga_map.get(str(hit.id))
            if not m:
                continue

            # Prioritize Vietnamese summary if filtering by has_vi, else English
            # Fallback to title if no summary exists
            summary = m.description_vi or m.description_en or m.title_main
            
            # Cross encoder requires a pair [query, document]
            cross_inp.append([query, summary])
            valid_hits.append((hit, m))

        if not cross_inp:
            return []

        # 5. Predict relevance scores
        logger.info(f"Reranking {len(cross_inp)} candidates...")
        rerank_scores = self.ml.reranker_model.predict(cross_inp)

        # 6. Score Blending & Normalization
        MAX_FOLLOWS = 100000.0  # Assumed ceiling for normalisation
        final_results = []

        for (hit, m), r_score in zip(valid_hits, rerank_scores):
            follows = hit.payload.get("follows", 0) if hit.payload else 0
            normalized_follows = min(follows / MAX_FOLLOWS, 1.0)

            # Rerank score (logits) is typically -10 to +10. 
            # We add a slight bump for popularity.
            final_score = (float(r_score) * 0.8) + (normalized_follows * 2.0)

            final_results.append(
                {
                    "manga": m,
                    "qdrant_score": float(hit.score),
                    "rerank_score": float(r_score),
                    "final_score": float(final_score),
                }
            )

        # 7. Sort and truncate
        final_results.sort(key=lambda x: x["final_score"], reverse=True)
        return final_results[:limit]
