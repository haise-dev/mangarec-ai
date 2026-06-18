# MangaRec AI — Deployment Guide

> **Loại tài liệu:** Operations & Deployment Runbook  
> **Đối tượng:** DevOps Engineer, Backend Developer cần deploy  
> **Cập nhật lần cuối:** 2026-06-15  
> **Mục tiêu:** Hướng dẫn đầy đủ để đưa ứng dụng lên môi trường bất kỳ

---

## 1. Prerequisites (Yêu cầu Tiên quyết)

| Yêu cầu | Phiên bản tối thiểu | Ghi chú |
|---|---|---|
| Docker Engine | ≥ 24.x | Hỗ trợ `docker compose` plugin |
| Docker Compose Plugin | ≥ 2.x | Dùng `docker compose` (không phải `docker-compose`) |
| Git | — | Clone repository |
| Domain + SSL | — | Production only |
| RAM | ≥ 8GB | AI model load vào memory |

---

## 2. Environment Strategy (Chiến lược Môi trường)

| Môi trường | Mục đích | Entry point |
|---|---|---|
| **Development** | Local dev, hot-reload tất cả services | `docker compose -f docker-compose.dev.yml up` |
| **Staging** | Testing tích hợp, QA | *(Chưa định nghĩa — cần implement)* |
| **Production** | Live users | `docker compose -f docker-compose.yml up` |

### Cấu trúc Docker Compose

Root `docker-compose.dev.yml` / `docker-compose.yml` dùng `include:` để kết hợp:

```
docker-compose.dev.yml (root)          docker-compose.yml (root)
  ├── ai-service/docker-compose.dev.yml   ├── ai-service/docker-compose.yml
  ├── be-service/docker-compose.dev.yml   ├── be-service/docker-compose.yml
  └── fe-service/docker-compose.dev.yml   └── fe-service/docker-compose.yml
```

Tất cả service dùng chung external network `mangarec_global_network`.

---

## 3. Development Setup (Local)

### Bước 1: Clone & Chuẩn bị

```bash
git clone <repo-url>
cd mangarec-ai
```

### Bước 2: Tạo Docker Network

```bash
docker network create mangarec_global_network
```

> Chỉ cần chạy **một lần**. Network này được chia sẻ giữa tất cả service.

### Bước 3: Cấu hình Environment Variables

Mỗi service có file `.env.example` riêng:

```bash
# AI Service
cp ai-service/.env.example ai-service/.env

# Core Backend
cp be-service/.env.example be-service/.env

# Frontend
cp fe-service/.env.example fe-service/.env
```

Điền các giá trị thực vào từng file `.env` (xem Bảng Variables bên dưới).

### Bước 4: Khởi động Stack

```bash
docker compose -f docker-compose.dev.yml up -d
```

Để build lại image (sau khi thay đổi dependencies):

```bash
docker compose -f docker-compose.dev.yml up -d --build
```

### Bước 5: Verify Services

```bash
# Kiểm tra trạng thái tất cả containers
docker compose -f docker-compose.dev.yml ps

# Xem logs realtime (Ctrl+C để thoát)
docker compose -f docker-compose.dev.yml logs -f

# Logs một service cụ thể
docker compose -f docker-compose.dev.yml logs -f ai-service
```

### Bước 6: Verify Endpoints

| Service | URL | Kỳ vọng |
|---|---|---|
| Frontend | `http://localhost:3000` | UI app |
| Backend API | `http://localhost:8080` | Spring Boot |
| Swagger UI | `http://localhost:8080/swagger-ui.html` | API docs |
| AI Service | `http://localhost:8000` | FastAPI |
| AI Health | `http://localhost:8000/health` | `{"status":"ok","qdrant":"connected","sqlite":"connected"}` |
| AI Docs | `http://localhost:8000/docs` | FastAPI Swagger |
| Qdrant Dashboard | `http://localhost:6333/dashboard` | Vector DB UI |

