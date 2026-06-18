# MangaRec AI — Product Definition & Requirements (PDR)

> **Loại tài liệu:** Product Definition & Requirements Document  
> **Đối tượng:** Product Manager, Stakeholder, Developer mới onboard  
> **Cập nhật lần cuối:** 2026-06-15  
> **Trạng thái:** 🟡 In Progress — một số mục cần input từ PM/Stakeholder

---

## 1. Executive Summary (Tóm tắt Điều hành)

| Thuộc tính | Giá trị |
|---|---|
| **Tên dự án** | MangaRec AI |
| **Loại sản phẩm** | Micro-SaaS, AI-powered Manga Recommendation Platform |
| **Mô hình kinh doanh** | Freemium + Credit-based |
| **Thị trường mục tiêu** | Độc giả truyện tranh đa ngôn ngữ (ưu tiên tiếng Việt + tiếng Anh) |
| **Phiên bản hiện tại** | 0.1.0 (MVP — đang phát triển) |

**Elevator Pitch:** *(Cần điền từ PM/Stakeholder)*

---

## 2. Problem Statement (Bài toán Giải quyết)

Người dùng manga đang đối mặt với 3 vấn đề cốt lõi:

1. **Khó tìm manga phù hợp** trong kho nội dung khổng lồ — các nền tảng hiện tại thiếu công cụ lọc ngữ nghĩa sâu.
2. **Gợi ý truyền thống không cá nhân hoá** — các hệ thống dựa trên tag/genre đơn giản không nắm bắt được gu đọc phức tạp của người dùng.
3. **Thiếu công cụ tư vấn tương tác** — người dùng không thể trò chuyện và khám phá manga theo ngữ cảnh, sở thích, hay tâm trạng.

---

## 3. Solution Overview (Giải pháp Đề xuất)

MangaRec AI giải quyết các bài toán trên thông qua 3 trụ cột kỹ thuật:

### 3.1 AI Recommendation Engine
- **Cơ chế:** Vector Search (Qdrant) + Semantic Embedding (sentence-transformers)
- **Nguyên lý:** Biểu diễn manga và truy vấn người dùng dưới dạng vector ngữ nghĩa, tìm kiếm Top-K manga gần nhất trong không gian vector.
- **Ưu điểm:** Gợi ý theo ngữ nghĩa thực sự, không chỉ dựa trên tag/từ khóa cứng nhắc.

### 3.2 AI Chatbot (RAG)
- **Cơ chế:** LangGraph + LangChain/Groq (LLM inference)
- **Nguyên lý:** Retrieval-Augmented Generation — chatbot tìm kiếm dữ liệu manga liên quan từ Qdrant trước, sau đó dùng Groq LLM để sinh câu trả lời có ngữ cảnh.
- **Ưu điểm:** Tư vấn manga theo thời gian thực, hiểu ngữ cảnh hội thoại đa lượt.

### 3.3 SaaS Subscription & Monetization
- **Cơ chế:** Hệ thống Credit/Token + Gói dịch vụ Basic/Pro
- **Thanh toán:** Tích hợp payOS (hỗ trợ thanh toán nội địa Việt Nam)
- **Model:** Freemium — dùng cơ bản miễn phí, tính năng AI nâng cao cần credit/gói trả phí.

---

## 4. Core Features (Tính năng Cốt lõi)

Danh sách tính năng được phân loại theo mức độ ưu tiên:

### P0 — Must Have (MVP)
- [ ] **Đăng ký / Đăng nhập** — Email + Google OAuth (JWT-based)
- [ ] **Gợi ý Manga dựa trên AI** — Semantic Search qua Qdrant
- [ ] **Hệ thống Credit / Gói dịch vụ** — Quản lý quota người dùng

