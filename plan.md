# Plan: Khung Sườn Chi Tiết cho 7 Tài Liệu Dự Án MangaRec AI

> **Vai trò:** Architect + Planner Agent  
> **Ngày lập:** 2026-06-15  
> **Nguồn phân tích:** `research-summary.md`, cấu trúc codebase thực tế  
> **Mục tiêu:** Cung cấp khung sườn chi tiết (outline) để viết 7 file tài liệu trong thư mục `./docs/`

---

## Tổng quan Chiến lược Tài liệu

Hệ thống tài liệu được thiết kế theo nguyên tắc **"Tài liệu như Code" (Docs-as-Code)**:
- Mỗi file phục vụ **một nhóm độc giả cụ thể** (dev mới, ops, architect, contributor).
- Tài liệu phải **phản ánh trực tiếp codebase**, không phải mô tả lý tưởng hoá.
- Tuân theo cấu trúc **từ tổng quan → chi tiết → hành động cụ thể**.

| File | Đối tượng Chính | Mục tiêu Cốt lõi |
|---|---|---|
| `project-overview-pdr.md` | Stakeholder, PM, Dev mới | Hiểu tầm nhìn & định nghĩa sản phẩm |
| `system-architecture.md` | Backend Dev, Architect | Hiểu thiết kế hệ thống & luồng dữ liệu |
| `codebase-summary.md` | Mọi Dev | Định hướng nhanh trong codebase |
| `code-standards.md` | Mọi Dev, Reviewer | Đảm bảo chất lượng & nhất quán code |
| `design-guidelines.md` | Frontend Dev, Designer | Đảm bảo nhất quán UI/UX |
| `deployment-guide.md` | DevOps, Dev | Vận hành & triển khai dự án |
| `project-roadmap.md` | Stakeholder, PM, Dev | Theo dõi tiến độ & định hướng tương lai |

---

## 1. `docs/project-overview-pdr.md`
> **PDR = Product Definition & Requirements Document**  
> Đối tượng: Product Manager, Stakeholder, Developer mới onboard  
> Mục tiêu: Trả lời câu hỏi "Dự án này là gì? Tại sao nó tồn tại?"

### Cấu trúc

```
# MangaRec AI — Product Definition & Requirements (PDR)
```

#### 1.1 Executive Summary (Tóm tắt Điều hành)
- **Tên dự án:** MangaRec AI
- **Loại sản phẩm:** Micro-SaaS, AI-powered Manga Recommendation Platform
- **Mô hình kinh doanh:** Freemium + Credit-based (người dùng mua credit để dùng tính năng AI)
- **Thị trường mục tiêu:** Độc giả truyện tranh đa ngôn ngữ (ưu tiên tiếng Việt + tiếng Anh)
- **Tuyên ngôn sản phẩm (Elevator Pitch):** *(Cần điền)*

#### 1.2 Problem Statement (Bài toán Giải quyết)
- Người dùng khó tìm manga phù hợp trong kho nội dung khổng lồ.
- Các hệ thống gợi ý truyền thống không cá nhân hoá theo gu đọc sâu.
- Thiếu công cụ tư vấn/trò chuyện về manga theo thời gian thực.

#### 1.3 Solution Overview (Giải pháp Đề xuất)
- **AI Recommendation Engine:** Dùng Vector Search (Qdrant) + Semantic Embedding để gợi ý manga ngữ nghĩa.
- **AI Chatbot (RAG):** Tích hợp LangGraph + LangChain/Groq để trò chuyện và tư vấn manga.
- **SaaS Subscription:** Gói trả phí qua payOS, hệ thống credit/token.

#### 1.4 Core Features (Tính năng Cốt lõi)
Danh sách có mức độ ưu tiên (P0/P1/P2):
- **P0 — Must Have:**
  - [ ] Đăng ký / Đăng nhập (Email + Google OAuth)
  - [ ] Gợi ý Manga dựa trên AI (Semantic Search)
  - [ ] Hệ thống Credit/Gói dịch vụ
- **P1 — Should Have:**
  - [ ] Chatbot tương tác về Manga (RAG)
  - [ ] Thanh toán tích hợp (payOS)
  - [ ] Thông báo Real-time (WebSocket)
- **P2 — Nice to Have:**
  - [ ] Lịch sử đọc & Cá nhân hoá sâu
  - [ ] Đánh giá / Review manga

#### 1.5 User Personas (Chân dung Người dùng)
- **Persona 1 — "Otaku Thông thường":** Đọc manga thường xuyên, muốn khám phá thể loại mới.
- **Persona 2 — "Người dùng Casually":** Ít đọc, muốn gợi ý nhanh không cần tìm kiếm.
- **Persona 3 — "Fan Cứng":** Cần chatbot tư vấn và thảo luận về manga yêu thích.

#### 1.6 Success Metrics (Chỉ số Thành công)
- Tỷ lệ chuyển đổi Freemium → Paid (Target: >5%)
- Số lượng query AI trung bình mỗi người dùng/ngày
- Độ chính xác gợi ý (User satisfaction score)
- Thời gian phản hồi API < 500ms (p95)

