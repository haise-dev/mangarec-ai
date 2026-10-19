# ETL Pipeline Rewrite Plan

**Goal:** Refactor `scripts/ingest_manga.py` to follow ETL best practices, matching the new 7-table normalized SQLAlchemy schema, and implement test coverage.

## Steps
1. **Script Refactor:**
   - **Extract:** Fetch manga in batches of 100 via `/manga` endpoint and corresponding statistics via `/statistics/manga`.
   - **Transform:** Parse raw JSON into SQLAlchemy ORM objects (`Manga`, `Tag`, `Author`, `MangaAltTitle`, etc.) and a constructed string for embeddings.
   - **Load:** Generate vectors in batch using `SentenceTransformer`. Use Qdrant `upsert` and SQLAlchemy `merge` (to handle duplicates elegantly) to write data.
2. **Testing:** Create `tests/test_ingest.py` containing unit tests for the Transform logic.
3. **Execution & CI:** Install `pytest` via `uv` and run the tests to ensure stability.

**Status:** Completed.
