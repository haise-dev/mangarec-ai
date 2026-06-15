# MangaRec AI — Codebase Summary (Bản đồ Codebase)

> **Loại tài liệu:** Codebase Navigation Guide  
> **Đối tượng:** Developer mới join, mọi contributor  
> **Cập nhật lần cuối:** 2026-06-15  
> **Mục tiêu:** Đọc 15 phút là biết "con gì ở đâu" trong codebase

---

## 1. Quick Start (5 phút để chạy được dự án)

### Yêu cầu Hệ thống
- Docker Engine ≥ 24.x
- Docker Compose Plugin ≥ 2.x
- Git

### Khởi động Local Development

```bash
# 1. Clone repository
git clone <repo-url>
cd mangarec-ai

# 2. Tạo Docker network dùng chung giữa các service
docker network create mangarec_global_network

# 3. Cấu hình biến môi trường (xem Bảng Env bên dưới)
cp ai-service/.env.example ai-service/.env
cp be-service/.env.example be-service/.env
cp fe-service/.env.example fe-service/.env
# → Điền các giá trị thực vào từng file .env

# 4. Khởi động toàn bộ stack
docker compose -f docker-compose.dev.yml up -d

# 5. Kiểm tra logs
docker compose -f docker-compose.dev.yml logs -f
docker compose -f docker-compose.dev.yml ps
```

### Service Endpoints (Development)

| Service | URL | Ghi chú |
|---|---|---|
| Frontend | `http://localhost:3000` | Vite dev server (hot-reload) |
| Backend API | `http://localhost:8080` | Spring Boot |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | API docs tự động |
| AI Service | `http://localhost:8000` | FastAPI |
| AI Docs | `http://localhost:8000/docs` | FastAPI Swagger |
| Qdrant Dashboard | `http://localhost:6333/dashboard` | Vector DB UI |

> **Lưu ý:** Port FE dev server là `3000` (từ `package.json`: `vite --host 0.0.0.0 --port 3000`), không phải `5173`.

---

## 2. Monorepo Structure Map (Bản đồ Thư mục Gốc)

```
mangarec-ai/                     ← Monorepo root
├── fe-service/                  # React SPA (Port 3000 dev)
├── be-service/                  # Spring Boot Core API (Port 8080)
├── ai-service/                  # FastAPI AI Service (Port 8000)
├── docs/                        # Tài liệu dự án (thư mục này)
│   └── docker_architecture.md   # Sơ đồ Docker đã có sẵn
├── tests/                       # Integration tests tổng hợp
├── app/                         # ⚠️ Vai trò chưa rõ — cần làm sạch (xem Technical Debt)
├── docker-compose.yml           # Production orchestration
├── docker-compose.dev.yml       # Development orchestration
├── plan.md                      # Khung sườn tài liệu dự án
├── research-summary.md          # Kết quả phân tích codebase
└── .agent/                      # ECC Agent config — KHÔNG sửa trừ khi biết rõ
```

---

## 3. Frontend Service (`fe-service/`) — File Map

### Tech Stack (từ `package.json`)
- **Framework:** React 19.1.1
- **Language:** TypeScript ~5.8.3
- **Build Tool:** Vite 7.1.2
- **Routing:** react-router-dom 7.9.1
- **HTTP Client:** Axios 1.13.2
- **Styling:** Tailwind CSS v4.1.14

### Cấu trúc Source (`fe-service/src/`)

```
fe-service/src/
├── App.tsx          # Root component
├── main.tsx         # Entry point (React DOM render)
├── vite-env.d.ts    # Vite type declarations
├── api/             # Axios API clients — gọi BE endpoints
├── app/             # App-level config (router setup, providers)
├── components/      # Reusable UI components
├── context/         # React Context providers (Auth, Theme, v.v.)
├── hooks/           # Custom React hooks
├── pages/           # Page-level components (route targets)
├── styles/          # Global CSS / Tailwind config
├── types/           # TypeScript type definitions
└── utils/           # Helper functions
```

**File cần đọc đầu tiên:** `App.tsx`, `src/app/` (router config), `src/api/`

### Scripts (từ `package.json`)

