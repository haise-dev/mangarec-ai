"""Pydantic schemas for Search API Request & Response validation."""

from typing import List, Optional

from pydantic import BaseModel, Field


class SearchRequest(BaseModel):
    """Payload for initiating a semantic hybrid search."""

    query: str = Field(
        ...,
        description="The semantic search query in natural language.",
        examples=["A funny isekai about slime"],
    )
    has_vi: Optional[bool] = Field(
        None, description="Set to true to strictly require Vietnamese translations."
    )
    content_rating: Optional[str] = Field(
        None, description="Filter by content rating (e.g. safe, erotica).", examples=["safe"]
    )
    demographic: Optional[str] = Field(
        None, description="Filter by target audience demographic.", examples=["shounen"]
    )
    include_genres: Optional[List[str]] = Field(
        None,
        description="Genres that MUST be present in the manga.",
        examples=[["Action", "Comedy"]],
    )
    exclude_genres: Optional[List[str]] = Field(
        None,
        description="Genres that MUST NOT be present.",
        examples=[["Horror"]],
    )
    include_themes: Optional[List[str]] = Field(
        None, description="Themes that MUST be present."
    )
    exclude_themes: Optional[List[str]] = Field(
        None, description="Themes that MUST NOT be present."
    )
    limit: int = Field(
        5, description="Maximum number of results to return.", ge=1, le=20
    )


class MangaResponse(BaseModel):
    """Standardized Manga metadata format for the frontend."""

    id: str
    title_main: str
    cover_url: Optional[str]
    description_en: Optional[str]
    description_vi: Optional[str]
    status: str
    content_rating: str
    rating_bayesian: Optional[float]
    follows: Optional[int]

    model_config = {"from_attributes": True}


class SearchResultItem(BaseModel):
    """Wrapper holding both the Manga data and calculated ML scores."""

    manga: MangaResponse
    qdrant_score: float = Field(..., description="Cosine similarity score from Vector DB.")
    rerank_score: float = Field(..., description="Contextual relevance score from Cross-Encoder.")
    final_score: float = Field(..., description="Blended score used for final ranking.")


class SearchResponse(BaseModel):
    """Top level Search API Response."""

    results: List[SearchResultItem]