#### 1.7 Out of Scope (Ngoài Phạm vi)
- Cung cấp/lưu trữ nội dung manga (không vi phạm bản quyền)
- Social features (follow, friend list) — xem xét ở v2

#### 1.8 Assumptions & Constraints (Giả định & Ràng buộc)
- Phụ thuộc vào nguồn dữ liệu manga bên ngoài (APIs/crawlers)
- Giới hạn ngân sách LLM (Groq) → cần tối ưu token
- Cần tuân thủ PDPA (bảo vệ dữ liệu cá nhân người dùng Việt Nam)

---

## 2. `docs/system-architecture.md`
> Đối tượng: Backend Developer, Architect, Senior Dev  
> Mục tiêu: Trả lời câu hỏi "Hệ thống hoạt động như thế nào? Dữ liệu chảy qua đâu?"

### Cấu trúc

```
# MangaRec AI — System Architecture
```

#### 2.1 Architecture Overview
- **Pattern:** Microservices Monorepo
- **Communication Style:**
  - Đồng bộ (Sync): REST API giữa FE → BE, BE → AI
  - Bất đồng bộ (Async): WebSocket cho real-time notifications
- **Sơ đồ kiến trúc cấp cao (High-Level Diagram):**
  ```
  [Browser/Client]
       │ HTTPS
       ▼
  [fe-service: React/Vite]
       │ REST API (Axios)
       ▼
  [be-service: Spring Boot] ◄──► [PostgreSQL]
       │         │                    ▲
       │         └──► [Redis Cache]   │ (Flyway Migration)
       │ REST API
       ▼
  [ai-service: FastAPI] ◄──► [Qdrant Vector DB]
       │                ◄──► [PostgreSQL (AI metadata)]
       │ LangChain/Groq
       ▼
  [Groq LLM API] (external)
  ```

#### 2.2 Service Breakdown
Mô tả chi tiết từng service:

**2.2.1 Frontend (`fe-service`)**
- Tech: React 19.1.1, TypeScript ~5.8.3, Vite 7.1.2, Tailwind CSS v4.1.14, react-router-dom 7.9.1, Axios 1.13.2
- Vai trò: SPA, giao tiếp với BE qua REST
- Cấu trúc thư mục: `api/`, `app/` (router + providers), `components/`, `pages/`, `hooks/`, `context/`, `types/`, `utils/`, `styles/`
- Dev: Vite dev server — Port **3000** (`vite --host 0.0.0.0 --port 3000`)
- Build: Nginx serving static assets (Production)

**2.2.2 Core Backend (`be-service`)**
- Tech: Java 17, Spring Boot 3.3.5, Maven
- Vai trò: API Gateway cho FE, xử lý nghiệp vụ chính, quản lý auth, payment
- Database: PostgreSQL (primary), Redis (cache/session)
- Key Modules: Auth (JWT + Google OAuth), User Management, Payment (payOS), WebSocket

**2.2.3 AI Backend (`ai-service`)**
- Tech: Python ≥3.12, FastAPI 0.136.3, uv (package manager)
- Dependencies chính: LangGraph 1.2.4, langchain-groq 1.1.2, LangSmith 0.8.9, qdrant-client 1.18.0, sentence-transformers 5.5.1, Pydantic 2.13.4, Tenacity 9.1.4
- Vai trò: Xử lý mọi tác vụ AI (recommendation, chatbot, RAG)
- Databases: Qdrant (vector search), **SQLite** `/app/data/manga_metadata.db` (metadata cục bộ — không phải PostgreSQL)
- Containers: `ai-service` (FastAPI server) + `ai-worker` (scheduler chạy `scripts/scheduler.py`)
- Key Modules:
  - `api/`: FastAPI routers
  - `chatbot/`: LangGraph conversation logic
  - `core/`: App config, settings
  - `db/`: Database init & connection management
  - `services/`: Business logic (recommendation, embedding)
  - `repositories/`: Data access layer (Qdrant + SQLite)
  - `tools/`: LangChain tools for chatbot
  - `schemas/`: Pydantic models

#### 2.3 Data Flow Diagrams (Luồng Dữ liệu)

**2.3.1 Luồng Gợi ý Manga (Recommendation Flow)**
```
User → FE → BE (auth check) → AI Service
       → Embedding(query) → Qdrant Search → Top-K Results
       → BE (enrich data) → FE (render)
```

**2.3.2 Luồng Chatbot (Chatbot Flow)**
```
User message → FE (WebSocket/REST) → BE → AI Service
             → LangGraph Agent
             → [Tool: Search Qdrant] | [Tool: Fetch BE Data]
             → Groq LLM (generate response)
             → Stream response → FE
```

**2.3.3 Luồng Xác thực (Auth Flow)**
```
Login (Email/Google) → BE → Validate → Issue JWT (Access + Refresh)
JWT → Request Header → BE (Spring Security Filter) → Authorize
```

