"""MangaRec AI Service — FastAPI application entry point."""

from contextlib import asynccontextmanager
from fastapi import FastAPI

from app.core.qdrant import init_qdrant, get_qdrant_client

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize Qdrant collection on startup
    init_qdrant()
    yield

app = FastAPI(
    title="MangaRec AI Service",
    description="AI-powered manga recommendation chatbot service.",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
async def health_check() -> dict[str, str]:
    """Liveness probe — returns service status and Qdrant DB connection."""
    status = {"status": "ok", "qdrant": "disconnected"}
    try:
        client = get_qdrant_client()
        # Ping Qdrant by attempting to fetch collections
        client.get_collections()
        status["qdrant"] = "connected"
    except Exception:
        status["qdrant"] = "error"
    return status
