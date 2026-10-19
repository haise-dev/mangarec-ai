"""Enterprise ETL logic tests."""

import pytest
import requests
from unittest.mock import patch, MagicMock
from scripts.ingest_manga import fetch_manga, send_alert

def test_fetch_manga_retry_on_timeout():
    """Test tenacity retry on timeouts."""
    with patch("scripts.ingest_manga.requests.get") as mock_get:
        # Fail 2 times, succeed on 3rd
        mock_resp = MagicMock()
        mock_resp.json.return_value = {"data": [{"id": "manga1"}]}
        mock_get.side_effect = [
            requests.exceptions.ReadTimeout("Timeout 1"),
            requests.exceptions.ReadTimeout("Timeout 2"),
            mock_resp
        ]
        
        # Override wait parameter to be fast for tests
        # We patch time.sleep if needed, but tenacity handles time internally.
        # Actually it's better to just mock the function or expect the calls.
        result = fetch_manga(limit=1, updated_since="2026-06-13T00:00:00")
        
        assert len(result) == 1
        assert result[0]["id"] == "manga1"
        assert mock_get.call_count == 3
        
        # Check if updated_since was passed correctly
        last_call_args = mock_get.call_args[1]["params"]
        assert "updatedAtSince" in last_call_args
        assert last_call_args["updatedAtSince"] == "2026-06-13T00:00:00"

def test_fetch_manga_retry_exhausted():
    """Test tenacity exhausts retries and raises error."""
    with patch("scripts.ingest_manga.requests.get") as mock_get:
        mock_get.side_effect = requests.exceptions.ReadTimeout("Timeout forever")
        
        with pytest.raises(requests.exceptions.ReadTimeout):
            fetch_manga(limit=1)
            
        assert mock_get.call_count == 5

def test_send_alert_no_url():
    """Test send_alert skips if no URL configured."""
    with patch("scripts.ingest_manga.settings") as mock_settings, \
         patch("scripts.ingest_manga.requests.post") as mock_post:
        mock_settings.ALERT_WEBHOOK_URL = None
        send_alert("Test message")
        mock_post.assert_not_called()

def test_send_alert_sends_payload():
    """Test send_alert sends payload."""
    with patch("scripts.ingest_manga.settings") as mock_settings, \
         patch("scripts.ingest_manga.requests.post") as mock_post:
        mock_settings.ALERT_WEBHOOK_URL = "http://fake-webhook.com"
        send_alert("Test message")
        mock_post.assert_called_once()
        args, kwargs = mock_post.call_args
        assert args[0] == "http://fake-webhook.com"
        assert "🚨 [MangaRec ETL] Test message" in kwargs["json"]["content"]
