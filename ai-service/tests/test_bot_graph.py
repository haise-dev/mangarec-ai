import pytest
from unittest.mock import MagicMock, patch
from app.chatbot.graph.bot_graph import retriever_node, reasoner_node

def test_retriever_node_missing_config():
    with pytest.raises(ValueError, match="db and qdrant must be provided"):
        retriever_node({"query": "test"}, {"configurable": {}})

@patch("app.chatbot.graph.bot_graph.SearchService")
def test_retriever_node_success(mock_search_service):
    mock_service_instance = MagicMock()
    
    mock_manga = MagicMock()
    mock_manga.id = "1"
    mock_manga.title_main = "Naruto"
    mock_manga.description_vi = "Ninja story"
    
    mock_service_instance.hybrid_search.return_value = [
        {"manga": mock_manga, "final_score": 0.9}
    ]
    mock_search_service.return_value = mock_service_instance

    state = {"query": "ninja"}
    config = {"configurable": {"db": MagicMock(), "qdrant": MagicMock()}}
    
    result = retriever_node(state, config)
    
    assert len(result["mangas"]) == 1
    assert result["mangas"][0]["title"] == "Naruto"
    assert result["mangas"][0]["score"] == 0.9
    assert result["mangas"][0]["summary"] == "Ninja story"

from langchain_groq import ChatGroq

@patch.object(ChatGroq, "invoke")
def test_reasoner_node(mock_invoke):
    mock_response = MagicMock()
    mock_response.content = "This is a great manga."
    mock_invoke.return_value = mock_response
    
    state = {"query": "ninja", "mangas": [{"title": "Naruto"}]}
    config = {}
    
    result = reasoner_node(state, config)
    
    assert result["answer"] == "This is a great manga."
