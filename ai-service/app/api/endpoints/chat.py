from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from typing import Any
from sqlalchemy.orm import Session
from qdrant_client import QdrantClient

from app.chatbot.graph.bot_graph import bot_graph
from app.core.tracing import get_tracer
from app.db.session import get_db
from app.core.qdrant import get_qdrant_client

router = APIRouter()

class ChatRequest(BaseModel):
    query: str

class ChatResponse(BaseModel):
    answer: str
    mangas: list[dict]

@router.post("/", response_model=ChatResponse)
async def chat_endpoint(
    request: ChatRequest,
    db: Session = Depends(get_db),
    qdrant: QdrantClient = Depends(get_qdrant_client)
):
    try:
        # Prepare LangGraph state
        initial_state = {"query": request.query, "mangas": [], "answer": ""}
        
        # Prepare tracer for graceful degradation
        tracer = get_tracer()
        config = {
            "configurable": {
                "db": db,
                "qdrant": qdrant
            }
        }
        if tracer:
            config["callbacks"] = [tracer]
            
        # Invoke LangGraph
        result = bot_graph.invoke(initial_state, config=config)
        
        return ChatResponse(
            answer=result.get("answer", ""),
            mangas=result.get("mangas", [])
        )
    except Exception as e:
        # Prevent the main process from crashing, but still return a standard HTTP 500
        raise HTTPException(status_code=500, detail=str(e))

