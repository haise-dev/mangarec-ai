"""Tests for MangaDex ingestion ETL logic."""

from app.db.models import Manga, Tag
from scripts.ingest_manga import compose_embedding_text, transform_to_models


def test_transform_to_models():
    """Test MangaDex API payload transformation into SQLAlchemy models."""
    manga_mock = {
        "id": "123",
        "attributes": {
            "title": {"en": "Test Manga"},
            "description": {"en": "A test description", "vi": "Một mô tả"},
            "status": "ongoing",
            "contentRating": "safe",
            "publicationDemographic": "shounen",
            "availableTranslatedLanguages": ["en", "vi"],
            "createdAt": "2020-01-01T00:00:00+00:00",
            "tags": [
                {
                    "id": "tag1",
                    "attributes": {"name": {"en": "Action"}, "group": "genre"}
                }
            ],
            "altTitles": [{"vi": "Truyện Test"}]
        },
        "relationships": [
            {
                "type": "author",
                "id": "auth1",
                "attributes": {"name": "John Doe"}
            }
        ]
    }

    stats_mock = {
        "123": {
            "rating": {"bayesian": 9.5},
            "follows": 1000
        }
    }

    manga, tags, authors, related = transform_to_models(manga_mock, stats_mock)

    assert manga.id == "123"
    assert manga.title_main == "Test Manga"
    assert manga.has_en_translation is True
    assert manga.has_vi_translation is True
    assert manga.rating_bayesian == 9.5
    assert manga.follows == 1000

    assert len(manga.alt_titles) == 1
    assert manga.alt_titles[0].title == "Truyện Test"

    assert len(tags) == 1
    assert tags[0].name_en == "Action"

    assert len(authors) == 1
    assert authors[0].name == "John Doe"


def test_compose_embedding_text():
    """Test embedding string format."""
    manga = Manga(
        title_main="Test Manga",
        description_en="Summary here",
        demographic="shounen",
    )
    
    from app.db.models import MangaAltTitle
    
    manga.alt_titles = [MangaAltTitle(title="AltTitle", language="en", manga_id="123")]

    tags = [Tag(name_en="Action", group_type="genre")]

    text = compose_embedding_text(manga, tags)

    assert "Title: Test Manga" in text
    assert "Alternative Titles: AltTitle" in text
    assert "Genres: Action" in text
    assert "Demographic: shounen" in text
    assert "Summary: Summary here" in text
