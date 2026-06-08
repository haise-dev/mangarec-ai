"""Machine Learning Model Management.

Handles singleton instances of heavy ML models (Embedders and Rerankers)
to ensure they are loaded only once into memory.
"""

import logging
from typing import Optional

from sentence_transformers import CrossEncoder, SentenceTransformer

logger = logging.getLogger(__name__)


class MLManager:
    """Singleton manager for ML Models."""

    _instance: Optional["MLManager"] = None

    def __init__(self):
        """Initialize empty model slots."""
        self.embedding_model: Optional[SentenceTransformer] = None
        self.reranker_model: Optional[CrossEncoder] = None

    @classmethod
    def get_instance(cls) -> "MLManager":
        """Get the singleton instance."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def load_models(self) -> None:
        """Load heavy models into memory. Call this during app startup."""
        if self.embedding_model is not None and self.reranker_model is not None:
            logger.info("ML Models already loaded.")
            return

        logger.info("Loading ML Models (Embedder & Reranker)... This may take a moment.")
        # Load embedding model
        self.embedding_model = SentenceTransformer("all-MiniLM-L6-v2")
        
        # Load Cross-Encoder reranker
        self.reranker_model = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")
        
        logger.info("ML Models loaded successfully.")
