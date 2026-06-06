import logging
from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams
from app.core.config import settings

logger = logging.getLogger(__name__)

# The embedding model we use (all-MiniLM-L6-v2) has a vector size of 384
VECTOR_SIZE = 384
COLLECTION_NAME = "manga_collection"

def get_qdrant_client() -> QdrantClient:
    """Initialize and return the Qdrant client."""
    if settings.QDRANT_URL:
        # Connect to Qdrant Cloud or remote URL
        return QdrantClient(url=settings.QDRANT_URL, api_key=settings.QDRANT_API_KEY)
    
    # Connect to local Qdrant
    return QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)

def init_qdrant() -> None:
    """Initialize Qdrant collection if it doesn't exist."""
    client = get_qdrant_client()
    
    try:
        collections = client.get_collections().collections
        collection_names = [collection.name for collection in collections]
        
        if COLLECTION_NAME not in collection_names:
            logger.info(f"Creating Qdrant collection: {COLLECTION_NAME}")
            client.create_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=VectorParams(size=VECTOR_SIZE, distance=Distance.COSINE),
            )
            logger.info("Collection created successfully.")
        else:
            logger.info(f"Collection '{COLLECTION_NAME}' already exists.")
    except Exception as e:
        logger.error(f"Failed to initialize Qdrant: {e}")
        # Not raising here to prevent the whole app from crashing if DB is just slow to boot
        # but the health check will reflect the status.
