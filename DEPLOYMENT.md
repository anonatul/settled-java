# Deploying Settled to Render

This project is configured to deploy to Render via a [Blueprint](./render.yaml) or manually.

## What you need

| Resource | Provider | Cost | Notes |
|---|---|---|---|
| App + container | Render Web Services (×2) | Free | Free tier spins down after 15 min idle; cold start ~30–60 s |
| Database | Supabase PostgreSQL | Free | Cloud-hosted Postgres; use the **Pooler** connection string (transaction mode) |
| Cache + rate limiting | Upstash Redis | Free (256 MB) | Requires TLS (the app enables SSL via `REDIS_SSL`) |
| Uploaded documents | Container disk | Free | Ephemeral — lost on redeploy on the free plan. Use a paid plan + persistent disk mounted at `/app/uploads` to keep them |

## Option A — Blueprint (recommended)

1. Push this repository to GitHub (done: `github.com/anonatul/settled-java`).
2. Create a free database at <https://console.upstash.com> and copy its host, port (6379) and password (the token).
3. Create a free Supabase project at <https://supabase.com>, then open **Project Settings → Database →
   Connection string → Pooler (Transaction mode)** and copy the URI (it looks like
   `postgresql://postgres.<ref>:<password>@aws-0-<region>.pooler.supabase.com:5432/postgres`).
4. In the Render dashboard: **New → Blueprint → select the repo → Apply**.
5. After it creates the services, open **settled-backend → Environment** and set:
   - `DATABASE_URL` = your full Supabase Pooler connection string (keeps the password out of the repo)
   - the four Upstash values (`REDIS_HOST`, `REDIS_PASSWORD`, keep `REDIS_PORT=6379` and `REDIS_SSL=true`).
6. **Deploy** the backend (and frontend). Open the frontend URL, e.g. `https://settled-frontend.onrender.com`.

## Option B — Manual

1. **PostgreSQL (Supabase)**: create a free project, then copy the **Pooler (Transaction mode)**
   connection string from **Project Settings → Database**. It looks like
   `postgresql://postgres.<ref>:<password>@aws-0-<region>.pooler.supabase.com:5432/postgres`.
   The app converts it to a JDBC URL and extracts the user/password automatically, so set it as `DATABASE_URL`
   on the backend — or convert it yourself and set `DB_URL=jdbc:postgresql://<host>:5432/postgres`
   together with `DB_USERNAME` / `DB_PASSWORD`. SSL is handled automatically by the pooler.
2. **Backend Web Service**:
   - Repository: `anonatul/settled-java`, **Root directory:** `backend`, Runtime: Docker, Plan: free.
   - Health check path: `/actuator/health`.
   - Environment:
     ```
     DATABASE_URL  = <supabase pooler connection string>
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
- **Keep your DATABASE_URL secret**: the repo is public, so never commit a connection string that contains
  the real password (the blueprint uses `sync: false` placeholders for this reason).
- **Uploads are ephemeral**: documents are stored on the container's writable layer and vanish on redeploy.
  For persistence, add a Render persistent disk mounted at `/app/uploads` (paid).
- **Single instance**: Redis-backed rate limits and caches work per-deployment; with multiple instances they
  still share state correctly because Redis is external.