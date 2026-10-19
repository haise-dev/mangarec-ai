"""Pre-download the SentenceTransformer model for baking into the Docker image.

This script is executed during the Docker build stage so that the model weights
are cached inside the image and no network access is required at runtime.
"""

from sentence_transformers import SentenceTransformer, CrossEncoder

EMBEDDING_MODEL = "sentence-transformers/all-MiniLM-L6-v2"
RERANKER_MODEL = "cross-encoder/ms-marco-MiniLM-L-6-v2"

def download_model() -> None:
    """Download and cache the embedding and reranker models."""
    print(f"Downloading embedding model: {EMBEDDING_MODEL}")
    SentenceTransformer(EMBEDDING_MODEL)
    print(f"Embedding model '{EMBEDDING_MODEL}' downloaded successfully.")

    print(f"Downloading reranker model: {RERANKER_MODEL}")
    CrossEncoder(RERANKER_MODEL)
    print(f"Reranker model '{RERANKER_MODEL}' downloaded successfully.")


if __name__ == "__main__":
    download_model()
