# MangaRec AI — Crawl & Synchronization Strategy

> **Status:** Approved (R&D Brainstormed)
> **Related Document:** [Data Architecture & Strategy](data_architecture_and_strategy.md)
> **Author:** AI Engineering Team

---

## 1. Overview
This document defines the comprehensive strategy for fetching, updating, and maintaining Manga data from the MangaDex API. It acts as an extension to the core Data Architecture document, focusing specifically on the **ETL Automation (Cronjob)** and **Vector Search Mitigation Strategies**.

## 2. Target Scope & Filtering Strategy
To optimize database performance and ensure the AI remains highly relevant to our specific use case, we will strictly filter out non-Japanese content at the API request level.

- **Target:** Japanese Manga ONLY.
- **Filter Applied:** `originalLanguage[]=ja`
- **Impact:** Eliminates tens of thousands of Manhwa (Korean, `ko`) and Manhua (Chinese, `zh`) from the dataset. The maximum DB size is naturally capped at roughly ~30,000 to ~40,000 high-quality records, heavily reducing storage and compute costs.

## 3. Worker Architecture (Docker-Native Scheduler)
To adhere to the KISS principle and ensure web server stability, the data ingestion scheduler will be decoupled from the FastAPI application.

- **Implementation:** A lightweight Python script (`scheduler.py`) using the `schedule` library.
- **Deployment:** Deployed as a dedicated standalone container (`ai-worker`) within `docker-compose.yml` and `docker-compose.dev.yml`.
- **Benefits:** The memory-heavy, CPU-intensive ETL pipeline runs in its own isolated process. It will never block or slow down the FastAPI web service serving user requests.

## 4. Phased Ingestion Logic (The Crawl Strategy)
The crawling process is divided into two distinct, automated phases. There is no manual intervention required after deployment.

### Phase 1: Auto-Seed (Initial Boot)
Executed exactly once when the `ai-worker` container starts.
- **Trigger:** Checks the SQLite database. If `SELECT count(id) FROM mangas` returns `0`, the Auto-Seed phase begins.
- **Action:** Fetches the Top 1,000 most followed Manga (`order[followedCount]=desc`).
- **Duration:** ~2.5 minutes (based on 50 items/batch).
- **Goal:** Instantly provides a robust, high-quality knowledge base so the AI Chatbot is fully functional right after the first `docker-compose up`.

### Phase 2: Daily Incremental Sync (Nightly Cronjob)
Runs automatically at 03:00 AM every day.
- **Trigger:** Scheduled by the `ai-worker` container.
- **Action 1 (Delta Sync - Catching New Trends):** Fetches Manga sorted by `latestUploadedChapter=desc` **BUT** smartly uses the `updatedAtSince` parameter retrieved from the last SQLite `EtlCheckpoint`. This filters the request down to ONLY the Manga that have actually changed since yesterday, saving up to 90% in API bandwidth.
- **Action 2 (Refreshing Stats):** Fetches the Top 500 Manga sorted by `followedCount=desc` to update their `rating`, `follows`, and `status`.
- **Duration:** < 1 minute (due to Delta Sync).
- **Goal:** Prevents "Stale Data". Ensures the AI knows about new releases and changing popularity trends without needing to do a full DB wipe.

## 5. Enterprise Resilience & Fault Tolerance
Since the ETL pipeline runs completely unattended, it is fortified with the following enterprise-grade mechanisms to prevent data loss or silent failures:

1. **State Checkpointing (SQLite):**
   - The system records its exact progress (the `offset`) into the `etl_checkpoints` table after every successful 100-item batch.
   - If the server loses power or the container is killed at offset 8,000, the next run will **Resume** exactly from 8,000 instead of restarting from 0.
2. **Exponential Backoff & Jitter (`tenacity`):**
   - MangaDex can occasionally drop connections (`408 Timeout` or `RemoteDisconnected`).
   - Instead of a naive sleep, the pipeline uses an exponential backoff algorithm (e.g., waiting 3s $\rightarrow$ 6s $\rightarrow$ 12s $\rightarrow$ 24s) before retrying. This prevents IP Blacklisting and gracefully handles temporary API outages.
3. **Webhook Alerting System:**
   - If a batch fails 5 times consecutively, the pipeline is marked as `failed` to prevent further damage.
   - It will immediately fire an alert payload (`🚨 [MangaRec ETL] Fatal error...`) to a configured Discord/Telegram webhook (`ALERT_WEBHOOK_URL`), instantly notifying the Admin.

## 6. Mitigating Vector Search Limitations (Semantic Crowding)
A major R&D concern was the "10k Point Accuracy Drop" myth. While Qdrant (using HNSW index) scales to millions of vectors without slowing down, the accuracy drops due to **Semantic Crowding** (e.g., thousands of generic Isekai manga having almost identical summary vectors).

We mitigate this entirely through a **Two-Stage Retrieval Pipeline**:

1. **Stage 1: Hybrid Pre-filtering (Broad Recall)**
   - We utilize Qdrant's payload filtering to instantly slice the vector space.
   - Example: Hard-filtering by `originalLanguage=ja` AND `demographic=shounen` AND `content_rating=safe` reduces the search space from 40,000 to perhaps 2,000 points.
   - The Bi-Encoder (`all-MiniLM-L6-v2`) then quickly retrieves the **Top 50** nearest vectors from this small subset.

2. **Stage 2: Cross-Encoder Reranking (Precision)**
   - The Top 50 results (which might suffer from vector crowding) are passed to a Cross-Encoder Reranker model.
   - The Reranker deeply analyzes the exact semantic relationship between the User's Query and each Manga's summary.
   - It re-sorts the list and outputs the **Absolute Top 5**.
   - **Conclusion:** This pipeline guarantees that crowding in a 40k point database will never impact the final Top 5 results presented to the user.