**2.3.4 Luồng Thanh toán (Payment Flow)**
```
User → FE → BE (create order) → payOS API
payOS Webhook → BE (verify signature) → Update user plan/credits
```

#### 2.4 Database Schema Overview
- **PostgreSQL (BE):** Users, Roles, Plans, Transactions, Refresh Tokens — migrations qua Flyway
- **SQLite (AI):** Manga metadata — file tại `/app/data/manga_metadata.db`, migrations qua Alembic
- **Qdrant Collections:** `manga_embeddings` (vector: title + description + tags)

#### 2.5 Security Architecture
- Tất cả traffic qua HTTPS (production)
- JWT short-lived (Access: 15 phút), Refresh Token rotation
- Spring Security filter chain cho mọi request BE
- AI Service chỉ accessible nội bộ (không expose public)
- Secrets qua Environment Variables (không hardcode)

#### 2.6 Infrastructure & Docker
- Tham chiếu đến `docker_architecture.md` (đã có)
- Mô tả thêm về chiến lược scale:
  - AI Service: Scale ngang khi cần xử lý nhiều embedding
  - BE Service: Stateless → dễ load balance
  - Redis: Cache layer để giảm DB load

---

## 3. `docs/codebase-summary.md`
> Đối tượng: Developer mới join, mọi contributor  
> Mục tiêu: Bản đồ nhanh về codebase — đọc 15 phút là biết "con gì ở đâu"

### Cấu trúc

```
# MangaRec AI — Codebase Summary (Bản đồ Codebase)
```

#### 3.1 Quick Start (5 phút để chạy được dự án)
- Yêu cầu hệ thống: Docker Engine ≥ 24.x, Docker Compose Plugin ≥ 2.x, Git
- Clone & chạy:
  ```bash
  git clone <repo-url>
  cd mangarec-ai
  # Mỗi service có .env.example riêng
  cp ai-service/.env.example ai-service/.env
  cp be-service/.env.example be-service/.env
  cp fe-service/.env.example fe-service/.env
  # Điền các biến môi trường thực vào từng file
  docker network create mangarec_global_network
  docker compose -f docker-compose.dev.yml up -d
  ```
- Verify: Bảng kiểm tra các endpoint đang hoạt động

  | Service | URL | Ghi chú |
  |---|---|---|
  | Frontend | `http://localhost:3000` | Port **3000**, không phải 5173 |
  | Backend API | `http://localhost:8080` | Spring Boot |
  | Swagger UI | `http://localhost:8080/swagger-ui.html` | |
  | AI Service | `http://localhost:8000` | FastAPI |
  | Qdrant Dashboard | `http://localhost:6333/dashboard` | |

#### 3.2 Monorepo Structure Map
Bản đồ thư mục annotated:
```
mangarec-ai/
├── fe-service/            # React SPA (Port 3000 dev / 80 prod)
├── be-service/            # Spring Boot API (Port 8080)
├── ai-service/            # FastAPI AI (Port 8000) + ai-worker (scheduler)
├── docs/                  # Tài liệu dự án (thư mục này)
├── tests/                 # Integration tests tổng hợp
├── app/                   # ⚠️ Vai trò chưa rõ — cần làm sạch (Technical Debt)
├── docker-compose.yml     # Production orchestration
├── docker-compose.dev.yml # Development orchestration (include từng service)
└── .agent/                # ECC Agent config (không sửa trừ khi biết)
```

#### 3.3 Frontend (`fe-service/`) — File Map
```
fe-service/src/
├── App.tsx      # Root component (RouterProvider + AppProviders)
├── main.tsx     # Entry point
├── api/         # Axios API clients (gọi BE endpoints)
├── app/
│   ├── providers/   # AppProviders (React context wrappers)
│   └── router/      # Router config (index.tsx, routes.constants.ts, guards/)
├── components/  # Reusable UI components
├── context/     # React Context providers (Auth, Theme, etc.)
├── hooks/       # Custom React hooks
├── pages/       # Page-level components (route targets)
├── styles/      # Global CSS / Tailwind config
├── types/       # TypeScript type definitions
└── utils/       # Helper functions
```
*Key file cần đọc đầu tiên:* `App.tsx`, `src/app/router/index.tsx`, `src/app/providers/AppProviders.tsx`

#### 3.4 Core Backend (`be-service/`) — File Map
```
be-service/src/main/java/.../
├── controller/   # REST API endpoints
├── service/      # Business logic layer
├── repository/   # Spring Data JPA repositories
├── entity/       # JPA entities (DB tables)
├── dto/          # Data Transfer Objects
├── security/     # Spring Security config, JWT filters
├── config/       # App configurations (Redis, CORS, etc.)
└── exception/    # Custom exception handlers
```
*Key file cần đọc đầu tiên:* `Application.java`, `SecurityConfig.java`, `application.yml`