| Lệnh | Mô tả |
|---|---|
| `npm run dev` | Dev server — hot reload, port 3000 |
| `npm run build` | Production build (TypeScript check + Vite bundle) |
| `npm run lint` | ESLint check |
| `npm run format` | Prettier format tất cả `src/**/*.{ts,tsx,css,json,md}` |
| `npm run format:check` | Prettier check (dùng trong CI) |
| `npm run preview` | Preview production build local |

---

## 4. Core Backend Service (`be-service/`) — File Map

### Tech Stack (từ `pom.xml`)
- **Language:** Java 17
- **Framework:** Spring Boot 3.3.5
- **Build:** Maven
- **Database:** PostgreSQL + Spring Data JPA + Flyway migrations
- **Cache:** Redis (spring-boot-starter-data-redis)
- **Auth:** Spring Security + JJWT 0.11.5 + Google API Client (OAuth)
- **Payment:** payOS Java SDK 2.0.1
- **Real-time:** Spring WebSocket
- **Email:** Spring Mail (OTP verification)
- **Utilities:** Lombok 1.18.44, Gson 2.11.0
- **API Docs:** springdoc-openapi 2.2.0 (Swagger UI)
- **Monitoring:** Spring Boot Actuator

### Cấu trúc Source (Java package: `com.mangarec`)

```
be-service/src/main/java/com/mangarec/
├── controller/   # REST API endpoints — nhận request, trả response
├── service/      # Business logic layer — xử lý nghiệp vụ
├── repository/   # Spring Data JPA repositories — truy vấn DB
├── entity/       # JPA entities — ánh xạ bảng PostgreSQL
├── dto/          # Data Transfer Objects — request/response shapes
├── security/     # Spring Security config, JWT filter chain
├── config/       # App configurations (Redis, CORS, WebSocket, v.v.)
└── exception/    # @ControllerAdvice — xử lý lỗi tập trung
```

**File cần đọc đầu tiên:** `Application.java`, `SecurityConfig.java`, `application.yml`

### Database Migrations
- **Tool:** Flyway (tự động chạy khi service start)
- **Location:** `be-service/src/main/resources/db/migration/`
- **Naming convention:** `V{version}__{description}.sql` (e.g., `V1__create_users_table.sql`)

---

## 5. AI Backend Service (`ai-service/`) — File Map

### Tech Stack (từ `pyproject.toml`)
- **Language:** Python ≥ 3.12
- **Framework:** FastAPI 0.136.3
- **Package Manager:** uv (thay thế pip/poetry — nhanh hơn 10-100x)
- **LLM:** LangChain-Groq 1.1.2 + LangGraph 1.2.4
- **Observability:** LangSmith 0.8.9
- **Vector DB Client:** qdrant-client 1.18.0
- **Embeddings:** sentence-transformers 5.5.1 + PyTorch (CPU-only)
- **Database:** SQLAlchemy 2.0 + Alembic (migrations)
- **Validation:** Pydantic 2.13.4
- **Resilience:** Tenacity 9.1.4 (retry với exponential backoff)
- **ASGI Server:** Uvicorn 0.49.0
- **Linting:** Ruff (line-length: 88, rules: E, F, I, W, B)
- **Testing:** pytest + pytest-asyncio

### Cấu trúc Source (`ai-service/app/`)

```
ai-service/app/
├── main.py          # FastAPI app entry point — đọc đây đầu tiên
├── __init__.py
├── api/             # FastAPI routers — định nghĩa endpoints
├── chatbot/         # LangGraph graph definition & conversation nodes
├── core/            # App config (settings, database connections)
├── db/              # Database init & connection management
├── repositories/    # Data access layer (Qdrant + SQLite)
├── schemas/         # Pydantic models (request/response validation)
├── services/        # Business logic (recommendation, embedding)
└── tools/           # LangChain tools cho AI agent
```

**File cần đọc đầu tiên:** `app/main.py`, `app/core/config.py`, `app/chatbot/graph.py`

### Database thực tế (AI Service)
- **Metadata storage:** SQLite — file tại `/app/data/manga_metadata.db` (từ `.env.example`)
- **Vector storage:** Qdrant — kết nối qua `QDRANT_HOST:QDRANT_PORT` (mặc định `localhost:6333`)
- **Migrations:** Alembic — thư mục `ai-service/alembic/versions/`

