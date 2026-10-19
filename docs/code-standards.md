# MangaRec AI — Code Standards & Best Practices

> **Loại tài liệu:** Code Standards & Engineering Guidelines  
> **Đối tượng:** Mọi Developer, Code Reviewer  
> **Cập nhật lần cuối:** 2026-06-15  
> **Mục tiêu:** "Luật chơi" để code của team nhất quán, bảo mật, và dễ bảo trì

---

## 1. Nguyên tắc Cốt lõi (Core Principles)

Áp dụng cho **toàn bộ** codebase — không phân biệt service hay ngôn ngữ:

1. **Test-Driven Development (TDD)** — Viết test trước, code sau. Coverage tối thiểu **80%**.
2. **Immutability First** — Không mutate object/state trực tiếp. Luôn trả về bản sao mới với thay đổi được áp dụng.
3. **Security-First** — Không hardcode secrets (API keys, passwords, tokens). Validate mọi input từ người dùng.
4. **Small, Focused Functions** — Hàm ≤ 50 lines. File ≤ 800 lines. Không nesting > 4 cấp.
5. **Explicit Error Handling** — Không swallow errors. Log với context đầy đủ ở server-side. UI nhận thông báo lỗi thân thiện.

---

## 2. Frontend Standards — React / TypeScript (`fe-service/`)

### 2.1 TypeScript
- **Strict mode bật** trong `tsconfig.app.json`.
- **Không dùng `any`** trừ trường hợp đặc biệt — phải có comment giải thích rõ lý do.
- Dùng `interface` cho Props types (không dùng `type` cho props component).

### 2.2 Component Rules
- **Một file = Một component** (ngoại lệ: sub-components nhỏ nội bộ không export).
- Chỉ dùng **functional components + hooks** — không dùng class components.
- Props phải được định nghĩa bằng `interface`:
  ```typescript
  interface MangaCardProps {
    title: string;
    coverUrl: string;
    tags: string[];
  }
  
  const MangaCard = ({ title, coverUrl, tags }: MangaCardProps) => { ... };
  ```

### 2.3 State Management
| Loại State | Giải pháp |
|---|---|
| Local UI state | `useState`, `useReducer` |
| Server/async state | Custom hooks + Axios |
| Global state | Context API (tránh prop drilling > 2 cấp) |

### 2.4 Naming Conventions

| Loại | Convention | Ví dụ |
|---|---|---|
| Component files | `PascalCase.tsx` | `MangaCard.tsx`, `AuthLoginForm.tsx` |
| Hook files | `useCamelCase.ts` | `useAuth.ts`, `useMangaSearch.ts` |
| Utility files | `camelCase.ts` | `formatDate.ts`, `apiHelpers.ts` |
| Type/interface files | `camelCase.types.ts` hoặc trong `types/` | `manga.types.ts` |

**Pattern đặt tên component:** `[Feature][Component].tsx`
- `MangaCard.tsx`, `AuthLoginForm.tsx`, `ChatMessageBubble.tsx`

### 2.5 Component Organization (`src/components/`)
```
components/
├── common/    # Button, Input, Modal — dùng ở mọi nơi
├── manga/     # MangaCard, MangaGrid, MangaDetail
├── chat/      # ChatWindow, ChatBubble, ChatInput
├── auth/      # LoginForm, RegisterForm
└── layout/    # Navbar, Sidebar, Footer
```

### 2.6 Styling
- Dùng **Tailwind CSS v4** utility classes làm ưu tiên đầu tiên.
- **Không viết inline styles** trừ trường hợp dynamic values không thể express bằng Tailwind.
- Custom CSS (khi Tailwind không đủ) đặt trong `src/styles/`.

### 2.7 Linting & Formatting
- **ESLint:** `eslint.config.js` — bao gồm `eslint-plugin-react-hooks` và `eslint-plugin-react-refresh`.
- **Prettier:** `.prettierrc` — chạy `npm run format` trước khi commit.
- **CI check:** `npm run format:check` và `npm run lint` phải pass.

### 2.8 Testing
- **Framework:** Vitest + React Testing Library.
- Test **behavior** (những gì người dùng thấy/làm), không test implementation details.
- Mock API calls bằng MSW hoặc `vi.mock()`.

---

## 3. Core Backend Standards — Java / Spring Boot (`be-service/`)