### P1 — Should Have (Beta)
- [ ] **Chatbot tương tác về Manga** — RAG với LangGraph, hỗ trợ multi-turn
- [ ] **Thanh toán tích hợp** — payOS (webhook + signature verification)
- [ ] **Thông báo Real-time** — WebSocket notifications

### P2 — Nice to Have (Growth)
- [ ] **Lịch sử đọc & Cá nhân hoá sâu** — Ghi nhớ preference người dùng
- [ ] **Đánh giá / Review manga** — User-generated content

---

## 5. User Personas (Chân dung Người dùng)

| Persona | Mô tả | Nhu cầu Chính |
|---|---|---|
| **Otaku Thông thường** | Đọc manga thường xuyên, muốn khám phá thể loại mới | Gợi ý chính xác theo gu đọc hiện tại |
| **Người dùng Casually** | Ít đọc, muốn gợi ý nhanh không cần tìm kiếm | Giao diện đơn giản, gợi ý 1-click |
| **Fan Cứng** | Đọc nhiều, muốn thảo luận chuyên sâu về manga | Chatbot tư vấn + khả năng so sánh/phân tích |

---

## 6. Success Metrics (Chỉ số Thành công)

| Chỉ số | Mục tiêu |
|---|---|
| Tỷ lệ chuyển đổi Freemium → Paid | > 5% |
| Số lượng AI query trung bình / người dùng / ngày | *(TBD)* |
| Độ chính xác gợi ý (User satisfaction score) | *(TBD)* |
| Thời gian phản hồi API | < 500ms (p95) |
| MAU (Monthly Active Users) sau 1 năm | 10,000+ |

---

## 7. Out of Scope (Ngoài Phạm vi)

Dự án **KHÔNG** thực hiện:
- ❌ **Lưu trữ / cung cấp nội dung manga** — Tránh vi phạm bản quyền; hệ thống chỉ gợi ý, không serve content.
- ❌ **Social features** (follow, friend list, chat giữa users) — Xem xét ở phiên bản v2 hoặc Phase 3.

---

## 8. Assumptions & Constraints (Giả định & Ràng buộc)

### Giả định
- Dữ liệu manga được thu thập từ nguồn bên ngoài (APIs / crawlers). MangaRec AI không tự tạo dữ liệu.
- Người dùng mục tiêu ban đầu có khả năng đọc tiếng Việt.

### Ràng buộc Kỹ thuật
- **Chi phí LLM:** Phụ thuộc vào Groq API — cần tối ưu token để kiểm soát chi phí. Prompts không được hardcode, lưu trong `core/prompts.py`.
- **Vector DB:** Qdrant được self-hosted (không dùng Pinecone managed) để kiểm soát chi phí.
- **Database AI Service:** AI service hiện dùng SQLite (`/app/data/manga_metadata.db`) cho metadata cục bộ.

### Ràng buộc Pháp lý
- Cần tuân thủ **PDPA** (Nghị định bảo vệ dữ liệu cá nhân người dùng Việt Nam).
- Không log sensitive user data (password, token, PII) ở bất kỳ layer nào.

---

## 9. Stakeholders (Các bên Liên quan)

| Bên liên quan | Vai trò | Quan tâm chính |
|---|---|---|
| Product Manager | Định nghĩa sản phẩm, ưu tiên feature | Business metrics, user satisfaction |
| Backend Developer | Xây dựng `be-service`, `ai-service` | API design, performance, security |
| Frontend Developer | Xây dựng `fe-service` | UX, tốc độ render, responsive |
| DevOps | Deploy, vận hành Docker infrastructure | Uptime, scalability, monitoring |
| End User | Sử dụng platform để tìm & khám phá manga | Độ chính xác gợi ý, tốc độ phản hồi |

---

> **Liên kết tài liệu liên quan:**
> - Kiến trúc hệ thống chi tiết → `docs/system-architecture.md`
> - Bản đồ codebase → `docs/codebase-summary.md`
> - Lộ trình phát triển → `docs/project-roadmap.md`
