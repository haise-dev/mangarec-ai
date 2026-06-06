import logging
import uuid
from qdrant_client import QdrantClient
from qdrant_client.http.models import PointStruct

from app.core.qdrant import get_qdrant_client, COLLECTION_NAME
from app.services.embedding import embedding_service

logger = logging.getLogger(__name__)

class QdrantService:
    """Service to handle storing and retrieving vectorized data from Qdrant."""

    def __init__(self, client: QdrantClient | None = None):
        # Inject client dependency
        self.client = client or get_qdrant_client()
        self.embedding_service = embedding_service
        self.collection_name = COLLECTION_NAME

    def save_manga_data(self, manga_id: str, text_content: str, metadata: dict | None = None) -> bool:
        """
        Vectorize text content and save it to Qdrant.
        
        Args:
            manga_id: Unique identifier for the manga (e.g., slug or UUID).
            text_content: The text to be embedded (synopsis, title, genres concatenated).
            metadata: Additional JSON payload to store with the vector.
        """
        try:
            # 1. Pass data through the embedding model perfectly
            vector = self.embedding_service.embed_text(text_content)
            
            # 2. Prepare payload to save alongside the vector
            payload = metadata or {}
            payload["manga_id"] = manga_id
            payload["text"] = text_content
            
            # 3. Create a deterministic point ID based on manga_id
            point_id = str(uuid.uuid5(uuid.NAMESPACE_URL, manga_id))
            
            point = PointStruct(
                id=point_id,
                vector=vector,
                payload=payload
            )
            
            # 4. Upsert to Qdrant (insert or update)
            self.client.upsert(
                collection_name=self.collection_name,
                points=[point]
            )
            logger.info(f"Successfully vectorized and saved manga {manga_id} to Qdrant.")
            return True
            
        except Exception as e:
            logger.error(f"Failed to save manga {manga_id} to Qdrant: {e}")
            return False

# Dependency Injection helper for FastAPI endpoints
def get_qdrant_service() -> QdrantService:
    return QdrantService()
