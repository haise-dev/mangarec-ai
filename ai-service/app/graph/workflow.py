import json
from typing import Literal
from langgraph.graph import StateGraph, END
from langchain_groq import ChatGroq
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app.graph.state import AgentState
from app.core.config import settings

# Initialize ChatGroq (using Llama-3.1-70B model via Groq)
llm = ChatGroq(
    model="llama-3.1-70b-versatile",
    api_key=settings.GROQ_API_KEY,
    temperature=0.7,
    max_retries=2,
)


async def router_node(state: AgentState) -> dict:
    """Analyze if the query is a manga recommendation request or general chat."""

    # We default the state language to Vietnamese as required
    return {"language": "vi"}


async def general_chat_node(state: AgentState) -> dict:
    """Handle general chat when not explicitly asking for recommendations."""
    system_msg = SystemMessage(
        content="Bạn là một trợ lý AI am hiểu về Manga có tên là MangaRec Agent. "
        "Hãy trả lời thân thiện và tự nhiên bằng tiếng Việt."
    )
    # The previous messages are already in state["messages"]
    # We just need to invoke the LLM with system message + context
    messages = [system_msg] + state.get("messages", [])

    response = await llm.ainvoke(messages)
    return {"messages": [response]}


async def retriever_node(state: AgentState) -> dict:
    """Perform vector search on Qdrant (placeholder for bge-m3 embedding logic)."""
    # TODO: Implement actual bge-m3 embedding and Qdrant similarity search here

    mock_docs = [
        {
            "title": "Chainsaw Man",
            "synopsis": "Denji là một thanh niên nghèo khổ sẽ làm bất cứ điều gì vì tiền...",
            "thumbnail": "https://example.com/csm.jpg",
            "mangadex_url": "https://mangadex.org/title/chainsaw-man",
        },
        {
            "title": "Jujutsu Kaisen",
            "synopsis": "Một cậu bé vô tình nuốt phải một loại bùa chú bị nguyền rủa...",
            "thumbnail": "https://example.com/jjk.jpg",
            "mangadex_url": "https://mangadex.org/title/jjk",
        },
    ]
    return {"retrieved_docs": mock_docs}


async def reasoner_node(state: AgentState) -> dict:
    """Use ChatGroq to explain why the retrieved mangas match the user's mood in Vietnamese."""
    query = state.get("query", "")
    docs = state.get("retrieved_docs", [])

    reasons = []
    for doc in docs:
        prompt = f"""
        Người dùng đang tìm kiếm manga với cảm xúc/yêu cầu sau: "{query}"
        Manga được trả về từ cơ sở dữ liệu: {doc["title"]}
        Tóm tắt: {doc["synopsis"]}
        
        Nhiệm vụ: Hãy đóng vai một chuyên gia manga, giải thích ngắn gọn, tự nhiên (bằng tiếng Việt) khoảng 2-3 câu 
        tại sao bộ manga này lại hoàn toàn phù hợp với vibes hoặc tâm trạng trên của họ.
        """
        response = await llm.ainvoke([HumanMessage(content=prompt)])
        # Preserve original doc dict and add the reason
        processed_doc = dict(doc)
        processed_doc["reason"] = response.content.strip()
        reasons.append(processed_doc)

    return {"recommendations": reasons}


async def formatter_node(state: AgentState) -> dict:
    """Structure the output as JSON containing title, thumbnail, mangadex_url, and reason."""
    recommendations = state.get("recommendations", [])

    formatted_recs = []
    for rec in recommendations:
        formatted_recs.append(
            {
                "title": rec.get("title", ""),
                "thumbnail": rec.get("thumbnail", ""),
                "mangadex_url": rec.get("mangadex_url", ""),
                "reason": rec.get("reason", ""),
            }
        )

    json_output = json.dumps(formatted_recs, ensure_ascii=False)

    # Store the formatted JSON logic into messages for history
    msg = AIMessage(content=json_output)

    return {"recommendations": formatted_recs, "messages": [msg]}


async def route_after_router(
    state: AgentState,
) -> Literal["retriever_node", "general_chat_node"]:
    """Conditional edge logic to determine the next path."""
    intent_prompt = f"""
    Determine if the user is asking for manga recommendations/suggestions based on mood, vibe, genres, etc., 
    or just chatting normally.
    
    User query: "{state.get("query", "")}"
    
    Rules:
    - If they want manga recommendations or suggestions, reply with ONLY the word: RECOMMEND
    - If they are just talking, asking general questions, or saying hi, reply with ONLY the word: GENERAL
    """
    try:
        response = await llm.ainvoke([HumanMessage(content=intent_prompt)])
        intent = response.content.strip().upper()
        if "RECOMMEND" in intent:
            return "retriever_node"
        return "general_chat_node"
    except Exception:
        return "general_chat_node"


# ----------------- Build Graph -----------------

builder = StateGraph(AgentState)

builder.add_node("router_node", router_node)
builder.add_node("general_chat_node", general_chat_node)
builder.add_node("retriever_node", retriever_node)
builder.add_node("reasoner_node", reasoner_node)
builder.add_node("formatter_node", formatter_node)

builder.set_entry_point("router_node")

builder.add_conditional_edges(
    "router_node",
    route_after_router,
    {"retriever_node": "retriever_node", "general_chat_node": "general_chat_node"},
)

builder.add_edge("general_chat_node", END)
builder.add_edge("retriever_node", "reasoner_node")
builder.add_edge("reasoner_node", "formatter_node")
builder.add_edge("formatter_node", END)

workflow = builder.compile()
