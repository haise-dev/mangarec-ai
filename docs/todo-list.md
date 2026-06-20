# Danh sách các hạng mục cần hoàn thiện (Todo-List)

Tài liệu này ghi chú các tính năng/lỗi còn thiếu sót (functional gaps) cần ưu tiên giải quyết ngay lập tức để hoàn thiện luồng Chatbot đa lượt (multi-turn) và hệ thống, trước khi chuyển sang các phần DevOps hoặc Admin Dashboard.

## 1. AI Service (Khôi phục trí nhớ cho AI & LangGraph)
- [ ] **Nhận ngữ cảnh hội thoại (`chat.py`)**: Cập nhật `ChatRequest` schema để chấp nhận truyền thêm `chat_history` hoặc mảng messages từ Backend, thay vì chỉ nhận mỗi `query`.
- [ ] **Bổ sung State cho LangGraph (`bot_graph.py`)**: Sửa lại `AgentState` để có thuộc tính lưu trữ ngữ cảnh hội thoại cũ.
- [ ] **Refactor Graph thành Agent thực thụ**:
    - Chuyển logic gọi `hybrid_search` từ một node cứng nhắc thành một **Tool** (trong thư mục `app/tools`).
    - Cho phép LLM tự suy luận: Nếu user chỉ chào hỏi (không cần tìm truyện), LLM có thể trả lời trực tiếp mà không cần trigger Qdrant Search.
- [ ] **Chống lỗi đứt gãy**: Đảm bảo AI Service trả về schema chuẩn hóa ngay cả khi xảy ra lỗi.

## 2. Core Backend (Đồng bộ lưu trữ & Chuyển tiếp lịch sử)
- [ ] **API lấy lịch sử chat (`ChatController.java`)**: Cần viết thêm endpoint `GET /api/chat/{sessionId}` và `GET /api/chat/sessions` để Frontend có thể lấy lại danh sách các phiên chat và tin nhắn cũ.
- [ ] **Chuyển tiếp History cho AI (`ChatServiceImpl.java`)**: Sửa logic khi gọi REST API sang `ai-service`: cần truy vấn các tin nhắn gần nhất (`ChatMessageEntity`) của user và đính kèm vào payload gửi sang AI để AI có ngữ cảnh trả lời.

## 3. Frontend UI (Phục hồi trải nghiệm người dùng)
- [ ] **Quản lý Session trong Chat**: `ChatPanel.tsx` cần gửi và lưu lại `session_id` để biết user đang ở phiên trò chuyện nào (thay vì mỗi tin nhắn khởi tạo một session mới nếu logic BE đang không xử lý tốt việc tự bắt guest session).
- [ ] **Render lại lịch sử chat**: Khi user quay lại trang Home hoặc vào Dashboard, Frontend cần gọi API `GET /api/chat/{sessionId}` để nạp lại danh sách tin nhắn (`entries`) thay vì luôn hiển thị khung chat trống tinh khôi như hiện tại.

## 4. Các nợ kỹ thuật (Nên làm sau khi xong 3 mục trên)
- [ ] Mở rộng Rule của `AiRateLimitFilter` cho cả các endpoint Recommendation.
- [ ] Review và xử lý dứt điểm thư mục `app/` nằm trơ trọi ở root.
- [ ] Viết thêm Unit Test cho luồng Chat (cả BE và FE).
