# MangaRec AI — System Architecture

> **Loại tài liệu:** System Architecture & Data Flow Guide  
> **Đối tượng:** Backend Developer, Architect, Senior Dev  
> **Cập nhật lần cuối:** 2026-06-15  
> **Mục tiêu:** Trả lời câu hỏi "Hệ thống hoạt động như thế nào? Dữ liệu chảy qua đâu?"

---

## 1. Architecture Overview

### 1.1 Pattern
- **Style:** Microservices Monorepo — 3 service độc lập, cùng một repository
- **Orchestration:** Docker Compose (root-level `docker-compose.yml` dùng `include:` kết hợp)
- **Network:** Tất cả service chia sẻ external Docker network `mangarec_global_network`

### 1.2 Communication
| Loại | Giữa | Protocol |
|---|---|---|
| Đồng bộ (Sync) | FE → BE | REST/HTTPS (Axios) |
| Đồng bộ (Sync) | BE → AI | REST (internal) |
| Bất đồng bộ (Async) | BE → Client | WebSocket (notifications) |
| Background | AI Worker | Scheduler (`scripts/scheduler.py`) |

### 1.3 High-Level Architecture Diagram

```
┌──────────────────────────────────────────────────────────┐
│                    Browser / Client                      │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS / WebSocket
                         ▼
┌──────────────────────────────────────────────────────────┐
│            fe-service  (Port 3000 dev / 80 prod)         │
│         React 19 + TypeScript + Vite + Tailwind v4       │
│    Nginx (prod: multi-stage Docker, node:22 → nginx:1.27)│
└────────────────────────┬─────────────────────────────────┘
                         │ REST API (Axios → VITE_API_BASE_URL)
                         ▼
┌──────────────────────────────────────────────────────────┐
│            be-service  (Port 8080)                       │
│    Spring Boot 3.3.5 · Java 17 · Spring Security · JWT   │
│    Spring Mail · payOS Java SDK · Spring WebSocket        │
│         ┌──────────────┐    ┌───────────────┐            │
│         │  PostgreSQL  │    │  Redis 7       │            │
│         │  (port 5432) │    │  (port 6379)   │            │
│         │  Flyway Mgr  │    │  Cache/Session │            │
│         └──────────────┘    └───────────────┘            │
└────────────────────────┬─────────────────────────────────┘
                         │ REST API (internal)
                         ▼
┌──────────────────────────────────────────────────────────┐
│            ai-service  (Port 8000)                       │
│    FastAPI 0.136.3 · Python 3.12 · uv · Uvicorn          │
│    LangGraph 1.2.4 · LangChain-Groq · Tenacity           │
│    sentence-transformers · LangSmith (observability)      │
│         ┌──────────────┐    ┌───────────────┐            │
│         │    Qdrant    │    │    SQLite      │            │
│         │  (port 6333) │    │  /app/data/   │            │
│         │ Vector Search│    │ manga_metadata│            │
│         └──────────────┘    └───────────────┘            │
│                                                          │
│    ai-worker (background container)                      │
│    └── uv run scripts/scheduler.py                       │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTP API
                         ▼
                 ┌───────────────┐
                 │  Groq LLM API │
                 │  (external)   │
                 └───────────────┘
```

---

## 2. Service Breakdown (Chi tiết từng Service)

### 2.1 Frontend (`fe-service`)

| Thuộc tính | Giá trị |
|---|---|
| Framework | React 19.1.1 + TypeScript ~5.8.3 |
| Build tool | Vite 7.1.2 (`@tailwindcss/vite` plugin) |
| Routing | react-router-dom 7.9.1 (createBrowserRouter) |
| HTTP | Axios 1.13.2 |
| Styling | Tailwind CSS v4.1.14 |
| Path alias | `@` → `./src` (cấu hình trong `vite.config.ts`) |
| Dev port | 3000 (host 0.0.0.0) |
| Prod | node:22 → nginx:1.27 (multi-stage build) |

**Nginx production config:** Gzip enabled, assets cache 1 năm (immutable), SPA fallback `try_files $uri /index.html`.

**Build args (truyền qua Docker):**
- `VITE_API_BASE_URL` — URL của BE API
- `VITE_GOOGLE_CLIENT_ID` — Google OAuth client ID

### 2.2 Core Backend (`be-service`)

| Thuộc tính | Giá trị |
|---|---|
| Framework | Spring Boot 3.3.5 |
| Language | Java 17 |
| Build | Maven |
| Auth | Spring Security + JJWT 0.11.5 + google-api-client 2.8.1 |
| DB Primary | PostgreSQL 16-alpine (Spring Data JPA) |
| DB Migration | Flyway 10-alpine (dedicated container, chạy khi `postgres` healthy) |
| Cache | Redis 7-alpine |
| Payment | payOS-java 2.0.1 |
| Real-time | Spring WebSocket |
| Email | Spring Mail (Gmail SMTP) — OTP verification |
| API Docs | springdoc-openapi 2.2.0 → `/swagger-ui.html` |
| Monitoring | Spring Boot Actuator |

