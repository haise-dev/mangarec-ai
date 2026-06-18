import pytest
from unittest.mock import MagicMock, patch
from qdrant_client.http.models import Distance, VectorParams
from app.core.qdrant import get_qdrant_client, init_qdrant, COLLECTION_NAME, VECTOR_SIZE
from app.core.config import settings

@patch("app.core.qdrant.QdrantClient")
def test_get_qdrant_client_remote(mock_qdrant_client):
    settings.QDRANT_URL = "http://remote-qdrant"
    settings.QDRANT_API_KEY = "key"
    client = get_qdrant_client()
    mock_qdrant_client.assert_called_once_with(url="http://remote-qdrant", api_key="key")

@patch("app.core.qdrant.QdrantClient")
def test_get_qdrant_client_local(mock_qdrant_client):
    settings.QDRANT_URL = ""
    settings.QDRANT_HOST = "localhost"
    settings.QDRANT_PORT = 6333
    client = get_qdrant_client()
    mock_qdrant_client.assert_called_once_with(host="localhost", port=6333)

@patch("app.core.qdrant.get_qdrant_client")
def test_init_qdrant_creates_collection(mock_get_client):
    mock_client = MagicMock()
    mock_collections = MagicMock()
    mock_collections.collections = []
    mock_client.get_collections.return_value = mock_collections
    mock_get_client.return_value = mock_client
    
    init_qdrant()
    
    mock_client.create_collection.assert_called_once()
    args = mock_client.create_collection.call_args[1]
    assert args["collection_name"] == COLLECTION_NAME
    assert args["vectors_config"].size == VECTOR_SIZE
    assert args["vectors_config"].distance == Distance.COSINE

@patch("app.core.qdrant.get_qdrant_client")
def test_init_qdrant_collection_exists(mock_get_client):
    mock_client = MagicMock()
    mock_collection = MagicMock()
    mock_collection.name = COLLECTION_NAME
    mock_collections = MagicMock()
    mock_collections.collections = [mock_collection]
    mock_client.get_collections.return_value = mock_collections
    mock_get_client.return_value = mock_client
    
    init_qdrant()
    
    mock_client.create_collection.assert_not_called()

@patch("app.core.qdrant.get_qdrant_client")
def test_init_qdrant_exception_handled(mock_get_client):
    mock_client = MagicMock()
    mock_client.get_collections.side_effect = Exception("Connection error")
    mock_get_client.return_value = mock_client
    
    # Should not raise exception
    init_qdrant()
