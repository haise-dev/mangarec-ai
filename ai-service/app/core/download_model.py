"""Pre-download the SentenceTransformer model for baking into the Docker image.

This script is executed during the Docker build stage so that the model weights
are cached inside the image and no network access is required at runtime.
"""

from sentence_transformers import SentenceTransformer

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"


def download_model() -> None:
    """Download and cache the embedding model."""
    print(f"Downloading model: {MODEL_NAME}")
    SentenceTransformer(MODEL_NAME)
    print(f"Model '{MODEL_NAME}' downloaded successfully.")


if __name__ == "__main__":
    download_model()