**Package structure (`com.mangarec`):**
```
com.mangarec/
├── common/          # ApiResponse wrapper, utility classes
├── config/          # SecurityConfig, OpenApiConfig
├── domain/          # Domain entities & repositories
│   ├── auth/        # AuthActionToken, UserAuthEvent, OTP logic
│   ├── chat/        # ChatSession
│   ├── guest/       # GuestProfile
│   └── user/        # User, UserAuthIdentity, UserPreference, RefreshToken
├── exception/       # GlobalExceptionHandling, custom exceptions
│   ├── ForBiddenException
│   ├── InvalidDataException
│   ├── RateLimitExceededException
│   ├── ResourceNotFoundException
│   └── UnauthorizedException
└── features/        # Feature slices
    └── auth/        # AuthController + request DTOs
```

**Flyway migrations hiện có:**
- `V1__init_mangarec_schema.sql` — Schema khởi tạo
- `V2__payos_payment_schema.sql` — Schema thanh toán payOS

### 2.3 AI Backend (`ai-service`)

| Thuộc tính | Giá trị |
|---|---|
| Framework | FastAPI 0.136.3 |
| Language | Python ≥ 3.12 |
| Package Mgr | uv |
| ASGI | Uvicorn 0.49.0 |
| LLM | LangChain-Groq 1.1.2 + LangGraph 1.2.4 |
| Embedding | sentence-transformers 5.5.1 + PyTorch (CPU-only) |
| Vector DB | Qdrant Client 1.18.0 |
| Metadata DB | SQLite (SQLAlchemy 2.0 + Alembic) |
| Observability | LangSmith 0.8.9 |
| Resilience | Tenacity 9.1.4 (retry + exponential backoff) |

**Startup sequence (lifespan):**
1. `_ensure_data_dir()` — Tạo `/app/data/` nếu chưa có
2. `alembic upgrade head` — Chạy migration SQLite tự động
3. `init_qdrant()` — Khởi tạo Qdrant collection
4. `MLManager.load_models()` — Load embedding model vào RAM

**API Endpoints đã implement:**
- `GET /health` — Liveness probe (check Qdrant + SQLite)
- `POST /api/v1/search` — Manga semantic search
- `POST /api/v1/chat` — Chatbot conversation (LangGraph)
- `/docs` — Swagger UI tự động
- `/static` — Static files

**AI Worker:** Container riêng (`ai-worker`) chạy `uv run scripts/scheduler.py` — background tasks (data sync, embedding updates).

---

## 3. Data Flow Diagrams (Luồng Dữ liệu)

### 3.1 Luồng Gợi ý Manga (Recommendation Flow)
```
User → FE (input query)
     → BE /api/v1/ (xác thực JWT, kiểm tra credit)
     → AI /api/v1/search (POST)
          → EmbeddingService (sentence-transformers → vector)
          → Qdrant search (cosine similarity, Top-K)
          → Trả về manga list
     → BE (enrich với metadata nếu cần)
     → FE (render kết quả)
```

### 3.2 Luồng Chatbot — RAG (Chatbot Flow)
```
User message → FE
             → BE (auth check)
             → AI /api/v1/chat (POST)
                  → LangGraph bot_graph.py (graph entry)
                       → Tool: Qdrant search (retrieve relevant manga)
                       → Groq LLM (langchain-groq → generate response)
                       → Tenacity retry nếu Groq API fail
                  → LangSmith tracing (observability)
             → Streaming/JSON response → FE
```

### 3.3 Luồng Xác thực — Auth Flow
```
Email Registration:
  FE (email + password) → BE /api/v1/auth/register
  → Hash password → Save User (PENDING_VERIFICATION)
  → Send OTP email (Spring Mail)
  → User clicks verify → /api/v1/auth/verify-email
  → Status: ACTIVE → Issue JWT

Google OAuth:
  FE (Google ID token) → BE /api/v1/auth/google-login
  → google-api-client verify ID token
  → Find or create UserAuthIdentity (provider=GOOGLE)
  → Issue JWT (Access + Refresh)

JWT Usage:
  Client → Authorization: Bearer <access_token>
  → Spring Security filter chain → validate → authorize
  → Refresh: /api/v1/auth/refresh → Rotation (old token revoked)
```

