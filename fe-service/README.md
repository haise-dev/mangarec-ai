# MangaRec FE Service

React/Vite frontend for MangaRec authentication.

## Local dev

```bash
npm install
npm run dev
```

Default API base URL: `http://localhost:8080`.

Copy `.env.example` to `.env` if you need to override:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=264778620233-r8k76djk9hqtcu9b0of9bqrgfd7kjcng.apps.googleusercontent.com
```

## Docker

Create the shared network if you run this compose file standalone:

```bash
docker network create mangarec_global_network
```

Production static image:

```bash
docker compose up --build fe-service
```

Development container:

```bash
docker compose -f docker-compose.dev.yml up --build fe-service
```

Open `http://localhost:3000`.
