from typing import TypedDict
from langgraph.graph import StateGraph, START, END
from langchain_core.runnables.config import RunnableConfig
from langchain_groq import ChatGroq
from langchain_core.prompts import PromptTemplate
from app.services.search import SearchService
from app.core.config import settings

class AgentState(TypedDict):
    query: str
    mangas: list[dict]
    answer: str

def retriever_node(state: AgentState, config: RunnableConfig):
    # Extract db and qdrant from config
    db = config["configurable"].get("db")
    qdrant = config["configurable"].get("qdrant")
    
    if not db or not qdrant:
        raise ValueError("db and qdrant must be provided in config['configurable']")
    
    search_service = SearchService(db=db, qdrant=qdrant)
    
    # Perform hybrid search
    results = search_service.hybrid_search(query=state["query"], filters={}, limit=5)
    
    mangas = []
    for r in results:
        mangas.append({
            "id": r.id,
            "title": r.title,
            "summary": r.summary,
            "score": r.score
        })
        
    return {"mangas": mangas}

def reasoner_node(state: AgentState, config: RunnableConfig):
    llm = ChatGroq(model="llama-3.3-70b-versatile", api_key=settings.GROQ_API_KEY)
    
    prompt = PromptTemplate.from_template(
        "Bạn là một trợ lý ảo am hiểu về truyện tranh (Manga). User hỏi: {query}\n"
        "Dựa vào danh sách truyện gợi ý sau đây (được cung cấp dưới dạng JSON):\n{mangas}\n"
        "Hãy tư vấn và giải thích ngắn gọn, tự nhiên bằng tiếng Việt tại sao những truyện này lại phù hợp với user."
    )
    
    chain = prompt | llm
    
    mangas_str = str(state["mangas"])
    response = chain.invoke({"query": state["query"], "mangas": mangas_str})
    
    return {"answer": response.content}

# Build Graph
builder = StateGraph(AgentState)
builder.add_node("retriever", retriever_node)
builder.add_node("reasoner", reasoner_node)

builder.add_edge(START, "retriever")
builder.add_edge("retriever", "reasoner")
builder.add_edge("reasoner", END)

bot_graph = builder.compile()