### 3.4 Luồng Thanh toán (Payment Flow)
```
User → FE (chọn gói)
     → BE /api/v1/payments (create order)
     → payOS API (create payment link)
     → Redirect user → payOS payment page
     → User completes payment
     → payOS Webhook → BE /api/v1/payments/payos/webhook
          → Verify HMAC signature (PAYOS_CHECKSUM_KEY)
          → Update UserSubscriptionStatus
          → Emit WebSocket notification → FE
```

### 3.5 Luồng AI Worker (Background)
```
ai-worker container → uv run scripts/scheduler.py
  → Schedule tasks (interval-based):
       - Sync manga metadata → SQLite
       - Batch embed new manga → Qdrant upsert
```

---

## 4. Database Design

### 4.1 PostgreSQL (`be-service`)
Managed bởi **Flyway** — migrations trong `be-service/src/main/resources/db/migration/`

| Migration | Nội dung |
|---|---|
| `V1__init_mangarec_schema.sql` | Core schema: users, auth identities, auth tokens, refresh tokens, guest profiles, chat sessions, user preferences, auth events |
| `V2__payos_payment_schema.sql` | Payment schema: orders, subscriptions, payment history |

**Entities chính:**
- `UserEntity` — Core user (email, status, subscription)
- `UserAuthIdentityEntity` — Auth provider (EMAIL / GOOGLE)
- `AuthActionTokenEntity` — OTP tokens (email verify, reset password)
- `UserRefreshTokenEntity` — JWT refresh token rotation
- `ChatSessionEntity` — Chat session tracking
- `GuestProfileEntity` — Anonymous user session
- `UserPreferenceEntity` — User reading preferences

### 4.2 SQLite (`ai-service`)
File: `/app/data/manga_metadata.db`  
Managed bởi **Alembic** — `alembic upgrade head` chạy tự động khi service start.

Data được mount ra host: `./data:/app/data` (persistent volume trong production).

### 4.3 Qdrant (Vector Database)
- **Collection:** `manga_embeddings`
- **Vector:** embedding của `title + description + tags` (sentence-transformers)
- **Search:** Cosine similarity, Top-K results
- **Storage:** Docker volume `qdrant_data_prod` / `qdrant_data_dev`
- **Model cache:** Baked into Docker image tại `/app/.cache/huggingface` (production)

---

## 5. Security Architecture

| Layer | Cơ chế |
|---|---|
| Transport | HTTPS toàn bộ (production) |
| Auth | JWT (JJWT 0.11.5) — Access Token ngắn hạn + Refresh Token rotation |
| Spring Security | Filter chain — bảo vệ mọi request BE |
| Google OAuth | ID token verification (`google-api-client`) |
| Payment | HMAC signature verification (`PAYOS_CHECKSUM_KEY`) |
| AI Service | Không expose public — chỉ accessible qua internal Docker network |
| Secrets | Environment variables (không hardcode) |
| OTP | Email verification + password reset — expiry configurable qua env |
| Exception handling | `GlobalExceptionHandling` — không leak stacktrace ra ngoài |

---

## 6. Infrastructure & Docker

### 6.1 Container Map (Production)

| Container | Image | Port | Role |
|---|---|---|---|
| `fe-service-prod` | `mangarec-fe-service:latest` | 3000→80 | Nginx SPA |
| `be-service-prod` | *(custom)* | 8080 | Spring Boot API |
| `mangarec-postgres` | `postgres:16-alpine` | 5432 | Primary DB |
| `mangarec-flyway` | `flyway/flyway:10-alpine` | — | DB migration runner |
| `mangarec-redis` | `redis:7-alpine` | 6379 | Cache |
| `ai-service-prod` | *(custom)* | 8000 | FastAPI AI |
| `ai-worker-prod` | *(custom)* | — | Background scheduler |
| `qdrant-prod` | `qdrant/qdrant:latest` | 6333 | Vector DB |

### 6.2 Health Checks (đã cấu hình)
- `postgres`: `pg_isready` — interval 10s, retries 5
- `redis`: `redis-cli ping` — interval 10s, retries 5
- `fe-service-prod`: `wget http://127.0.0.1/` — interval 30s
- `ai-service`: `GET /health` — kiểm tra Qdrant + SQLite connection

### 6.3 Scalability Notes
- **FE Service:** Stateless → scale ngang dễ dàng; Nginx serve static assets hiệu quả
- **BE Service:** Stateless (JWT) → load balance multiple instances; Redis cache giảm DB load
- **AI Service:** Scale ngang khi embedding demand tăng; Model được bake vào image (không fetch runtime)
- **Qdrant:** Self-hosted, scale theo storage nếu cần

> **Xem thêm:** `docs/docker_architecture.md` — sơ đồ Docker đầy đủ đã có sẵn.  
> **Deployment chi tiết:** `docs/deployment-guide.md`