#### 3.5 AI Backend (`ai-service/`) — File Map
```
ai-service/app/
├── main.py       # FastAPI entry point
├── api/          # FastAPI routers (endpoints)
├── chatbot/      # LangGraph graph definition & nodes
├── core/         # App config, settings
├── db/           # Database init & connection management
├── repositories/ # Data access (Qdrant + SQLite)
├── schemas/      # Pydantic models (request/response)
├── services/     # Business logic (recommendation, embedding)
└── tools/        # LangChain tools for AI agent
```
*Key file cần đọc đầu tiên:* `app/main.py`, `app/core/config.py`, `app/chatbot/graph.py`

*Containers thực tế:*
- `ai-service`: FastAPI server — `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
- `ai-worker`: Background scheduler — `uv run scripts/scheduler.py`

#### 3.6 Environment Variables Guide
Bảng biến môi trường theo từng service (lấy từ `.env.example` thực tế):

**AI Service (`ai-service/.env`):**
| Biến | Mô tả | Bắt buộc |
|------|--------|----------|
| `GROQ_API_KEY` | API key Groq LLM | ✅ |
| `QDRANT_HOST` | Hostname Qdrant server (e.g., `localhost`) | ✅ |
| `QDRANT_PORT` | Port Qdrant server (e.g., `6333`) | ✅ |
| `DATABASE_URL` | SQLite connection string (`sqlite:////app/data/manga_metadata.db`) | ✅ |
| `LANGCHAIN_API_KEY` | LangSmith observability key | ⚠️ Optional |
| `LANGCHAIN_TRACING_V2` | Bật LangSmith tracing (`true`/`false`) | ⚠️ Optional |
| `LANGCHAIN_PROJECT` | Tên project LangSmith | ⚠️ Optional |

**Core Backend (`be-service/.env`):**
| Biến | Mô tả | Bắt buộc |
|------|--------|----------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | PostgreSQL credentials | ✅ |
| `SPRING_DATASOURCE_URL` | JDBC connection string | ✅ |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection | ✅ |
| `GOOGLE_AUTH_CLIENT_ID` | Google OAuth client ID | ✅ |
| `SPRING_MAIL_*` | SMTP config (Gmail) cho OTP email | ✅ |
| `PAYOS_CLIENT_ID` / `PAYOS_API_KEY` / `PAYOS_CHECKSUM_KEY` | payOS credentials | ✅ |
| `PAYOS_WEBHOOK_URL` | URL nhận payOS webhook callback | ✅ |
| `SPRING_FLYWAY_ENABLED` | Bật Flyway auto-migration (`true`) | ✅ |

**Frontend (`fe-service/.env`):**
| Biến | Mô tả | Bắt buộc |
|------|--------|----------|
| `VITE_API_BASE_URL` | URL của BE API (e.g., `http://localhost:8080`) | ✅ |
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth client ID cho FE | ✅ |

#### 3.7 Key Patterns & Conventions
- **API Versioning:** Tất cả endpoints đi kèm prefix `/api/v1/`
- **Error Handling:** BE dùng `@ControllerAdvice`, AI dùng FastAPI exception handlers
- **Database Migrations:** BE dùng Flyway (`/resources/db/migration/`), AI dùng Alembic (`/alembic/versions/`)
- **Logging:** Cấu hình qua môi trường (không log sensitive data)

---

## 4. `docs/code-standards.md`
> Đối tượng: Mọi Developer, Code Reviewer  
> Mục tiêu: "Luật chơi" để code của team nhất quán và bảo mật

### Cấu trúc

```
# MangaRec AI — Code Standards & Best Practices
```

#### 4.1 Nguyên tắc Cốt lõi (Core Principles)
Áp dụng cho **toàn bộ** codebase:
1. **Test-Driven Development (TDD):** Viết test trước, code sau. Coverage tối thiểu 80%.
2. **Immutability First:** Không mutate object/state trực tiếp. Luôn trả về bản sao mới.
3. **Security-First:** Không hardcode secrets. Validate mọi input từ người dùng.
4. **Small, Focused Functions:** Hàm ≤ 50 lines, file ≤ 800 lines. Không nesting > 4 cấp.
5. **Explicit Error Handling:** Không swallow errors. Log với context đầy đủ ở server-side.

#### 4.2 Frontend Standards (React/TypeScript)
- **TypeScript:** Strict mode bật. Không dùng `any` trừ trường hợp đặc biệt (cần comment giải thích).
- **Component Rules:**
  - Một file = Một component (ngoại lệ: sub-components nhỏ).
  - Dùng functional components + hooks (không dùng class components).
  - Props types định nghĩa bằng `interface` (không `type` cho props).
- **State Management:**
  - Local state: `useState`, `useReducer`
  - Server state: Custom hooks + Axios (xem xét React Query nếu cần)
  - Global state: Context API (tránh prop drilling quá 2 cấp)
- **Naming Convention:**
  - Component files: `PascalCase.tsx` (e.g., `MangaCard.tsx`)
  - Hook files: `useCamelCase.ts` (e.g., `useAuth.ts`)
  - Utility files: `camelCase.ts` (e.g., `formatDate.ts`)
- **Styling:**
  - Dùng Tailwind CSS v4 utility classes.
  - Không viết inline styles trừ trường hợp dynamic values.
  - Custom CSS trong `styles/` nếu Tailwind không đủ.
