# MangaRec AI — Project Roadmap

> **Loại tài liệu:** Product Roadmap & Progress Tracker  
> **Đối tượng:** Product Manager, Stakeholder, toàn bộ team  
> **Cập nhật lần cuối:** 2026-06-15 (Q2 2026)  
> **Mục tiêu:** Bức tranh tổng thể về tiến độ và hướng đi

---

## 1. Vision & North Star (Tầm nhìn)

| Horizon | Mục tiêu |
|---|---|
| **1 năm** | Trở thành platform gợi ý manga AI hàng đầu tại Việt Nam với **10,000+ MAU** |
| **3 năm** | Mở rộng đa ngôn ngữ (EN, JP, KO), hỗ trợ Manhwa/Manhua, B2B API licensing |

**Điều hướng đọc tài liệu:**
> `codebase-summary.md` → `code-standards.md` → `system-architecture.md` → roadmap này

---

## 2. Current Status (Hiện trạng — Q2 2026)

### 2.1 Module Status

| Module | Status | Ghi chú chi tiết |
|---|---|---|
| **Auth — Email** | 🟡 In Progress | Đăng ký + OTP verify email + Forgot/Reset password đã có controller + request DTOs |
| **Auth — Google OAuth** | 🟡 In Progress | `GoogleLoginRequest` + `google-api-client` tích hợp; cần end-to-end verify |
| **JWT / Refresh Token** | 🟢 Implemented | `UserRefreshTokenEntity` có, rotation pattern cần verify |
| **AI Recommendation** | 🟡 In Progress | Qdrant + embedding (`sentence-transformers`) hoạt động; endpoint `/api/v1/search` đã có |
| **AI Chatbot (LangGraph)** | 🟡 In Progress | `bot_graph.py` có, endpoint `/api/v1/chat` có; cần thêm tools và multi-turn context |
| **Payment (payOS)** | 🔴 Schema Ready | `V2__payos_payment_schema.sql` đã migrate, `payos-java 2.0.1` đã import; controller chưa triển khai |
| **Frontend UI** | 🟡 In Progress | Pages: `auth/`, `dashboard/`, `home/` có cơ bản; components: `ChatPanel`, `Loading`, `GoogleSignInButton`, `FormAlert`, `PasswordField` |
| **WebSocket Real-time** | 🔴 Not Started | Dependency `spring-boot-starter-websocket` đã có trong `pom.xml` |
| **Admin Dashboard** | 🔴 Not Started | — |
| **Deployment (Docker)** | 🟢 Done | Production `docker-compose.yml` + Dev `docker-compose.dev.yml` hoàn chỉnh, health checks configured |
| **Database Migrations** | 🟢 Done | Flyway (BE): V1, V2 done. Alembic (AI): auto-run on startup |
| **AI Worker / Scheduler** | 🟡 In Progress | Container `ai-worker` chạy `scripts/scheduler.py` đã cấu hình trong Docker; logic cụ thể cần verify |
| **LangSmith Observability** | 🟡 Configured | Tracing config có trong `.env.example`; cần verify kết nối |
| **Documentation** | 🟡 In Progress | **Đây chính là công việc hiện tại** — 7 file docs đang hoàn thiện |

### 2.2 Đã Implement (Confirmed từ codebase)

**Infrastructure & DevOps:**
- ✅ Docker Compose Dev + Production với `include:` structure
- ✅ External Docker network `mangarec_global_network`
- ✅ Flyway container dedicated (chờ postgres healthy rồi migrate)
- ✅ Alembic auto-run on AI service startup
- ✅ Qdrant collection init on AI service startup
- ✅ AI model baked into production Docker image (không fetch runtime)
- ✅ Health checks: postgres, redis, fe-service, ai-service `/health`

**AI Service:**
- ✅ FastAPI app với lifespan management
- ✅ `/health` endpoint (check Qdrant + SQLite)
- ✅ `/api/v1/search` endpoint
- ✅ `/api/v1/chat` endpoint
- ✅ `MLManager` — load embedding model vào memory
- ✅ `bot_graph.py` — LangGraph graph definition
- ✅ Embedding service (sentence-transformers)
- ✅ Qdrant service + repository
- ✅ SQLite via SQLAlchemy + Alembic
- ✅ LangSmith tracing config

**Core Backend:**
- ✅ Domain entities: User, UserAuthIdentity, AuthActionToken, RefreshToken, ChatSession, GuestProfile, UserPreference
- ✅ `GlobalExceptionHandling` (@ControllerAdvice)
- ✅ Custom exceptions: Forbidden, InvalidData, RateLimit, ResourceNotFound, Unauthorized
- ✅ `ApiResponse` wrapper chuẩn
- ✅ `SecurityConfig` + `OpenApiConfig`
- ✅ `AuthController` với full auth flow requests

**Frontend:**
- ✅ React 19 + TypeScript + Vite 7 + Tailwind v4 + react-router-dom v7
- ✅ `@/` path alias
- ✅ CSS design system (variables, animations, gradient background)
- ✅ Google Sans Text font
- ✅ AppProviders + Router architecture
- ✅ Pages: auth, dashboard, home
- ✅ Auth context + useAuth hook

---

## 3. Release Plan

### Phase 1 — MVP (Target: Q3 2026)
**Goal:** Người dùng có thể đăng ký, đăng nhập, và nhận gợi ý manga từ AI.

