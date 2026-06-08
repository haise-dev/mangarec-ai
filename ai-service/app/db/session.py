"""SQLite database session management.

Provides:
- ``engine``   — SQLAlchemy Engine bound to the configured SQLite file.
- ``SessionLocal`` — Session factory for creating scoped DB sessions.
- ``get_db()`` — FastAPI dependency that yields a session per request.
- ``init_db()`` — Creates all tables and ensures the data directory exists.
"""

import logging
from collections.abc import Generator
from pathlib import Path

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from app.core.config import settings
from app.db.base import Base

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Extract the filesystem path from the DATABASE_URL so we can ensure the
# parent directory exists before SQLite tries to create the file.
# Format: "sqlite:///relative/path" or "sqlite:////absolute/path"
# ---------------------------------------------------------------------------
_db_path: str = settings.DATABASE_URL.replace("sqlite:///", "", 1)


def _ensure_data_dir() -> None:
    """Create the parent directory for the SQLite file if it doesn't exist."""
    Path(_db_path).parent.mkdir(parents=True, exist_ok=True)


# ---------------------------------------------------------------------------
# Engine & Session factory
# ---------------------------------------------------------------------------
_ensure_data_dir()

engine: Engine = create_engine(
    settings.DATABASE_URL,
    # SQLite does not support concurrent writes from multiple threads by
    # default; this flag allows FastAPI's thread-pool workers to share the
    # single connection safely.
    connect_args={"check_same_thread": False},
    # Emit connection-pool events at DEBUG level only.
    echo=False,
)


@event.listens_for(engine, "connect")
def _set_sqlite_pragmas(dbapi_connection, _connection_record):
    """Tune SQLite for production reliability on every new connection.

    • WAL mode   — allows concurrent readers while writing.
    • journal_size_limit — caps the WAL file at 64 MB.
    • foreign_keys — enforces FK constraints (off by default in SQLite).
    • busy_timeout — waits up to 5 s instead of raising SQLITE_BUSY immediately.
    """
    cursor = dbapi_connection.cursor()
    cursor.execute("PRAGMA journal_mode=WAL;")
    cursor.execute("PRAGMA journal_size_limit=67108864;")
    cursor.execute("PRAGMA foreign_keys=ON;")
    cursor.execute("PRAGMA busy_timeout=5000;")
    cursor.close()


SessionLocal: sessionmaker[Session] = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine,
)


# ---------------------------------------------------------------------------
# Public helpers
# ---------------------------------------------------------------------------
def get_db() -> Generator[Session, None, None]:
    """FastAPI dependency — yields a DB session and closes it after the request."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db() -> None:
    """Create all tables that inherit from ``Base`` (idempotent).

    Call this once during application startup (e.g. in the FastAPI lifespan).
    """
    _ensure_data_dir()
    Base.metadata.create_all(bind=engine)

    # Quick connectivity sanity-check
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))

    logger.info("SQLite database initialised at: %s", _db_path)