### Hot-reload (Development)
- **Frontend:** Vite hot-reload — chỉnh sửa `fe-service/src/` → tự động cập nhật browser
- **AI Service:** Uvicorn `--reload` — chỉnh sửa `ai-service/app/` → tự động restart
- **BE Service:** Spring DevTools (nếu được cấu hình trong Dockerfile.dev)

---

## 4. Environment Variables Reference

### AI Service (`ai-service/.env`)
```bash
# LLM — BẮT BUỘC
GROQ_API_KEY=your_groq_api_key_here

# Qdrant Vector DB — BẮT BUỘC (chú ý: QDRANT_HOST + QDRANT_PORT, không phải QDRANT_URL)
QDRANT_HOST=qdrant          # Trong Docker: tên container; ngoài Docker: localhost
QDRANT_PORT=6333

# SQLite Database — BẮT BUỘC
DATABASE_URL=sqlite:////app/data/manga_metadata.db

# LangSmith Observability — Optional
LANGCHAIN_TRACING_V2=true
LANGCHAIN_ENDPOINT=https://api.smith.langchain.com
LANGCHAIN_API_KEY=your_langsmith_api_key
LANGCHAIN_PROJECT=mangarec_ai_service
```

> ⚠️ **Lưu ý:** Trong Docker Compose, `QDRANT_HOST=qdrant` (tên service). Ngoài Docker, dùng `QDRANT_HOST=localhost`.

### Core Backend (`be-service/.env`)
```bash
# PostgreSQL
POSTGRES_DB=mangarec
POSTGRES_USER=mangarec
POSTGRES_PASSWORD=your_secure_password
POSTGRES_PORT=5432

# Redis
REDIS_HOST=redis              # Trong Docker: tên service; ngoài Docker: localhost
REDIS_PORT=6379

# Spring Datasource
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mangarec?currentSchema=mangarec
SPRING_DATASOURCE_USERNAME=mangarec
SPRING_DATASOURCE_PASSWORD=your_secure_password
SPRING_FLYWAY_ENABLED=true

# Google OAuth
GOOGLE_AUTH_CLIENT_ID=your_google_client_id.apps.googleusercontent.com

# Email OTP (Gmail SMTP)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-gmail@gmail.com
SPRING_MAIL_PASSWORD=your_gmail_app_password  # App Password, không phải Gmail password
EMAIL_VERIFICATION_OTP_EXPIRY_MINUTES=10
PASSWORD_RESET_OTP_EXPIRY_MINUTES=10

# payOS Payment Gateway
PAYOS_CLIENT_ID=your_payos_client_id
PAYOS_API_KEY=your_payos_api_key
PAYOS_CHECKSUM_KEY=your_payos_checksum_key
PAYOS_BASE_URL=https://api-merchant.payos.vn
PAYOS_RETURN_URL=https://yourdomain.com/payment/success
PAYOS_CANCEL_URL=https://yourdomain.com/payment/cancel
PAYOS_WEBHOOK_URL=https://yourdomain.com/api/v1/payments/payos/webhook
PAYOS_PAYMENT_EXPIRY_MINUTES=15
```

### Frontend (`fe-service/.env`)
```bash
VITE_API_BASE_URL=http://localhost:8080        # Thay bằng domain thực khi production
VITE_GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
```

> ⚠️ **Build-time vars:** `VITE_*` variables được bake vào bundle lúc build — không thể thay đổi runtime.  
> Production: truyền qua Docker build args `VITE_API_BASE_URL` và `VITE_GOOGLE_CLIENT_ID`.

---

## 5. Database Management

### 5.1 PostgreSQL Migrations (Flyway)

**Development:** Flyway chạy trong dedicated container `mangarec-flyway` — tự động migrate khi `postgres` healthy.

**Kiểm tra trạng thái migration:**
```bash
# Xem logs flyway container
docker logs mangarec-flyway

# Kết nối trực tiếp postgres để kiểm tra
docker exec -it mangarec-postgres psql -U mangarec -d mangarec -c "SELECT * FROM mangarec.flyway_schema_history;"
```

**Migration files:** `be-service/src/main/resources/db/migration/`
- `V1__init_mangarec_schema.sql`
- `V2__payos_payment_schema.sql`

