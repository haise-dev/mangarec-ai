# MangaRec AI - Docker Architecture & Orchestration

> **Author:** AI Engineering Team  
> **Date:** 2026-06-08

## 1. Overview
This monorepo utilizes the **Modular Compose (Compose Include)** pattern. Instead of maintaining one massive `docker-compose.yml`, the root orchestrator includes individual compose files from each service directory.

This guarantees:
- **Separation of Concerns:** Backend engineers manage Postgres & BE containers, AI engineers manage Qdrant & AI containers.
- **Unified Networking:** All services communicate via `mangarec_global_network`.
- **Environment Parity:** Strict separation between Development (Hot-reloading) and Production (Multi-stage builds).

## 2. Directory Structure

```text
/workspace/mangarec-ai/
├── docker-compose.yml        (Root Production Orchestrator)
├── docker-compose.dev.yml    (Root Development Orchestrator)
├── docs/
├── ai-service/
│   ├── docker-compose.yml    (AI Prod: FastAPI + Qdrant)
│   ├── docker-compose.dev.yml(AI Dev: Hot-reload mounts)
│   ├── Dockerfile
│   └── Dockerfile.dev
├── be-service/
│   ├── docker-compose.yml    (BE Prod: API + Postgres)
│   └── docker-compose.dev.yml(BE Dev: API + Exposed Postgres)
└── fe-service/
    ├── docker-compose.yml    (FE Prod: Nginx/Next.js)
    └── docker-compose.dev.yml(FE Dev: React Hot-reload)
```

## 3. The Network Strategy
A single bridge network named `mangarec_global_network` is instantiated by the Root Compose.
Inside the individual service composes, this network is referenced as `external: true`.

**How to communicate between services:**
Do NOT use `localhost`. Use the container names!
- AI Service calls Backend: `http://be-service-prod:8080/...`
- Backend calls AI Search: `http://ai-service-prod:8000/api/v1/search`

## 4. How to Run

### Development Mode
Runs everything with source code mounted. If you edit code, the server restarts automatically.
```bash
# At the root directory:
docker network create mangarec_global_network || true
docker compose -f docker-compose.dev.yml up -d
```

### Production Mode
Runs heavily optimized, compiled Docker images. No source code mapping.
```bash
# At the root directory:
docker network create mangarec_global_network || true
docker compose -f docker-compose.yml up -d --build
```
