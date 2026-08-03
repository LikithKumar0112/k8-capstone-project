# Employee Management — Frontend (React)

The presentation tier of the Kubernetes capstone: a small React UI to list, add,
and delete employees, served by **nginx**, which also **reverse-proxies `/api`**
to the Spring Boot backend.

## Stack
React 18 · Vite · nginx (multi-stage Docker build)

## Why the nginx proxy?
The app calls a **relative `/api`** path, and nginx forwards it to the backend.
This means:
- **No CORS** — the browser only ever talks to one origin (the frontend).
- **Backend stays internal** — only the frontend is exposed (via Ingress in K8s).
- **One config knob** — the backend target is the `BACKEND_URL` env var, which
  becomes a **ConfigMap** value in Kubernetes.

## Run
```bash
# As part of the full stack (from the app repo root):
docker compose up --build
# open http://localhost:8083   (8080 = Jenkins, 8082 = backend API)

# Or just the UI in dev mode (needs the backend running on :8082):
cd frontend && npm install && npm run dev
```

## Configuration
| Variable | Default | Purpose |
|----------|---------|---------|
| `BACKEND_URL` | `http://backend:8082` | Upstream the nginx `/api/` location proxies to |

In Kubernetes, `BACKEND_URL` points at the backend Service DNS name
(e.g. `http://employee-backend:8082`) and is injected from a ConfigMap.
