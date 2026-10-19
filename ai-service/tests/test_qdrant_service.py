import pytest
import uuid
from unittest.mock import MagicMock, patch
from qdrant_client.http.models import PointStruct
from app.services.qdrant_service import QdrantService, get_qdrant_service

def test_get_qdrant_service():
    """Test the dependency injection helper."""
    with patch("app.services.qdrant_service.get_qdrant_client") as mock_get_client:
        mock_get_client.return_value = MagicMock()
        service = get_qdrant_service()
        assert isinstance(service, QdrantService)

@patch("app.services.qdrant_service.uuid.uuid5")
def test_save_manga_data_success(mock_uuid5):
    """Test successfully saving manga data to Qdrant."""
    mock_uuid5.return_value = uuid.UUID("12345678-1234-5678-1234-567812345678")
    mock_client = MagicMock()
    service = QdrantService(client=mock_client)
    
    # Mock embedding service
    service.embedding_service = MagicMock()
    service.embedding_service.embed_text.return_value = [0.1, 0.2, 0.3]
    
    result = service.save_manga_data(
        manga_id="test-manga",
        text_content="A great manga",
        metadata={"author": "Oda"}
    )
    
    assert result is True
    service.embedding_service.embed_text.assert_called_once_with("A great manga")
    mock_client.upsert.assert_called_once()
    
    # Verify the payload
    call_args = mock_client.upsert.call_args[1]
    assert call_args["collection_name"] == service.collection_name
    points = call_args["points"]
    assert len(points) == 1
    assert isinstance(points[0], PointStruct)
    assert points[0].payload["manga_id"] == "test-manga"
    assert points[0].payload["author"] == "Oda"
    assert points[0].vector == [0.1, 0.2, 0.3]

def test_save_manga_data_exception():
    """Test error handling when saving manga data."""
    mock_client = MagicMock()
    service = QdrantService(client=mock_client)
    
    # Force an exception
    service.embedding_service = MagicMock()
    service.embedding_service.embed_text.side_effect = Exception("Embedding failed")
    
    result = service.save_manga_data(
        manga_id="test-manga",
        text_content="A great manga"
    )
    
    assert result is False
    mock_client.upsert.assert_not_called()