| Task | Status | Priority |
|---|---|---|
| Hoàn thiện Auth Flow (Email OTP end-to-end) | 🟡 In Progress | P0 |
| Hoàn thiện Google OAuth (verify flow) | 🟡 In Progress | P0 |
| AI Recommendation API — production ready | 🟡 In Progress | P0 |
| Qdrant data ingestion pipeline (manga data) | 🔴 Not Started | P0 |
| Trang chủ (Landing Page) hoàn chỉnh | 🟡 In Progress | P0 |
| Trang kết quả gợi ý (Search Results Page) | 🔴 Not Started | P0 |
| User dashboard cơ bản | 🟡 In Progress | P1 |
| Admin — User management (basic) | 🔴 Not Started | P1 |
| Deploy lên môi trường staging | 🔴 Not Started | P1 |
| Test coverage ≥ 80% | 🔴 Not Started | P1 |

### Phase 2 — Beta (Target: Q4 2026)
**Goal:** Monetization + AI Chatbot hoàn chỉnh.

| Task | Status | Priority |
|---|---|---|
| Tích hợp thanh toán payOS (gói Basic/Pro) | 🔴 Schema Ready | P0 |
| Chatbot AI hoàn chỉnh (LangGraph + multi-turn + tools) | 🟡 In Progress | P0 |
| Lịch sử gợi ý & Cá nhân hoá | 🔴 Not Started | P1 |
| WebSocket real-time notifications | 🔴 Not Started | P1 |
| CI/CD Pipeline (GitHub Actions) | 🔴 Not Started | P0 |
| Rate limiting trên AI endpoints | 🔴 Not Started | P0 |
| Public launch + Marketing | 🔴 Not Started | P2 |

### Phase 3 — Growth (Target: Q1–Q2 2027)
**Goal:** Mở rộng & Tối ưu.

| Task | Priority |
|---|---|
| Multilingual support (EN primary) | P1 |
| Mô hình recommendation nâng cao (collaborative filtering) | P1 |
| Mobile optimization (PWA) | P2 |
| Analytics dashboard cho users | P2 |
| API public cho third-party (B2B licensing) | P3 |

---

## 4. Technical Debt & Known Issues

| Issue | Mức độ | Ghi chú |
|---|---|---|
| Test coverage < 80% (cả 3 service) | 🔴 HIGH | Bắt buộc trước Phase 2 |
| Không có CI/CD pipeline | 🔴 HIGH | GitHub Actions — thêm vào Phase 2 |
| Không có rate limiting trên AI endpoints | 🔴 HIGH | Ngăn abuse & cost overrun Groq API |
| `app/` directory ở root chưa rõ vai trò | 🟡 MEDIUM | Cần làm sạch hoặc document |
| Không có centralized monitoring/alerting | 🟡 MEDIUM | Cần trước production launch |
| `pytest-asyncio` chưa có trong AI `pyproject.toml` | 🟡 MEDIUM | Cần bổ sung để test async endpoints |
| Staging environment chưa defined | 🟡 MEDIUM | Cần trước Phase 2 |
| payOS controller chưa triển khai | 🟡 MEDIUM | Schema đã có, cần implement logic |

---

## 5. Decision Log — Architecture Decision Records (ADR)

| # | Quyết định | Ngày | Lý do |
|---|---|---|---|
| 001 | Dùng `uv` thay pip/poetry cho AI service | — | Tốc độ cài đặt nhanh hơn 10–100x; deterministic lockfile |
| 002 | Chọn Qdrant làm Vector DB | — | Self-hosted, cost thấp hơn Pinecone; qdrant-client 1.18.0 |
| 003 | Chọn Groq làm LLM provider | — | Inference speed nhanh nhất hiện tại; langchain-groq |
| 004 | Microservices Monorepo thay Monolith | — | Phân tách team, scale độc lập từng service |
| 005 | payOS thay vì Stripe | — | Hỗ trợ thanh toán nội địa Việt Nam |
| 006 | SQLite cho AI metadata (không phải PostgreSQL) | — | Đơn giản, zero config, phù hợp với AI service isolated |
| 007 | AI model baked vào Docker image | — | Tránh download runtime → startup nhanh, offline-capable |
| 008 | Flyway dedicated container | — | Tách biệt migration lifecycle, chờ postgres healthy |
| 009 | Google Sans Text làm font chính | — | Đã implement trong codebase; hiện đại, đọc tốt |
| 010 | React Router v7 (`createBrowserRouter`) | — | Modern API, type-safe routing, loader support |

---

## 6. Contributing & Onboarding

### Thứ tự đọc tài liệu cho dev mới
1. **`codebase-summary.md`** — Biết "con gì ở đâu" (15 phút)
2. **`code-standards.md`** — Hiểu luật chơi
3. **`system-architecture.md`** — Hiểu luồng dữ liệu
4. **`deployment-guide.md`** — Chạy được local
5. **`design-guidelines.md`** — (Frontend dev) Hiểu design system

### Thời gian onboard ước tính
- **Đọc docs + setup local:** 1–2 ngày
- **Làm quen codebase đủ để commit:** 2–3 ngày thêm

### Effort ước tính theo loại tài liệu (reference)
| File | Effort | Người phụ trách |
|---|---|---|
| `codebase-summary.md` | 2–3 giờ | Senior Dev |
| `system-architecture.md` | 4–6 giờ | Architect |
| `code-standards.md` | 3–4 giờ | Tech Lead |
| `deployment-guide.md` | 3–4 giờ | DevOps / Senior Dev |
| `design-guidelines.md` | 4–5 giờ | Frontend Lead |
| `project-overview-pdr.md` | 2–3 giờ | PM + Tech Lead |
| `project-roadmap.md` | 2–3 giờ | PM + Team |
| **Tổng** | **~20–28 giờ** | |

### Contact
*(Thêm thông tin team leads theo từng domain)*

---

> **Cập nhật roadmap này:** Sau mỗi sprint/milestone, cập nhật bảng Status và Technical Debt.  
> **PRD đầy đủ:** `docs/project-overview-pdr.md`
