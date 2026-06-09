# Walkthrough: MR-0017 | Core Chatbot RAG Flow & Sandbox UI

Ticket MR-0017 đã được hoàn tất và tuân thủ chặt chẽ `coding-rules.md` cùng các nguyên tắc kỹ thuật.

## 1. Xây dựng Core LangGraph Thực tế
- **`app/chatbot/graph/bot_graph.py`**: Mình đã xây dựng đồ thị `StateGraph` với 2 node:
  1. `retriever_node`: Trích xuất `SearchService` (với DB & Qdrant session) từ `RunnableConfig` (được inject vào lúc `invoke`) và gọi `hybrid_search` để lấy 5 kết quả tìm kiếm.
  2. `reasoner_node`: Sử dụng **ChatGroq** (`llama-3.3-70b-versatile`) với Prompt tiếng Việt để giải thích và tư vấn dựa trên danh sách truyện thu thập được.

## 2. API Endpoint Cải tiến
- **`app/api/endpoints/chat.py`**: API POST `/api/v1/chat` giờ đây đã sử dụng `bot_graph`. Các dependency `db` và `qdrant` của FastAPI được truyền an toàn qua cấu hình của LangGraph (tránh khởi tạo lại Graph gây chậm).
- Dữ liệu trả về đúng chuẩn JSON: `{ "answer": "...", "mangas": [...] }`.

## 3. Sandbox UI Tuyệt Đẹp (Aesthetics 🌟)
- **`static/index.html`**: File tĩnh chứa giao diện Sandbox. Trái ngược với giao diện trắng đen nhàm chán, mình đã thiết kế một UI **Dark Mode Glassmorphism** cực xịn xò bằng Vanilla JS/CSS.
- UI có Typing Indicator khi chờ API, các message bong bóng trượt mượt mà (micro-animations), và danh sách truyện gợi ý xuất hiện bắt mắt.
- FastAPI đã được mount thư mục `static/` ở Endpoint `/static` (bằng cách cập nhật `app/main.py`).

## 4. Execution Logs
- Theo đúng quy trình của dự án, mọi thao tác chỉnh sửa source code đều đã được mình log đầy đủ vào file:
  `ai-service/.agent-execution/MR-0017/log/execution__implementation_plan.md`

---

## 🔥 Hướng dẫn Nghiệm thu (E2E Test)

Bạn có thể tự tay chạy nghiệm thu cực kỳ dễ dàng:

1. Hãy chắc chắn rằng bạn đã điền `GROQ_API_KEY` của mình vào `.env` (copy từ `.env.example`).
2. Mở terminal và chạy lệnh:
   ```bash
   uv run fastapi dev app/main.py
   ```
3. Mở trình duyệt và truy cập: **[http://localhost:8000/static/index.html](http://localhost:8000/static/index.html)**
4. Gõ thử: *"Tìm cho tôi vài bộ truyện thể loại hành động kịch tính"*. Bạn sẽ thấy UI xoay spinner và hiển thị kết quả từ DB thật cùng lời tư vấn của AI!
