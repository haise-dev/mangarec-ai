# Báo cáo Phân tích Codebase: MangaRec AI

Dưới đây là tổng hợp toàn bộ sự thật về codebase của dự án **mangarec-ai** dựa trên kết quả rà quét ban đầu.

## 1. Cấu trúc Thư mục

Dự án được tổ chức theo kiến trúc microservices với các thành phần chính sau:

- `fe-service/`: Chứa mã nguồn của Frontend (Giao diện người dùng).
- `be-service/`: Chứa mã nguồn của Core Backend (Xử lý nghiệp vụ chính, database chính).
- `ai-service/`: Chứa mã nguồn của AI Backend (Hệ thống gợi ý, chatbot, RAG).
- `docker-compose.yml` / `docker-compose.dev.yml`: Các file cấu hình Docker để chạy đồng thời các service.
- `.agent/`: Chứa các cấu hình, workflow và kỹ năng của hệ thống agent (Everything Claude Code - ECC).
- `app/`: Thư mục mã nguồn phụ (có thể là module dùng chung hoặc tàn dư của phiên bản trước, cấu trúc bên trong tương đồng với thư mục `ai-service/app`).
- `tests/` và `docs/`: Chứa các bài kiểm thử và tài liệu dự án.

## 2. Công nghệ Đang sử dụng (Tech Stack)

### Frontend (`fe-service`)
- **Core:** React 19, TypeScript, Vite.
- **Styling:** Tailwind CSS v4.
- **Routing & Fetching:** React Router DOM v7, Axios.
- **Tooling:** ESLint, Prettier.

### Core Backend (`be-service`)
- **Core:** Java 17, Spring Boot 3.3.5.
- **Database & Caching:** PostgreSQL, Redis, Flyway (Database Migration), Spring Data JPA.
- **Security:** Spring Security, JWT (JSON Web Token), Google API Client (cho Google Login).
- **Payment:** payOS (Cổng thanh toán nội địa).
- **Communication:** WebSocket, Restful API.
- **Documentation:** Springdoc OpenAPI (Swagger UI).
- **Build Tool:** Maven.

### AI Backend (`ai-service`)
- **Core:** Python 3.12, FastAPI, Uvicorn.
- **AI/LLM Frameworks:** LangGraph, LangChain (Groq), Sentence Transformers (Mô hình embedding).
- **Vector Database:** Qdrant Client.
- **Database (Relational):** SQLAlchemy, Alembic (Migration).
- **Package Manager:** `uv` (thay vì pip/poetry).
- **Khác:** Pydantic (Validation), Tenacity (Retry mechanisms), Schedule (Cron jobs).

## 3. Tiêu chuẩn Code (Coding Standards) & Tooling

- **Frontend:**
  - Sử dụng TypeScript hoàn toàn (Strict mode).
  - Linter: ESLint.
  - Formatter: Prettier.
  - Quản lý package bằng `npm` (qua `package-lock.json`).

- **Core Backend:**
  - Tiêu chuẩn của Spring Boot với Lombok để giảm boilerplate code.
  - Xác thực payload bằng `spring-boot-starter-validation`.
  - Quản lý version database qua Flyway.

- **AI Backend:**
  - Quản lý dependencies siêu tốc qua `uv`.
  - Linter & Formatter: `Ruff` (được cấu hình trong `pyproject.toml` giới hạn line-length là 88, bắt lỗi các chuẩn E, F, I, W, B).
  - Testing: `pytest`.

- **Toàn cục (Dựa trên rule của Agent/ECC):**
  - **Test-Driven:** Khuyến khích viết test trước (TDD), độ phủ code tối thiểu 80%.
  - **Immutability:** Ưu tiên tạo object mới thay vì mutate (đột biến) object cũ.
  - **Security-First:** Không hardcode secrets, chống SQL Injection/XSS/CSRF.

## 4. Luồng Nghiệp vụ Chính

Dự án **mangarec-ai** là một hệ thống Micro-SaaS cung cấp dịch vụ gợi ý truyện tranh (Manga) đa ngôn ngữ tích hợp AI. Các luồng nghiệp vụ chính bao gồm:

1. **Gợi ý Truyện tranh (AI Recommendation):**
   - Người dùng tương tác với hệ thống để lấy danh sách gợi ý.
   - `ai-service` nhận yêu cầu, sử dụng mô hình nhúng (Sentence Transformers) và Qdrant (Vector DB) để tìm kiếm và đưa ra các manga phù hợp.
   
2. **Chatbot Tương tác (AI Chatbot):**
   - Tích hợp LangGraph để xử lý logic hội thoại phức tạp.
   - Cho phép người dùng trò chuyện, đặt câu hỏi về manga, hoặc nhận tư vấn trực tiếp từ AI.

3. **Quản lý Tài khoản & Xác thực:**
   - Xử lý bởi `be-service`. Hỗ trợ đăng nhập truyền thống, đăng nhập qua Google (OAuth) và quản lý phiên bằng JWT. Hỗ trợ gửi email xác thực (OTP).

4. **Thanh toán & Gói dịch vụ (SaaS):**
   - Hỗ trợ thanh toán qua cổng payOS, cho phép người dùng mua các gói dịch vụ cao cấp hoặc nạp credit/token để sử dụng tính năng AI.

5. **Giao tiếp Real-time:**
   - Sử dụng WebSocket trong `be-service` để có thể cập nhật trạng thái hệ thống, thông báo hoặc luồng chat real-time.
