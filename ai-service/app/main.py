"""MangaRec AI Service — FastAPI application entry point."""

from fastapi import FastAPI

app = FastAPI(
    title="MangaRec AI Service",
    description="AI-powered manga recommendation chatbot service.",
    version="0.1.0",
)


@app.get("/health")
async def health_check() -> dict[str, str]:
    """Liveness probe — returns service status."""
    return {"status": "ok"}