- **Testing:** Vitest + React Testing Library. Test behavior, không test implementation.

#### 4.3 Core Backend Standards (Java/Spring Boot)
- **Code Style:**
  - Dùng Lombok (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`) để giảm boilerplate.
  - Record classes cho DTOs bất biến nếu Java version hỗ trợ.
  - Không dùng field injection (`@Autowired` trên field) — dùng constructor injection.
- **API Design:**
  - RESTful, tuân thủ HTTP verbs đúng nghĩa (GET, POST, PUT/PATCH, DELETE).
  - Response envelope chuẩn: `{ success, data, message, pagination? }`
  - Validate input bằng `@Valid` + Bean Validation annotations.
- **Security:**
  - Không bao giờ trả về stacktrace trong response production.
  - Dùng parameterized queries (JPA tự xử lý — không dùng string concat SQL thủ công).
  - Mọi endpoint cần xác thực phải đi qua `@PreAuthorize`.
- **Database:**
  - Mọi thay đổi schema qua Flyway migration files (không sửa trực tiếp DB).
  - Naming convention migration: `V{version}__{description}.sql`
  - Không lazy-load trong loop (N+1 problem).
- **Testing:** JUnit 5 + Mockito. Phân tách Unit Test (`/test/unit/`) và Integration Test (`/test/integration/`).

#### 4.4 AI Backend Standards (Python/FastAPI)
- **Code Style:**
  - Tuân thủ Ruff config trong `pyproject.toml` (line-length: 88, rules: E, F, I, W, B).
  - Dùng type hints cho **tất cả** function signatures.
  - Docstring theo Google Style.
- **API Design:**
  - Tất cả request/response validate qua Pydantic models.
  - Dependency injection qua FastAPI `Depends()` (không dùng global variables).
  - Async functions (`async def`) cho mọi I/O-bound operations.
- **AI/LLM Specific:**
  - Không hardcode prompts — prompts là config, lưu trong `core/prompts.py` hoặc file riêng.
  - Luôn dùng Tenacity cho LLM calls (retry với exponential backoff).
  - Log token usage để kiểm soát chi phí.
  - Giới hạn context window — tóm tắt hội thoại khi cần.
- **Database:**
  - Mọi schema change qua Alembic migrations.
  - Repository pattern — không query trực tiếp từ service layer.
- **Testing:** pytest (`pyproject.toml` dev deps). Mock external APIs (Groq, Qdrant) trong unit tests. (Lưu ý: `pytest-asyncio` chưa được thêm vào `pyproject.toml` — cần bổ sung nếu cần test async.)

#### 4.5 Git Workflow & Commit Convention
- **Branch naming:** `{type}/MR-{ticket_number}-{mô-tả-ngắn}` (ví dụ: `feat/MR-0001-setup-env`)
- **Commit messages:** `MR-XXXX | <type>: <mô tả ngắn gọn>` (ví dụ: `MR-0001 | feat: add login`)
  ```
  MR-XXXX | <type>: <mô tả ngắn gọn>

  [body tùy chọn — giải thích WHY, không phải WHAT]
  ```
  - Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`
- **Pull Request:**
  - Mô tả rõ ràng: Vấn đề gì → Giải pháp gì → Cách test
  - Cần ít nhất 1 reviewer approve trước khi merge
  - Không merge khi CI/CD thất bại

#### 4.6 Security Checklist (Bắt buộc trước mỗi commit)
- [ ] Không có credentials/API keys trong code
- [ ] Tất cả user input được validate
- [ ] Không có SQL string concatenation thủ công
- [ ] Không log sensitive user data (password, token, PII)
- [ ] Dependencies không có CVE nghiêm trọng (dùng `npm audit`, `mvn verify`, `uv pip compile`)

---

## 5. `docs/design-guidelines.md`
> Đối tượng: Frontend Developer, UI/UX Designer  
> Mục tiêu: Đảm bảo giao diện nhất quán, dễ dùng, đẹp mắt

### Cấu trúc

```
# MangaRec AI — Design System & Frontend Guidelines
```

#### 5.1 Design Philosophy (Triết lý Thiết kế)
- **Aesthetic:** Dark mode-first, modern, premium — lấy cảm hứng từ các nền tảng streaming (Crunchyroll, Netflix).
- **Core Values:** Dễ khám phá (Discoverable) · Phản hồi nhanh (Responsive) · Tập trung vào nội dung (Content-first)
- **Target feel:** "AI-powered, but human"

#### 5.2 Color System (Hệ thống Màu sắc)
Định nghĩa bảng màu chính thức:
| Token | Value | Dùng cho |
|-------|-------|----------|
| `--color-primary` | *[Cần chọn — e.g., #6C63FF]* | CTA, links, highlights |
| `--color-secondary` | *[Cần chọn]* | Accent, badges |
| `--color-bg-base` | `#0F0F13` | Background chính |
| `--color-bg-surface` | `#1A1A24` | Card, panel |
| `--color-text-primary` | `#F0F0F5` | Văn bản chính |
| `--color-text-muted` | `#888899` | Placeholder, caption |
| `--color-success` | `#22C55E` | Trạng thái thành công |
| `--color-error` | `#EF4444` | Lỗi, cảnh báo |

#### 5.3 Typography (Kiểu chữ)
- **Font chính (Body & UI):** Inter (Google Fonts)
- **Font phụ (Headings):** Outfit hoặc Poppins
- **Thang đo font size (Type Scale):**
  ```
  --text-xs:   0.75rem (12px)  — Caption, labels
  --text-sm:   0.875rem (14px) — Secondary text
  --text-base: 1rem (16px)     — Body text (default)
  --text-lg:   1.125rem (18px) — Sub-heading
  --text-xl:   1.25rem (20px)  — Card title
  --text-2xl:  1.5rem (24px)   — Section heading
  --text-3xl:  1.875rem (30px) — Page title
  ```

#### 5.4 Spacing & Layout
- **Spacing System:** Bội số của 4px (0.25rem)
  - `xs: 4px`, `sm: 8px`, `md: 16px`, `lg: 24px`, `xl: 32px`, `2xl: 48px`
- **Grid System:** 12 columns, max-width: 1280px, padding: 16px (mobile) / 24px (tablet) / 32px (desktop)
- **Breakpoints (Tailwind defaults):**
  - `sm: 640px`, `md: 768px`, `lg: 1024px`, `xl: 1280px`

#### 5.5 Component Library
Danh sách components cần có và spec cơ bản:
- **Buttons:** Primary, Secondary, Ghost, Danger — 3 sizes (sm/md/lg) — Loading state
- **Cards:** MangaCard (cover + title + tags + rating), InfoCard (flat), SkeletonCard (loading)
- **Forms:** Input, Textarea, Select, Checkbox, Toggle — với validation states (error/success)
- **Navigation:** Navbar (desktop sidebar / mobile bottom bar), Breadcrumb
- **Feedback:** Toast notifications, Modal/Dialog, Alert Banner, Spinner
- **AI-specific:** ChatBubble (user/bot), TypingIndicator, StreamingText

#### 5.6 Animation & Interaction Principles
- **Timing:** Micro-interactions: 150-200ms. Page transitions: 300ms.
- **Easing:** `ease-out` cho enter, `ease-in` cho exit.
- **Loading States:** Mọi async action phải có skeleton loader hoặc spinner.
- **Hover Effects:** Scale 1.02 cho cards, color shift cho buttons/links.
- **Không dùng:** Animation quá phức tạp gây mất tập trung vào nội dung.

#### 5.7 Accessibility (Khả năng Tiếp cận)
- Tỷ lệ tương phản tối thiểu WCAG AA (4.5:1 cho text thường, 3:1 cho text lớn)
- Mọi interactive element phải có `focus-visible` style
- Images phải có `alt` text mô tả
- Form inputs phải có `label` liên kết đúng

#### 5.8 Naming Convention cho Components
- Đặt tên theo pattern: `[Feature][Component].tsx`
  - e.g., `MangaCard.tsx`, `AuthLoginForm.tsx`, `ChatMessageBubble.tsx`
- Tổ chức theo feature folders trong `components/`:
  ```
  components/
  ├── common/    # Button, Input, Modal (dùng ở mọi nơi)
  ├── manga/     # MangaCard, MangaGrid, MangaDetail
  ├── chat/      # ChatWindow, ChatBubble, ChatInput
  ├── auth/      # LoginForm, RegisterForm
  └── layout/    # Navbar, Sidebar, Footer
  ```

---

## 6. `docs/deployment-guide.md`
> Đối tượng: DevOps Engineer, Backend Developer cần deploy  
> Mục tiêu: Hướng dẫn đầy đủ để đưa ứng dụng lên môi trường bất kỳ

### Cấu trúc

```
# MangaRec AI — Deployment Guide
```

#### 6.1 Prerequisites (Yêu cầu Tiên quyết)
- Docker Engine ≥ 24.x
- Docker Compose Plugin ≥ 2.x
- Git
- Domain + SSL certificate (production)
- Biến môi trường đã được cấu hình đầy đủ

#### 6.2 Environment Strategy (Chiến lược Môi trường)
| Môi trường | Mục đích | Cấu hình Docker |
|------------|----------|-----------------|
| **Development** | Local dev, hot-reload | `docker-compose.dev.yml` (root — include từng service) |
| **Staging** | Testing tích hợp, QA | *(Cần định nghĩa — recommended)* |
| **Production** | Live users | `docker-compose.yml` (multi-stage builds) |

*Cấu trúc Docker Compose:* Root `docker-compose.dev.yml` dùng `include:` để kết hợp:
- `ai-service/docker-compose.dev.yml` — `ai-service` + `ai-worker` + `qdrant`
- `be-service/docker-compose.dev.yml` — `be-service` + `postgres`
- `fe-service/docker-compose.dev.yml` — `fe-service`

Tất cả services dùng chung `mangarec_global_network` (external Docker network).

#### 6.3 Development Setup (Chi tiết)
Các bước từng dòng:
1. Clone repository
2. Copy `.env.example` → `.env` cho từng service
3. Điền biến môi trường cần thiết (xem bảng ở `codebase-summary.md`)
4. Tạo Docker network:
   ```bash
   docker network create mangarec_global_network
   ```
5. Khởi động tất cả services:
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```
6. Verify & Troubleshooting thông thường:
   ```bash
   docker compose -f docker-compose.dev.yml logs -f
   docker compose -f docker-compose.dev.yml ps
   ```
7. Service endpoints:
   - Frontend: `http://localhost:3000` ← Port **3000**, không phải 5173
   - Backend API: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - AI Service: `http://localhost:8000`
   - AI FastAPI Docs: `http://localhost:8000/docs`
   - Qdrant Dashboard: `http://localhost:6333/dashboard`

#### 6.4 Database Management
**PostgreSQL:**
```bash
# BE Migrations (Flyway — tự động khi service start)
# Check migration status:
docker exec -it be-service mvn flyway:info

# AI Migrations (Alembic)
docker exec -it ai-service uv run alembic upgrade head
docker exec -it ai-service uv run alembic history
```

**Qdrant — Khởi tạo Collections:**
```bash
# Chạy script khởi tạo (nếu có):
docker exec -it ai-service uv run python scripts/init_qdrant.py
```

#### 6.5 Production Deployment (Chi tiết)
1. **Chuẩn bị server:**
   - OS: Ubuntu 22.04 LTS (recommended)
   - Minimum: 4 vCPU, 8GB RAM, 50GB SSD
   - Cài Docker, Docker Compose, Nginx (reverse proxy ngoài Docker)
2. **Setup secrets:**
   - KHÔNG copy `.env` lên server trực tiếp (rủi ro bảo mật)
   - Dùng Docker Secrets hoặc environment variables của hosting platform
3. **Build & Deploy:**
   ```bash
   docker network create mangarec_global_network
   docker compose -f docker-compose.yml up -d --build
   ```
4. **Nginx Reverse Proxy (ngoài Docker):**
   - Frontend: `proxy_pass http://localhost:80`
   - Backend API: `proxy_pass http://localhost:8080`
   - AI Service: *Không expose public — chỉ internal*
5. **SSL/TLS:** Dùng Certbot + Let's Encrypt
6. **Health checks:**
   - BE: `GET /api/v1/health`
   - AI: `GET /health`

#### 6.6 CI/CD Pipeline (Khung Đề xuất)
*(Cần implement — placeholder cho GitHub Actions)*
```yaml
# .github/workflows/deploy.yml
# Trigger: push to main
# Steps:
# 1. Run tests (all 3 services)
# 2. Build Docker images
# 3. Push to Container Registry
# 4. SSH to server & pull + restart containers
```

#### 6.7 Monitoring & Logging
- **Log aggregation:** `docker compose logs -f [service-name]`
- **Cần implement:** Centralized logging (ELK stack / Grafana Loki)
- **Health monitoring:** Uptime checks cho các public endpoints
- **Alert:** Cảnh báo khi service down hoặc error rate tăng đột biến

#### 6.8 Backup Strategy
- **PostgreSQL:** Cron job `pg_dump` hàng ngày, lưu ≥ 7 ngày
- **Qdrant:** Snapshot API định kỳ
- **Recovery test:** Kiểm tra khôi phục backup mỗi tháng

#### 6.9 Rollback Procedure
1. Identify last stable image tag
2. Pull previous image: `docker pull <image>:<prev-tag>`
3. Restart service với image cũ
4. Rollback DB migration nếu cần:
   ```bash
   # Flyway: mvn flyway:undo
   # Alembic: docker exec ai-service uv run alembic downgrade -1
   ```

---

## 7. `docs/project-roadmap.md`
> Đối tượng: Product Manager, Stakeholder, toàn bộ team  
> Mục tiêu: Bức tranh tổng thể về tiến độ và hướng đi

### Cấu trúc

```
# MangaRec AI — Project Roadmap
```

#### 7.1 Vision & North Star (Tầm nhìn)
- **1-year goal:** Trở thành platform gợi ý manga AI hàng đầu tại Việt Nam với 10,000+ MAU.
- **3-year goal:** Mở rộng đa ngôn ngữ (EN, JP, KO), hỗ trợ Manhwa/Manhua, B2B API licensing.

#### 7.2 Current Status (Hiện trạng — as of Q2 2026)
Bảng trạng thái các module:
| Module | Status | Notes |
|--------|--------|-------|
| Auth (Email + Google) | 🟡 In Progress | JWT implemented, Google OAuth cần verify |
| AI Recommendation Engine | 🟡 In Progress | Qdrant + Embedding hoạt động, cần tuning |
| AI Chatbot (LangGraph) | 🟡 In Progress | Core graph xây dựng, cần thêm tools |
| Payment (payOS) | 🔴 Not Started | BE dependency ready |
| Frontend UI | 🟡 In Progress | Cấu trúc cơ bản, cần hoàn thiện pages |
| WebSocket Real-time | 🔴 Not Started | BE có WebSocket config |
| Admin Dashboard | 🔴 Not Started | — |
| Deployment (Docker) | 🟢 Done | Cấu hình Compose hoàn chỉnh |
| Documentation | 🟡 In Progress | Đây chính là công việc hiện tại |

#### 7.3 Release Plan

**Phase 1 — MVP (Target: Q3 2026)**  
Goal: Người dùng có thể đăng ký, đăng nhập, và nhận gợi ý manga từ AI.
- [ ] Hoàn thiện Auth Flow (Email + Google OAuth)
- [ ] Hoàn thiện AI Recommendation API + Qdrant ingestion pipeline
- [ ] Trang chủ + Trang kết quả gợi ý (Frontend)
- [ ] Hệ thống quản lý users cơ bản (Admin)
- [ ] Deploy lên môi trường staging

**Phase 2 — Beta (Target: Q4 2026)**  
Goal: Monetization + AI Chatbot.
- [ ] Tích hợp thanh toán payOS (gói Basic/Pro)
- [ ] Chatbot AI hoàn chỉnh (LangGraph + multi-turn)
- [ ] Lịch sử gợi ý & Cá nhân hoá
- [ ] WebSocket notifications
- [ ] Public launch + Marketing

**Phase 3 — Growth (Target: Q1-Q2 2027)**  
Goal: Mở rộng & Tối ưu.
- [ ] Multilingual support (EN primary)
- [ ] Mô hình recommendation nâng cao (collaborative filtering)
- [ ] Mobile optimization (PWA)
- [ ] Analytics dashboard cho users
- [ ] API public cho third-party (B2B)

#### 7.4 Technical Debt & Known Issues
Danh sách nợ kỹ thuật đang biết:
| Issue | Mức độ | Ghi chú |
|-------|--------|---------|
| Test coverage < 80% | HIGH | Cần ưu tiên trước Phase 2 |
| `app/` directory ở root chưa rõ vai trò | MEDIUM | Cần làm sạch hoặc document |
| Không có CI/CD pipeline | HIGH | Thêm vào Phase 2 |
| Monitoring/Alerting chưa có | MEDIUM | Cần trước production launch |
| Không có rate limiting trên AI endpoints | HIGH | Ngăn chặn abuse & cost overrun |

#### 7.5 Decision Log (Lịch sử Quyết định Kỹ thuật)
Ghi lại các quyết định quan trọng theo format ADR (Architecture Decision Record):

| # | Quyết định | Ngày | Lý do |
|---|-----------|------|-------|
| 001 | Dùng `uv` thay pip/poetry cho AI service | — | Tốc độ cài đặt nhanh hơn 10-100x |
| 002 | Chọn Qdrant làm Vector DB | — | Self-hosted, tốt hơn Pinecone về cost |
| 003 | Chọn Groq làm LLM provider | — | Tốc độ inference nhanh nhất hiện tại |
| 004 | Microservices thay vì Monolith | — | Phân tách team, scale độc lập từng service |
| 005 | payOS thay vì Stripe | — | Hỗ trợ thanh toán nội địa Việt Nam |

#### 7.6 Contributing & Onboarding
- **Đọc trước khi code:** `codebase-summary.md` → `code-standards.md` → `system-architecture.md`
- **Thời gian onboard ước tính:** 1-2 ngày để hiểu cấu trúc và chạy được local
- **Contact:** *(Thêm thông tin team leads theo từng domain)*

---

## Ghi chú cho Người Thực hiện

> **Thứ tự viết tài liệu được đề xuất:**
> 1. `codebase-summary.md` — Viết đầu tiên, làm nền tảng cho các file khác
> 2. `system-architecture.md` — Viết thứ hai, làm rõ thiết kế hệ thống
> 3. `code-standards.md` — Viết thứ ba, thiết lập luật chơi
> 4. `deployment-guide.md` — Viết thứ tư, dựa trên Docker config đã có
> 5. `design-guidelines.md` — Viết thứ năm, cần phân tích FE code kỹ hơn
> 6. `project-overview-pdr.md` — Viết thứ sáu, cần input từ PM/Stakeholder
> 7. `project-roadmap.md` — Viết cuối cùng, cần toàn bộ context

> **Mức độ effort ước tính:**
> | File | Effort | Người phụ trách |
> |------|--------|----------------|
> | `codebase-summary.md` | 2-3 giờ | Senior Dev |
> | `system-architecture.md` | 4-6 giờ | Architect |
> | `code-standards.md` | 3-4 giờ | Tech Lead |
> | `deployment-guide.md` | 3-4 giờ | DevOps/Senior Dev |
> | `design-guidelines.md` | 4-5 giờ | Frontend Lead |
> | `project-overview-pdr.md` | 2-3 giờ | PM + Tech Lead |
> | `project-roadmap.md` | 2-3 giờ | PM + Team |
> | **Tổng** | **~20-28 giờ** | |
