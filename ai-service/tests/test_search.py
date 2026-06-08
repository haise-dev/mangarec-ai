"""Tests for SearchService logic."""
from unittest.mock import MagicMock, patch

from qdrant_client.http import models as qmodels

from app.services.search import SearchService


def test_build_filter():
    """Verify filter translation into Qdrant FieldConditions."""
    # MLManager is a singleton, we don't strictly need it loaded just for building filters
    service = SearchService(db=MagicMock(), qdrant=MagicMock())
    
    filters = {
        "has_vi": True,
        "content_rating": "safe",
        "include_genres": ["Action", "Fantasy"],
        "exclude_themes": ["Harem"]
    }
    
    q_filter = service._build_filter(filters)
    
    must_keys = [c.key for c in q_filter.must]
    must_not_keys = [c.key for c in q_filter.must_not]
    
    # We should have 4 must conditions: has_vi, content_rating, and 2x genres
    assert len(q_filter.must) == 4
    assert "has_vi" in must_keys
    assert "content_rating" in must_keys
    assert "genres" in must_keys
    
    # We should have 1 must_not condition: themes
    assert len(q_filter.must_not) == 1
    assert "themes" in must_not_keys


@patch("app.core.ml.MLManager.get_instance")
def test_hybrid_search_empty(mock_get_instance):
    """Test safe handling when Qdrant returns no results."""
    mock_ml = MagicMock()
    mock_tensor = MagicMock()
    mock_tensor.tolist.return_value = [0.1] * 384
    mock_ml.embedding_model.encode.return_value = mock_tensor
    mock_get_instance.return_value = mock_ml
    
    mock_db = MagicMock()
    mock_qdrant = MagicMock()
    mock_qdrant.search.return_value = []
    
    service = SearchService(db=mock_db, qdrant=mock_qdrant)
    service.ml = mock_ml  # explicitly override for safety
    
    results = service.hybrid_search("test query", limit=5)
    
    assert len(results) == 0
    mock_qdrant.search.assert_called_once()
