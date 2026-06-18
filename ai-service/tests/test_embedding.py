import pytest
from unittest.mock import patch, MagicMock
from app.services.embedding import EmbeddingService

@pytest.fixture(autouse=True)
def reset_singleton():
    """Reset the singleton before each test."""
    EmbeddingService._instance = None
    EmbeddingService._model = None
    yield
    EmbeddingService._instance = None
    EmbeddingService._model = None

@patch("app.services.embedding.SentenceTransformer")
def test_embedding_service_singleton(mock_st):
    """Test that EmbeddingService is a singleton."""
    service1 = EmbeddingService()
    service2 = EmbeddingService()
    assert service1 is service2
    mock_st.assert_called_once()  # Model should only be loaded once

@patch("app.services.embedding.SentenceTransformer")
def test_embed_text(mock_st):
    """Test single text embedding."""
    mock_model = MagicMock()
    mock_tensor = MagicMock()
    mock_tensor.tolist.return_value = [0.1, 0.2, 0.3]
    mock_model.encode.return_value = mock_tensor
    mock_st.return_value = mock_model
    
    service = EmbeddingService()
    result = service.embed_text("hello")
    
    assert result == [0.1, 0.2, 0.3]
    mock_model.encode.assert_called_once_with("hello")

@patch("app.services.embedding.SentenceTransformer")
def test_embed_texts(mock_st):
    """Test multiple texts embedding."""
    mock_model = MagicMock()
    mock_tensor = MagicMock()
    mock_tensor.tolist.return_value = [[0.1, 0.2], [0.3, 0.4]]
    mock_model.encode.return_value = mock_tensor
    mock_st.return_value = mock_model
    
    service = EmbeddingService()
    result = service.embed_texts(["hello", "world"])
    
    assert result == [[0.1, 0.2], [0.3, 0.4]]
    mock_model.encode.assert_called_once_with(["hello", "world"])
