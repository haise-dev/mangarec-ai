## [2026-06-09 16:20:00] Task: Implement Core RAG Flow and API
- **Action:** Create & Update
- **Files Affected:**
  - `app/core/config.py`
  - `app/chatbot/graph/bot_graph.py`
  - `app/chatbot/graph/dummy_graph.py` (Deleted)
  - `app/api/endpoints/chat.py`
- **Summary:** Added GROQ config, created real LangGraph for chatbot (retriever + reasoner), removed dummy graph, and updated chat API to inject dependencies (`db`, `qdrant`).
- **Verify:** Run FastAPI server and test via UI.
- **Status:** ✅ Success

## [2026-06-09 16:21:00] Task: Implement Sandbox UI
- **Action:** Create & Update
- **Files Affected:**
  - `static/index.html`
  - `app/main.py`
- **Summary:** Created beautiful Glassmorphism Dark Mode UI in `index.html` to query `/api/v1/chat/`. Mounted static files in `main.py`.
- **Verify:** Open `http://localhost:8000/static/index.html` in browser.
- **Status:** ✅ Success
