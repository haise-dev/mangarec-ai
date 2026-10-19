import logging
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

# Same model baked into the Docker image
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"

class EmbeddingService:
    """Service to handle text vectorization using SentenceTransformers."""
    
    _instance = None
    _model = None

    def __new__(cls):
        # Singleton pattern to ensure we only load the model into RAM once
        if cls._instance is None:
            cls._instance = super(EmbeddingService, cls).__new__(cls)
        return cls._instance

    def __init__(self):
        if self._model is None:
            logger.info(f"Loading embedding model: {MODEL_NAME}")
            # Explicitly load on CPU to respect < 1GB RAM constraints
            self._model = SentenceTransformer(MODEL_NAME, device="cpu")
            logger.info("Embedding model loaded successfully.")

    def embed_text(self, text: str) -> list[float]:
        """Convert a single text string into a vector."""
        # encode() returns a numpy array, we convert it to a standard Python list
        vector = self._model.encode(text)
        return vector.tolist()

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """Convert a list of text strings into a list of vectors."""
        vectors = self._model.encode(texts)
        return vectors.tolist()

# Provide a global instance to avoid multiple initializations across requests
embedding_service = EmbeddingService()
