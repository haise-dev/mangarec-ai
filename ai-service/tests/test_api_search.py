"""Acceptance tests for the Search API endpoint."""

from unittest.mock import MagicMock

from fastapi.testclient import TestClient

# Mock MLManager before importing app to prevent heavy model loads during testing
import app.core.ml
app.core.ml.MLManager.get_instance().load_models = MagicMock()

from app.api.endpoints.search import get_search_service
from app.main import app

# Create a mock SearchService
mock_service = MagicMock()

def override_get_search_service():
    return mock_service

app.dependency_overrides[get_search_service] = override_get_search_service

client = TestClient(app)


def test_search_manga_endpoint():
    """Verify endpoint correctly parses request, validates Pydantic, and calls service."""
    # Reset mock to clean state
    mock_service.reset_mock()
    
    # Mock return data matching the structure from SearchService
    mock_service.hybrid_search.return_value = [
        {
            "manga": {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "title_main": "Test Manga",
                "cover_url": "http://img.com/1.jpg",
                "description_en": "Desc",
                "description_vi": None,
                "status": "ongoing",
                "content_rating": "safe",
                "rating_bayesian": 9.0,
                "follows": 100
            },
            "qdrant_score": 0.95,
            "rerank_score": 5.5,
            "final_score": 6.8
        }
    ]
    
    payload = {
        "query": "a funny isekai about slime",
        "has_vi": True,
        "content_rating": "safe",
        "limit": 5
    }
    
    response = client.post("/api/v1/search/", json=payload)
    
    assert response.status_code == 200, response.text
    data = response.json()
    
    assert "results" in data
    assert len(data["results"]) == 1
    
    # Verify Manga object fields serialization
    manga = data["results"][0]["manga"]
    assert manga["id"] == "123e4567-e89b-12d3-a456-426614174000"
    assert manga["title_main"] == "Test Manga"
    
    # Verify the service was called with correctly extracted filters (excluding query and limit)
    mock_service.hybrid_search.assert_called_once_with(
        query="a funny isekai about slime",
        filters={"has_vi": True, "content_rating": "safe"},
        limit=5
    )


def test_search_manga_validation_error():
    """Verify Pydantic validation strictly enforces limits."""
    payload = {
        "query": "action",
        "limit": 100  # maximum allowed is 20
    }
    
    response = client.post("/api/v1/search/", json=payload)
    assert response.status_code == 422  # Unprocessable Entity
    
    # Missing query
    payload_no_query = {"limit": 5}
    response2 = client.post("/api/v1/search/", json=payload_no_query)
    assert response2.status_code == 422