**Thêm migration mới:**
```
# Tạo file mới theo convention
V3__add_new_feature.sql
# Flyway tự detect và chạy theo thứ tự version
```

### 5.2 SQLite Migrations (Alembic — AI Service)

Alembic chạy tự động khi `ai-service` start (trong `lifespan`):
```python
command.upgrade(alembic_cfg, "head")
```

**Thủ công (nếu cần):**
```bash
# Upgrade to latest
docker exec -it ai-service-dev uv run alembic upgrade head

# Xem lịch sử migration
docker exec -it ai-service-dev uv run alembic history

# Rollback 1 version
docker exec -it ai-service-dev uv run alembic downgrade -1

# Tạo migration mới
docker exec -it ai-service-dev uv run alembic revision --autogenerate -m "describe_change"
```

### 5.3 Qdrant — Khởi tạo Collection

Qdrant collection được khởi tạo tự động khi AI service start (`init_qdrant()` trong lifespan).

```bash
# Kiểm tra collections qua dashboard
open http://localhost:6333/dashboard

# Hoặc via API
curl http://localhost:6333/collections
```

---

## 6. Production Deployment

### 6.1 Chuẩn bị Server

**Recommended:**
- OS: Ubuntu 22.04 LTS
- Minimum: 4 vCPU, 8GB RAM, 50GB SSD (AI model chiếm ~2GB)
- Cài đặt: Docker Engine, Docker Compose Plugin, Nginx (reverse proxy ngoài Docker)

**Cài Docker trên Ubuntu:**
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# Log out và log in lại
```

### 6.2 Setup Secrets (Không copy .env lên server)

**Option A — Environment variables của hosting platform** (Coolify, Railway, Render)

**Option B — Docker Secrets (tự managed server):**
```bash
# Tạo secret file
echo "your_groq_key" | docker secret create groq_api_key -
```

**Option C — Truyền qua shell environment:**
```bash
export GROQ_API_KEY="your_key"
export POSTGRES_PASSWORD="your_password"
docker compose -f docker-compose.yml up -d
```

### 6.3 Build & Deploy

```bash
# Tạo network (chỉ lần đầu)
docker network create mangarec_global_network

# Build và start tất cả services
docker compose -f docker-compose.yml up -d --build

# Kiểm tra
docker compose -f docker-compose.yml ps
docker compose -f docker-compose.yml logs -f
```

### 6.4 Frontend Build Args (Production)

Frontend cần build args khi build Docker image:
```bash
# Build FE riêng với args
docker build \
  --build-arg VITE_API_BASE_URL=https://api.yourdomain.com \
  --build-arg VITE_GOOGLE_CLIENT_ID=your_client_id \
  -f fe-service/Dockerfile \
  -t mangarec-fe-service:latest \
  fe-service/
```

Hoặc qua `docker-compose.yml` FE service (đã cấu hình env vars trong file).

### 6.5 Nginx Reverse Proxy (Ngoài Docker)

```nginx
# /etc/nginx/sites-available/mangarec

# Frontend
server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

