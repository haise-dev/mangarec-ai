import logging
from typing import Optional
from langchain_core.tracers.langchain import LangChainTracer
from app.core.config import settings

logger = logging.getLogger(__name__)

def get_tracer() -> Optional[LangChainTracer]:
    """
    Returns a LangChainTracer if LangSmith is properly configured.
    Provides graceful degradation: returns None if API Key is missing,
    ensuring that the core logic doesn't crash.
    """
    if str(settings.LANGCHAIN_TRACING_V2).lower() == "true":
        if not settings.LANGCHAIN_API_KEY:
            logger.warning("LANGCHAIN_TRACING_V2 is true but LANGCHAIN_API_KEY is missing. Tracing disabled.")
            return None
        try:
            tracer = LangChainTracer(project_name=settings.LANGCHAIN_PROJECT)
            return tracer
        except Exception as e:
            logger.error(f"Failed to initialize LangChainTracer: {e}")
            return None
    return None

def with_tracing(func):
    """
    A simple decorator to trace a function using LangSmith's traceable
    only if we have a valid configuration.
    """
    try:
        from langsmith import traceable
        if str(settings.LANGCHAIN_TRACING_V2).lower() == "true" and settings.LANGCHAIN_API_KEY:
            return traceable(name=func.__name__)(func)
    except ImportError:
        pass
    return func
