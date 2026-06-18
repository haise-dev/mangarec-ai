import pytest
from unittest.mock import MagicMock, patch
from sqlalchemy.orm import Session
from app.db.session import _set_sqlite_pragmas, get_db, init_db

def test_set_sqlite_pragmas():
    mock_dbapi_connection = MagicMock()
    mock_cursor = MagicMock()
    mock_dbapi_connection.cursor.return_value = mock_cursor
    
    _set_sqlite_pragmas(mock_dbapi_connection, None)
    
    assert mock_cursor.execute.call_count == 4
    mock_cursor.close.assert_called_once()

@patch("app.db.session.SessionLocal")
def test_get_db_mocked(mock_session_local):
    mock_db = MagicMock()
    mock_session_local.return_value = mock_db
    
    gen = get_db()
    db = next(gen)
    
    assert db is mock_db
    mock_db.close.assert_not_called()
    
    try:
        next(gen)
    except StopIteration:
        pass
        
    mock_db.close.assert_called_once()

@patch("app.db.session.Base.metadata.create_all")
@patch("app.db.session.engine.connect")
@patch("app.db.session._ensure_data_dir")
def test_init_db(mock_ensure, mock_connect, mock_create_all):
    mock_conn = MagicMock()
    # Mock context manager
    mock_connect.return_value.__enter__.return_value = mock_conn
    
    init_db()
    
    mock_ensure.assert_called_once()
    mock_create_all.assert_called_once()
    mock_conn.execute.assert_called_once()
