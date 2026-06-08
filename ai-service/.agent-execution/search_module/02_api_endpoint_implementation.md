# Search API Endpoint Plan

**Goal:** Expose the Hybrid Search Service via a production-ready FastAPI endpoint with strict Pydantic validation, comprehensive OpenAPI examples, and robust error handling.

## Steps
1. **Define Schemas (`app/schemas/search.py`):**
   - Create Pydantic v2 models for `SearchRequest` and `SearchResponse`.
   - Embed detailed `examples` inside `Field()` to ensure the auto-generated Swagger UI is self-documenting and easy for the Frontend team to test.
2. **Implement API Router (`app/api/endpoints/search.py`):**
   - Create a FastAPI `APIRouter`.
   - Build a `POST /` endpoint that injects `Session` and `QdrantClient`.
   - Instantiate `SearchService` and map the `SearchRequest` fields to the `hybrid_search` logic.
3. **App Integration (`app/main.py`):**
   - Import and mount the router under the `/api/v1/search` prefix.
4. **Acceptance Testing (`tests/test_api_search.py`):**
   - Use `fastapi.testclient.TestClient`.
   - Mock the `SearchService` so tests run reliably without invoking ML models or real databases.
   - Assert correct HTTP status codes, routing, and Pydantic validation behavior.

**Status:** Implementation started.
