from typing import TypedDict, Annotated
import operator
from langgraph.graph import StateGraph, START, END

class AgentState(TypedDict):
    input: str
    intermediate_steps: Annotated[list[str], operator.add]
    output: str

def router_node(state: AgentState):
    return {"intermediate_steps": ["Router decided to retrieve data."]}

def retriever_node(state: AgentState):
    return {"intermediate_steps": ["Retriever fetched mock manga data."]}

def reasoner_node(state: AgentState):
    # Dummy token measurement info or text
    return {"output": f"Mock answer for '{state['input']}'. [Tokens used: 42]"}

# Build Graph
builder = StateGraph(AgentState)
builder.add_node("router", router_node)
builder.add_node("retriever", retriever_node)
builder.add_node("reasoner", reasoner_node)

builder.add_edge(START, "router")
builder.add_edge("router", "retriever")
builder.add_edge("retriever", "reasoner")
builder.add_edge("reasoner", END)

dummy_graph = builder.compile()
