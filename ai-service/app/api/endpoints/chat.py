from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Any
from app.chatbot.graph.dummy_graph import dummy_graph
from app.core.tracing import get_tracer

router = APIRouter()

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    reply: str
    steps: list[str]

@router.post("/", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest):
    try:
        # Prepare LangGraph state
        initial_state = {"input": request.message, "intermediate_steps": []}
        
        # Prepare tracer for graceful degradation
        # If API key is missing or invalid, get_tracer() returns None and we don't crash
        tracer = get_tracer()
        config = {}
        if tracer:
            config["callbacks"] = [tracer]
            
        # Invoke LangGraph
        result = dummy_graph.invoke(initial_state, config=config)
        
        return ChatResponse(
            reply=result.get("output", ""),
            steps=result.get("intermediate_steps", [])
        )
    except Exception as e:
        # Prevent the main process from crashing, but still return a standard HTTP 500
        # LangSmith errors during tracing usually don't reach here because they happen in background threads.
        raise HTTPException(status_code=500, detail=str(e))
