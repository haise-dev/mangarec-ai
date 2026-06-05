from typing import TypedDict, List, Any, Annotated
from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages


class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]
    query: str
    retrieved_docs: List[Any]
    recommendations: List[dict]
    language: str
