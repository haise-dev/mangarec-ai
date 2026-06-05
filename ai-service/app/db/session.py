from qdrant_client import AsyncQdrantClient
from supabase import create_client, Client
from app.core.config import settings

# Initialize Async Qdrant Client
qdrant_client = AsyncQdrantClient(
    url=settings.QDRANT_URL,
    api_key=settings.QDRANT_API_KEY,
)

# Initialize Supabase Client
supabase_client: Client = create_client(settings.SUPABASE_URL, settings.SUPABASE_KEY)