### 3.1 Code Style
- **Lombok:** Dùng `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor` để giảm boilerplate.
- **Dependency Injection:** Chỉ dùng **constructor injection** — tuyệt đối không dùng field injection (`@Autowired` trên field).
  ```java
  // ✅ Đúng
  @RequiredArgsConstructor
  public class MangaService {
      private final MangaRepository mangaRepository;
  }
  
  // ❌ Sai
  @Autowired
  private MangaRepository mangaRepository;
  ```
- Record classes cho DTOs bất biến (nếu phù hợp với Java 17).

### 3.2 API Design
- **RESTful** — tuân thủ HTTP verbs đúng nghĩa:
  - `GET` — Đọc dữ liệu (idempotent)
  - `POST` — Tạo mới resource
  - `PUT` / `PATCH` — Cập nhật (PUT = toàn bộ, PATCH = một phần)
  - `DELETE` — Xoá resource
- **Tất cả endpoints** có prefix `/api/v1/`
- **Response Envelope chuẩn:**
  ```json
  {
    "success": true,
    "data": { ... },
    "message": "Thao tác thành công",
    "pagination": { "page": 1, "size": 20, "total": 100 }
  }
  ```
- **Validate input** bằng `@Valid` + Bean Validation annotations (`@NotNull`, `@Size`, v.v.).

### 3.3 Security Rules
- **Không bao giờ** trả về stacktrace trong response ở môi trường production.
- Dùng **parameterized queries** (Spring Data JPA xử lý tự động) — không concat string SQL thủ công.
- Mọi endpoint cần xác thực phải được bảo vệ bằng `@PreAuthorize`.
- JWT: Access Token **short-lived**, Refresh Token **rotation** (vô hiệu hoá token cũ sau mỗi lần refresh).

### 3.4 Database (PostgreSQL + Flyway)
- **Mọi thay đổi schema** phải qua Flyway migration files — không sửa trực tiếp DB.
- **Naming convention migration:** `V{version}__{description}.sql`
  - Ví dụ: `V1__create_users_table.sql`, `V2__add_plans_table.sql`
- Không lazy-load trong vòng lặp — gây **N+1 problem**.
- Migration chạy tự động khi service start (`SPRING_FLYWAY_ENABLED=true`).

### 3.5 Testing
- **Framework:** JUnit 5 + Mockito
- Phân tách rõ ràng:
  - Unit Tests → `src/test/unit/` — mock tất cả dependencies
  - Integration Tests → `src/test/integration/` — test với DB thực (TestContainers)

---

## 4. AI Backend Standards — Python / FastAPI (`ai-service/`)

### 4.1 Code Style
- **Linter:** Ruff — config trong `pyproject.toml`:
  ```toml
  [tool.ruff]
  line-length = 88
  target-version = "py312"
  
  [tool.ruff.lint]
  select = ["E", "F", "I", "W", "B"]
  ```
- **Type hints bắt buộc** cho tất cả function signatures.
  ```python
  # ✅ Đúng
  async def get_recommendations(query: str, limit: int = 10) -> list[MangaResult]:
      ...
  
  # ❌ Sai
  async def get_recommendations(query, limit=10):
      ...
  ```
- **Docstring:** Theo Google Style.
  ```python
  def embed_text(text: str) -> list[float]:
      """Chuyển đổi text thành vector embedding.
  
      Args:
          text: Văn bản cần embed.
  
      Returns:
          Vector embedding dạng list of floats.
      
      Raises:
          EmbeddingError: Nếu model không thể xử lý text.
      """
  ```

### 4.2 API Design (FastAPI)
- **Tất cả request/response** validate qua **Pydantic models** — không nhận raw dict.
- **Dependency injection** qua FastAPI `Depends()` — không dùng global variables.
  ```python
  # ✅ Đúng
  @router.get("/recommendations")
  async def get_recommendations(
      query: str,
      service: RecommendationService = Depends(get_recommendation_service),
  ) -> RecommendationResponse:
      ...
  ```
- **Async functions** (`async def`) cho **mọi I/O-bound operations** (DB queries, API calls, file I/O).

### 4.3 AI/LLM Specific Rules
- **Không hardcode prompts** — prompts là config, lưu trong `app/core/prompts.py` hoặc file riêng.
- **Luôn dùng Tenacity** cho LLM/external API calls — retry với exponential backoff:
  ```python
  from tenacity import retry, stop_after_attempt, wait_exponential
  
  @retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=4, max=10))
  async def call_groq_llm(prompt: str) -> str:
      ...
  ```
