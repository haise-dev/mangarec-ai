# Hybrid Search Module Plan

**Goal:** Implement a highly optimized, isolated Semantic Search service that performs Hybrid Search (Vector + Payload Filter) and Cross-Encoder Reranking, strictly adhering to `coding-rules.md`.

## Steps
1. **ML Model Management (`app/core/ml.py`):**
   - Create a singleton `MLManager` to load `all-MiniLM-L6-v2` (Embedder) and `cross-encoder/ms-marco-MiniLM-L-6-v2` (Reranker) ONCE at startup to preserve RAM and minimize latency.
   - Update `app/main.py` lifespan to preload these models.
2. **Search Service (`app/services/search.py`):**
   - Build a `SearchService` class handling dependencies (DB Session, QdrantClient).
   - Implement `_build_filter` to dynamically convert user constraints (`has_vi`, `demographic`, `genres`) into Qdrant `FieldCondition`s.
   - Implement `search`: 
     1. Vectorize query.
     2. Query Qdrant for top `limit * 4` candidates.
     3. Retrieve corresponding `Manga` objects from SQLite.
     4. Build `[query, summary]` pairs and pass to Cross-Encoder.
     5. Calculate blended score `(Rerank Score * 0.8) + (Popularity * 0.2)`.
     6. Return sorted top `limit` results.
3. **Testing (`tests/test_search.py`):**
   - Write unit tests mocking Qdrant and SQLite to ensure scoring and filtering algorithms work as expected.

**Status:** Implementation started.
