import pytest
from unittest.mock import patch
from app.core.download_model import download_model

@patch("app.core.download_model.CrossEncoder")
@patch("app.core.download_model.SentenceTransformer")
def test_download_model(mock_st, mock_ce):
    download_model()
    mock_st.assert_called_once_with("sentence-transformers/all-MiniLM-L6-v2")
    mock_ce.assert_called_once_with("cross-encoder/ms-marco-MiniLM-L-6-v2")