### Scripts thường dùng (chạy qua `uv run`)

```bash
# Khởi động dev server
uv run uvicorn app.main:app --reload

# Chạy migrations
uv run alembic upgrade head
uv run alembic history

# Chạy tests
uv run pytest

# Linting
uv run ruff check .
uv run ruff format .
```

---

## 6. Environment Variables Guide

### AI Service (`ai-service/.env`)

| Biến | Bắt buộc | Mô tả | Ví dụ |
|---|---|---|---|
| `GROQ_API_KEY` | ✅ | API key Groq LLM | `gsk_...` |
| `QDRANT_HOST` | ✅ | Hostname Qdrant server | `localhost` |
| `QDRANT_PORT` | ✅ | Port Qdrant server | `6333` |
| `DATABASE_URL` | ✅ | SQLite connection string | `sqlite:////app/data/manga_metadata.db` |
| `LANGCHAIN_API_KEY` | ⚠️ | LangSmith API key (observability) | `ls__...` |
| `LANGCHAIN_TRACING_V2` | Optional | Bật LangSmith tracing | `true` |
| `LANGCHAIN_PROJECT` | Optional | Tên project LangSmith | `mangarec_ai_service` |

### Core Backend (`be-service/.env`)

| Biến | Bắt buộc | Mô tả |
|---|---|---|
| `POSTGRES_DB` | ✅ | Tên database PostgreSQL |
| `POSTGRES_USER` | ✅ | User PostgreSQL |
| `POSTGRES_PASSWORD` | ✅ | Password PostgreSQL |
| `SPRING_DATASOURCE_URL` | ✅ | JDBC connection string |
| `REDIS_HOST` / `REDIS_PORT` | ✅ | Redis connection |
| `GOOGLE_AUTH_CLIENT_ID` | ✅ | Google OAuth client ID |
| `SPRING_MAIL_*` | ✅ | SMTP config (Gmail) cho OTP |
| `PAYOS_CLIENT_ID` | ✅ | payOS client ID |
| `PAYOS_API_KEY` | ✅ | payOS API key |
| `PAYOS_CHECKSUM_KEY` | ✅ | payOS checksum key |
| `PAYOS_WEBHOOK_URL` | ✅ | URL nhận payOS webhook |

---

## 7. Key Patterns & Conventions

### API Convention
- **Prefix:** Tất cả endpoints BE đi kèm prefix `/api/v1/`
- **Response Envelope:**
  ```json
  { "success": true, "data": {...}, "message": "OK", "pagination": {...} }
  ```
- **HTTP Verbs:** Tuân thủ đúng nghĩa — `GET` (read), `POST` (create), `PUT`/`PATCH` (update), `DELETE` (remove)

### Error Handling
- **BE:** `@ControllerAdvice` xử lý lỗi tập trung — không bao giờ trả stacktrace ra ngoài production.
- **AI:** FastAPI exception handlers theo từng loại lỗi.

### Database Migrations
- **BE:** Flyway — file SQL trong `be-service/src/main/resources/db/migration/` — **tự động chạy khi service start**.
- **AI:** Alembic — chạy thủ công: `uv run alembic upgrade head`.

### Auth Flow
- **JWT:** Access Token (short-lived) + Refresh Token rotation
- **Mọi request** đến BE qua Spring Security filter chain
- **Google OAuth:** ID token verification qua `google-api-client`

---

## 8. Known Issues & Technical Debt

| Issue | Mức độ | Ghi chú |
|---|---|---|
| `app/` directory ở root chưa rõ vai trò | MEDIUM | Cần làm sạch hoặc document thêm |
| Test coverage < 80% | HIGH | Ưu tiên trước Phase 2 |
| Không có CI/CD pipeline | HIGH | Thêm vào Phase 2 |
| Không có rate limiting trên AI endpoints | HIGH | Ngăn chặn abuse & cost overrun |

---

> **Tiếp theo nên đọc:**
> - Kiến trúc hệ thống & luồng dữ liệu → `docs/system-architecture.md`
> - Quy chuẩn code → `docs/code-standards.md`
> - Hướng dẫn deploy → `docs/deployment-guide.md`