- **Log token usage** — kiểm soát chi phí Groq API.
- **Giới hạn context window** — tóm tắt hội thoại khi conversation history vượt ngưỡng.
- **LangSmith tracing** — bật trong development để debug LangGraph flows.

### 4.4 Repository Pattern
- **Bắt buộc** dùng Repository Pattern — không query trực tiếp từ service layer.
- Service layer chỉ biết về interface của repository, không biết về SQLAlchemy/Qdrant internals.
  ```
  FastAPI Router → Service (business logic) → Repository (data access) → DB/Qdrant
  ```

### 4.5 Database (SQLite + Qdrant)
- **SQLite** (`manga_metadata.db`): Lưu metadata manga cục bộ — migrations qua Alembic.
- **Qdrant**: Vector storage — collections tên `manga_embeddings` (vector: title + description + tags).
- **Mọi schema change** qua Alembic migrations:
  ```bash
  uv run alembic revision --autogenerate -m "add_manga_table"
  uv run alembic upgrade head
  ```

### 4.6 Testing
- **Framework:** pytest + pytest-asyncio
- **Mock external APIs** (Groq, Qdrant) trong unit tests — không gọi API thật trong unit tests.

---

## 5. Git Workflow & Commit Convention

### 5.1 Branch Naming
Quy tắc đặt tên nhánh yêu cầu gắn mã ticket Jira (ví dụ: MR-XXXX) vào sau tiền tố loại nhánh:
```
{type}/MR-{ticket_number}-{mô-tả-ngắn}
```

**Ví dụ:**
```
feat/MR-0001-setup-env         # Feature mới liên quan ticket MR-0001
fix/MR-0002-fix-login          # Bug fix liên quan ticket MR-0002
refactor/MR-0003-clean-code    # Refactoring
docs/MR-0004-update-readme     # Tài liệu
chore/MR-0005-update-deps      # Maintenance
```

### 5.2 Commit Messages (Conventional Commits)
Thông điệp commit bắt buộc phải có mã ticket ở đầu (cách nhau bởi ký tự `|`), tiếp theo là định dạng Conventional Commits.
```
MR-XXXX | <type>: <mô tả ngắn gọn (imperative mood)>

[body tùy chọn — giải thích WHY, không phải WHAT]
```

**Types:**
| Type | Dùng khi |
|---|---|
| `feat` | Thêm tính năng mới |
| `fix` | Sửa bug |
| `refactor` | Tái cấu trúc code (không thêm feature, không fix bug) |
| `docs` | Cập nhật tài liệu |
| `test` | Thêm/sửa test |
| `chore` | Maintenance (deps, config, scripts) |
| `perf` | Cải thiện performance |
| `ci` | Thay đổi CI/CD pipeline |

**Ví dụ:**
```
MR-0001 | feat: add manga recommendation endpoint with Qdrant search

Implements semantic search using sentence-transformers embeddings.
Returns top-K results sorted by cosine similarity score.
```

### 5.3 Pull Request Rules
- **Mô tả rõ ràng:** Vấn đề gì → Giải pháp gì → Cách test
- **Cần ít nhất 1 reviewer** approve trước khi merge
- **Không merge** khi CI/CD thất bại
- **Squash merge** để giữ lịch sử sạch (tùy team convention)

---

## 6. Security Checklist (Bắt buộc trước mỗi commit)

Trước khi mở Pull Request, kiểm tra toàn bộ mục sau:

- [ ] Không có credentials/API keys/tokens trong code hoặc commit history
- [ ] Tất cả user input được validate (Bean Validation / Pydantic)
- [ ] Không có SQL string concatenation thủ công
- [ ] Không log sensitive user data (password, token, email, PII)
- [ ] Dependencies không có CVE nghiêm trọng:
  - Frontend: `npm audit`
  - Backend: `mvn dependency-check:check` hoặc kiểm tra thủ công
  - AI: `uv pip compile --audit` hoặc `safety check`
- [ ] Không có file `.env` thực trong staged changes (kiểm tra `.gitignore`)

---

## 7. Tham khảo

| Tài liệu | Link |
|---|---|
| Bản đồ codebase | `docs/codebase-summary.md` |
| Kiến trúc hệ thống | `docs/system-architecture.md` |
| Hướng dẫn thiết kế UI | `docs/design-guidelines.md` |
| Hướng dẫn deploy | `docs/deployment-guide.md` |
| Conventional Commits spec | https://www.conventionalcommits.org |
| Ruff config hiện tại | `ai-service/pyproject.toml` |
| ESLint config hiện tại | `fe-service/eslint.config.js` |
