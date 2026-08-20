# Deploying Settled to Render

This project is configured to deploy to Render via a [Blueprint](./render.yaml) or manually.

## What you need

| Resource | Provider | Cost | Notes |
|---|---|---|---|
| App + container | Render Web Services (×2) | Free | Free tier spins down after 15 min idle; cold start ~30–60 s |
| Database | Render managed PostgreSQL | Free (1 GB) | **Expires after 30 days**; upgrade to $7/mo to keep it |
| Cache + rate limiting | Upstash Redis | Free (256 MB) | Requires TLS (the app enables SSL via `REDIS_SSL`) |
| Uploaded documents | Container disk | Free | Ephemeral — lost on redeploy on the free plan. Use a paid plan + persistent disk mounted at `/app/uploads` to keep them |

## Option A — Blueprint (recommended)

1. Push this repository to GitHub (done: `github.com/anonatul/settled-java`).
2. Create a free database at <https://console.upstash.com> and copy its host, port (6379) and password (the token).
3. In the Render dashboard: **New → Blueprint → select the repo → Apply**.
4. After it creates the services, open **settled-backend → Environment** and set the four Upstash values
   (`REDIS_HOST`, `REDIS_PASSWORD`, keep `REDIS_PORT=6379` and `REDIS_SSL=true`).
5. **Deploy** the backend (and frontend). Open the frontend URL, e.g. `https://settled-frontend.onrender.com`.

## Option B — Manual

1. **PostgreSQL**: Render Dashboard → New → PostgreSQL → free plan. Note the **Internal Database URL**
   (`postgres://user:pass@host:5432/settled`). The app converts it to a JDBC URL automatically, so set it
   as `DATABASE_URL` on the backend — or convert it yourself and set `DB_URL=jdbc:postgresql://...`.
2. **Backend Web Service**:
   - Repository: `anonatul/settled-java`, **Root directory:** `backend`, Runtime: Docker, Plan: free.
   - Health check path: `/actuator/health`.
   - Environment:
     ```
     DATABASE_URL  = <internal postgres connection string>
     JWT_SECRET    = <long random string>
     REDIS_HOST    = <upstash host>      e.g. decent-dove-12345.upstash.io
     REDIS_PORT    = 6379
     REDIS_PASSWORD= <upstash token>
     REDIS_SSL     = true
     UPLOAD_DIR    = /app/uploads
     ```
   - Deploy. Note the public URL, e.g. `https://settled-backend.onrender.com` — Swagger UI lives at `/swagger-ui.html`.
3. **Frontend Web Service**:
   - Repository: `anonatul/settled-java`, **Root directory:** `frontend`, Runtime: Docker, Plan: free.
   - Environment:
     ```
     PORT        = (Render sets this automatically)
     BACKEND_URL = https://settled-backend.onrender.com
     ```
   - Deploy. The nginx container listens on `$PORT` and proxies `/api` to the backend, so the browser
     only talks to the frontend (no CORS issues).

## Demo accounts

After the backend starts it seeds itself on an empty database. All passwords are `password123`:

- Admin: `admin@settled.io`
- Officers: `officer1@settled.io`, `officer2@settled.io`
- Customers: `customer1@settled.io`, `customer2@settled.io`, `customer3@settled.io`

## Notes / limitations on the free tier

- **Cold starts**: free Web Services sleep after ~15 minutes of inactivity; the first request may take 30–60 s.
- **Database expiry**: Render's free PostgreSQL is deleted after 30 days. Either recreate it (data loss) or upgrade.
- **Uploads are ephemeral**: documents are stored on the container's writable layer and vanish on redeploy.
  For persistence, add a Render persistent disk mounted at `/app/uploads` (paid).
- **Single instance**: Redis-backed rate limits and caches work per-deployment; with multiple instances they
  still share state correctly because Redis is external.