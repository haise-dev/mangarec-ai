"""FastAPI router for Semantic Hybrid Search."""

import logging

from fastapi import APIRouter, Depends, HTTPException
from qdrant_client import QdrantClient
from sqlalchemy.orm import Session

from app.core.qdrant import get_qdrant_client
from app.db.session import get_db
from app.schemas.search import SearchRequest, SearchResponse
from app.services.search import SearchService

logger = logging.getLogger(__name__)

router = APIRouter()


def get_search_service(
    db: Session = Depends(get_db), qdrant: QdrantClient = Depends(get_qdrant_client)
) -> SearchService:
    """Dependency injection factory for SearchService."""
    return SearchService(db=db, qdrant=qdrant)


@router.post("/", response_model=SearchResponse, summary="Semantic Hybrid Search")
def search_manga(
    request: SearchRequest, service: SearchService = Depends(get_search_service)
):
    """
    Search for manga using Semantic Vector Similarity + Hard Payload Filtering.

    The API uses a two-stage ML process:
    1. **Retrieval**: Uses all-MiniLM-L6-v2 to fetch candidate vectors from Qdrant.
       Pre-filters candidates strictly using payload fields (e.g. content_rating).
    2. **Reranking**: Uses cross-encoder/ms-marco to deeply analyze semantic 
       relevance between the query and the candidate's summary, blending it 
       with popularity metrics.
    """
    filters = request.model_dump(exclude_none=True, exclude={"query", "limit"})

    try:
        results = service.hybrid_search(
            query=request.query, filters=filters, limit=request.limit
        )
    except Exception as e:
        logger.error(f"Search API Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

    return SearchResponse(results=results)
