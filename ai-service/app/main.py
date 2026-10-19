"""MangaRec AI Service — FastAPI application entry point."""

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.core.qdrant import get_qdrant_client, init_qdrant
from app.core.ml import MLManager
from app.db.session import engine, init_db
from app.api.endpoints import search, chat


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Ensure data directory exists
    from app.db.session import _ensure_data_dir
    _ensure_data_dir()
    
    # Run Alembic migrations on startup
    from alembic import command
    from alembic.config import Config
    alembic_cfg = Config("alembic.ini")
    command.upgrade(alembic_cfg, "head")
    
    # Initialize Qdrant collection on startup
    init_qdrant()
    
    # Load ML Models into memory
    MLManager.get_instance().load_models()
    
    yield

app = FastAPI(
    title="MangaRec AI Service",
    description="AI-powered manga recommendation chatbot service.",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
async def health_check() -> dict[str, str]:
    """Liveness probe — returns service status, Qdrant & SQLite connections."""
    status = {"status": "ok", "qdrant": "disconnected", "sqlite": "disconnected"}

    # Check Qdrant
    try:
        client = get_qdrant_client()
        client.get_collections()
        status["qdrant"] = "connected"
    except Exception:
        status["qdrant"] = "error"

    # Check SQLite
    try:
        from sqlalchemy import text

        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        status["sqlite"] = "connected"
    except Exception:
        status["sqlite"] = "error"

    return status

app.include_router(search.router, prefix="/api/v1/search", tags=["Search"])
app.include_router(chat.router, prefix="/api/v1/chat", tags=["Chat"])

app.mount("/static", StaticFiles(directory="static"), name="static")

