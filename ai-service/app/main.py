import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Any, Optional
from groq import RateLimitError
from langchain_core.messages import HumanMessage

from app.graph.workflow import workflow

app = FastAPI(
    title="MangaRec AI Service",
    description="FastAPI service with a LangGraph agent that recommends manga based on semantic vibe/mood.",
    version="1.0.0"
)

class ChatRequest(BaseModel):
    query: str
    session_id: Optional[str] = "default_session"

class ChatResponse(BaseModel):
    response: Any
    is_json: bool = False

@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest):
    try:
        # Initialize the LangGraph state
        initial_state = {
            "query": request.query,
            "messages": [HumanMessage(content=request.query)],
            "language": "vi"
        }
        
        # Invoke LangGraph workflow
        result = await workflow.ainvoke(initial_state)
        
        # Determine output format based on workflow logic
        if "recommendations" in result and result["recommendations"]:
            return ChatResponse(
                response=result["recommendations"],
                is_json=True
            )
        
        # Fallback to general chat response parsing
        if "messages" in result and len(result["messages"]) > 0:
            last_message = result["messages"][-1]
            return ChatResponse(
                response=last_message.content,
                is_json=False
            )
            
        return ChatResponse(
            response="Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.",
            is_json=False
        )
        
    except RateLimitError as e:
        raise HTTPException(
            status_code=429, 
            detail=f"Groq API rate limit exceeded: {str(e)}"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500, 
            detail=f"Internal server error: {str(e)}"
        )
