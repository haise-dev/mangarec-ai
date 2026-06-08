# ============================================================================
# Stage 1 — Builder: install dependencies and pre-download the embedding model
# ============================================================================
FROM python:3.12-slim AS builder

# Install uv for fast, deterministic dependency management
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Copy dependency manifests first (layer caching)
COPY pyproject.toml uv.lock ./

# Install project dependencies — CPU-only torch resolved via uv.lock
RUN uv sync --frozen --no-install-project --no-dev

# Copy application source
COPY app/ app/

# Pre-download the HuggingFace model so it's baked into the image (AC-1, AC-3)
RUN uv run python app/core/download_model.py

# ============================================================================
# Stage 2 — Runtime: minimal image with only what's needed to run
# ============================================================================
FROM python:3.12-slim AS runtime

# --- CPU / memory optimisation environment variables ---
ENV OMP_NUM_THREADS=1 \
    MKL_NUM_THREADS=1 \
    TOKENIZERS_PARALLELISM=false \
    TRANSFORMERS_NO_ADVISORY_WARNINGS=1 \
    HF_HOME=/app/.cache/huggingface

WORKDIR /app

# Create data directory for SQLite database
RUN mkdir -p /app/data

# Bring over installed packages from builder
COPY --from=builder /app/.venv /app/.venv
# Bring over application code
COPY --from=builder /app/app ./app
# Bring over scripts
COPY scripts/ ./scripts/
# Bring over the cached HuggingFace model
COPY --from=builder /root/.cache/huggingface /app/.cache/huggingface

# Make the venv's Python the default
ENV PATH="/app/.venv/bin:$PATH"

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