# Backend API
server {
    listen 443 ssl;
    server_name api.yourdomain.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

# AI Service — KHÔNG expose public
# Chỉ accessible qua internal Docker network
```

**AI Service KHÔNG được expose public** — chỉ accessible nội bộ giữa `be-service` và `ai-service` qua Docker network.

### 6.6 SSL/TLS

```bash
# Cài Certbot
sudo apt install certbot python3-certbot-nginx

# Issue certificate
sudo certbot --nginx -d yourdomain.com -d api.yourdomain.com

# Auto-renew (đã tự cấu hình, kiểm tra):
sudo certbot renew --dry-run
```

### 6.7 Health Checks

```bash
# Backend
curl https://api.yourdomain.com/api/v1/health

# AI Service (internal check qua docker)
docker exec ai-service-prod curl -s http://localhost:8000/health

# Frontend
curl -I https://yourdomain.com
```

---

## 7. CI/CD Pipeline (Khung Đề xuất)

> ⚠️ **Chưa implement** — đây là placeholder cho GitHub Actions (Technical Debt: HIGH)

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    # 1. Run tests — FE (eslint + format:check), BE (mvn test), AI (pytest)
    
  build:
    needs: test
    # 2. Build Docker images
    # 3. Push to Container Registry (ghcr.io hoặc Docker Hub)
    
  deploy:
    needs: build
    # 4. SSH to server
    # 5. docker compose pull
    # 6. docker compose up -d --no-build
    # 7. Health check verify
```

---

## 8. Monitoring & Logging

### Logging hiện tại
```bash
# Xem logs realtime
docker compose -f docker-compose.yml logs -f [service-name]

# Logs với timestamp
docker compose -f docker-compose.yml logs --timestamps ai-service

# Export logs
docker logs ai-service-prod --since 24h > ai-service-$(date +%Y%m%d).log
```

### AI Service Observability
- **LangSmith:** Nếu `LANGCHAIN_TRACING_V2=true` và `LANGCHAIN_API_KEY` được set → tất cả LangGraph/LangChain calls được trace tự động tại `smith.langchain.com`

### Cần Implement (Technical Debt)
- Centralized logging: ELK Stack hoặc Grafana Loki
- Uptime monitoring: Uptime Kuma hoặc Better Uptime
- Alerting: PagerDuty hoặc Slack webhook khi service down

---

## 9. Backup Strategy

### PostgreSQL
```bash
# Manual backup
docker exec mangarec-postgres pg_dump \
  -U mangarec -d mangarec \
  --schema=mangarec \
  > backup_$(date +%Y%m%d_%H%M%S).sql

# Cron job backup hàng ngày (thêm vào crontab)
0 2 * * * docker exec mangarec-postgres pg_dump -U mangarec -d mangarec > /backups/pg_$(date +\%Y\%m\%d).sql
```

### SQLite (AI Service)
```bash
# Backup SQLite file
docker exec ai-service-prod \
  cp /app/data/manga_metadata.db /tmp/manga_backup.db

docker cp ai-service-prod:/tmp/manga_backup.db ./backups/sqlite_$(date +%Y%m%d).db
```

### Qdrant
```bash
# Snapshot via Qdrant API
curl -X POST http://localhost:6333/collections/manga_embeddings/snapshots

# Download snapshot
curl -O http://localhost:6333/collections/manga_embeddings/snapshots/<snapshot-name>
```

**Recovery Test:** Kiểm tra khôi phục backup tối thiểu mỗi tháng.

---

## 10. Rollback Procedure

### Rollback Docker Image
```bash
# Pull image version cũ
docker pull ghcr.io/your-org/ai-service:v1.2.3

# Update docker-compose.yml để dùng version cũ
# Restart service
docker compose -f docker-compose.yml up -d ai-service
```

### Rollback Database

**PostgreSQL (Flyway):**
```bash
# Rollback migration (Flyway không hỗ trợ tự động undo — cần viết script thủ công)
# Restore từ backup:
docker exec -i mangarec-postgres psql -U mangarec -d mangarec < backup_YYYYMMDD.sql
```

**SQLite (Alembic):**
```bash
docker exec -it ai-service-prod uv run alembic downgrade -1
# Hoặc về version cụ thể:
docker exec -it ai-service-prod uv run alembic downgrade <revision_id>
```

---

## 11. Troubleshooting thường gặp

| Vấn đề | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `ai-service` không start | Qdrant chưa ready | Check `docker logs qdrant-dev`, chờ Qdrant khởi động xong |
| FE không kết nối được BE | `VITE_API_BASE_URL` sai | Kiểm tra `fe-service/.env` → `VITE_API_BASE_URL` |
| BE không kết nối PostgreSQL | DB chưa ready hoặc credentials sai | Check `docker logs mangarec-flyway`; verify `.env` credentials |
| AI model load chậm | Lần đầu chạy, model download | Production: model baked vào image. Dev: chờ download xong |
| Network error giữa services | `mangarec_global_network` chưa tạo | `docker network create mangarec_global_network` |
| payOS webhook không nhận | URL không accessible từ internet | Dùng ngrok để test local: `ngrok http 8080` |
