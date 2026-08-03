# Blueprint — Dockerise & Deploy on Kubernetes

> **Goal:** take the developer hand-off in this folder (React frontend, Spring
> Boot backend, MySQL database, Redis cache) from *runs-on-localhost* to a
> *multi-tier app running on Kubernetes*, demonstrating every task in the
> SkillfyMe **K8 Project manual** — projects **3.1, 3.2, 3.3, 3.4**.
>
> This is the map. Build it in the order below; each phase produces the exact
> deliverables (YAML + screenshots) the manual asks for.

---

## 0. Target architecture

```
                        ┌──────────────── Kubernetes namespace: employee-app ────────────────┐
   Internet             │                                                                     │
     │                  │   ┌───────────┐      ┌────────────┐        ┌──────────────────┐     │
     ▼                  │   │ frontend  │      │  backend   │        │ mysql (Stateful) │     │
 [Ingress] ───────────────►│ Deployment│─/api►│ Deployment │──JDBC─►│  + PVC/PV        │     │
   (or LoadBalancer)    │   │ (React/   │      │ (Spring)   │        │  Service (headless)   │
                        │   │  nginx)   │      │  +HPA      │───────┐ └──────────────────┘     │
                        │   └─────┬─────┘      └─────┬──────┘       │ ┌──────────────────┐     │
                        │         │Service            │Service       └►│ redis            │     │
                        │         ▼                   ▼                │ Deployment+Svc   │     │
                        │      ClusterIP           ClusterIP           └──────────────────┘     │
                        │                                                                     │
                        │   ConfigMap (non-secret config) · Secret (DB creds)                 │
                        └─────────────────────────────────────────────────────────────────────┘
                                    ▲                         ▲
                                    │ kubectl apply / ArgoCD sync
                              Git repo (manifests)  ◄── ArgoCD watches (Project 3.4)
```

| Tier | Image you build | K8s workload | Manual coverage |
|------|-----------------|--------------|-----------------|
| Frontend | React → static → nginx | Deployment + Service (+ Ingress) | 3.1, 3.3 |
| Backend  | Spring Boot fat-jar | Deployment + Service + **HPA** | 3.1, 3.2, 3.3 |
| Cache    | `redis:7` (stock) | Deployment + Service | 3.3 |
| Database | `mysql:8.0` (stock) | **StatefulSet** + PV/PVC + headless Service | 3.3 |
| Config   | — | ConfigMap + Secret | 3.3 |
| Delivery | — | ArgoCD Application | 3.4 |

---

## Phase 1 — Dockerise (prerequisite for everything)

Kubernetes runs container images, so this comes before any manifest. Build,
run with `docker compose` locally to prove the images, then push to a registry
(Docker Hub / GHCR / a local `kind`/`minikube` registry).

### 1a. Backend `Dockerfile` (`app/backend/Dockerfile`)
Multi-stage: build the jar with Maven, run it on a slim JRE.
```dockerfile
# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

### 1b. Frontend `Dockerfile` (`app/frontend/Dockerfile`)
Build the static bundle with Node, serve with nginx, and have nginx proxy
`/api` → the backend Service (the code calls a relative `/api`).
```dockerfile
# ---- build ----
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- serve ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```
Add `app/frontend/nginx/default.conf` that serves the SPA and proxies
`location /api/ { proxy_pass http://<backend-service>:8082; }`.

### 1c. `docker-compose.yml` (local smoke test)
Recreate the 4-tier stack (mysql, redis, backend, frontend) to verify the
images talk to each other before you touch Kubernetes. Mount `db/init.sql` into
the mysql container's `/docker-entrypoint-initdb.d/`.

**Exit criteria:** `docker compose up --build` → UI works end-to-end → then
`docker build`, tag, and `docker push` both app images.

---

## Phase 2 — Project 3.1 · ReplicaSets & Deployments

**Manual tasks:** container image → ReplicaSet → verify self-healing →
Deployment → scale → rolling update → rollback.

Do this with the **backend** (and frontend) images:

1. `namespace.yaml` — `employee-app` namespace (keeps everything tidy).
2. `backend-replicaset.yaml` — a bare **ReplicaSet** (replicas: 3). Delete a pod
   (`kubectl delete pod <name>`) and watch it be recreated → **Task 3**.
3. `backend-deployment.yaml` + `frontend-deployment.yaml` — replace the RS with
   **Deployments** (which own a ReplicaSet) → **Task 4**.
4. Scale: `kubectl scale deployment/backend --replicas=5` → **Task 5**.
5. Rolling update: bump the image tag, `kubectl set image ...`, watch
   `kubectl rollout status` → **Task 6**.
6. Rollback: `kubectl rollout undo deployment/backend` → **Task 7**.

**Deliverables:** ReplicaSet YAML, Deployment YAML, screenshots of multiple pods,
scaled pods, and a rolling update in progress.
**Elevate:** expose via a Service + add resource limits (leads into 3.2/3.3).

---

## Phase 3 — Project 3.2 · Resource Management & HPA

**Manual tasks:** deploy → set requests/limits → verify allocation → create HPA
→ generate load → observe scaling.

Target the **backend** (it's the CPU-bound, stateless tier — the right thing to
autoscale).

1. Add to `backend-deployment.yaml`:
   ```yaml
   resources:
     requests: { cpu: "250m", memory: "512Mi" }
     limits:   { cpu: "500m", memory: "1Gi" }
   ```
2. Install **metrics-server** (needed for HPA + `kubectl top`).
3. `backend-hpa.yaml` — HPA on the backend, `minReplicas: 2`, `maxReplicas: 10`,
   target `averageUtilization: 60` on CPU.
4. Generate load (e.g. `hey`/`ab`/a `while true; curl` loop, or a `busybox` load
   pod hitting `/api/employees`).
5. Watch `kubectl get hpa -w` and `kubectl top pods` scale up, then back down.

**Deliverables:** Deployment YAML with limits, HPA YAML, screenshots of running
pods, HPA config, and automatic scaling.
**Elevate:** add memory-based scaling alongside CPU.

---

## Phase 4 — Project 3.3 · PV, ConfigMap, Secrets & Services (the big one)

**Manual tasks:** PV → PVC → ConfigMap → Secret → Deployment (mount storage +
load config + read secrets) → Service → verify.

This is where the **MySQL database** and full wiring come together.

1. **Persistent storage (MySQL):**
   - `mysql-pv.yaml` (or rely on a dynamic StorageClass) + `mysql-pvc.yaml`.
   - `mysql-statefulset.yaml` using `volumeClaimTemplates` → the DB survives pod
     restarts (this is the "persistent storage for application data" requirement).
   - `mysql-service.yaml` — a **headless** Service for stable network identity.
   - Seed the schema: mount `db/init.sql` via a ConfigMap into
     `/docker-entrypoint-initdb.d/`.
2. **Redis:** `redis-deployment.yaml` + `redis-service.yaml` (stateless cache).
3. **ConfigMap** (`app-config.yaml`) — non-secret config: `SPRING_DATASOURCE_URL`
   (points at the mysql Service DNS), `SPRING_DATA_REDIS_HOST` (redis Service),
   `APP_CORS_ALLOWED_ORIGINS`, and the frontend's `BACKEND_URL`.
4. **Secret** (`db-secret.yaml`) — `SPRING_DATASOURCE_USERNAME` /
   `SPRING_DATASOURCE_PASSWORD` and MySQL root/user passwords. **Dummy values in
   git only** (note Sealed Secrets/SOPS as the production answer).
5. **Wire the backend Deployment** to consume both via `envFrom`
   (configMapRef + secretRef) — no code change needed, the app already reads
   these env vars.
6. **Services** for backend (ClusterIP) and frontend (ClusterIP/NodePort).
7. **Ingress** (`ingress.yaml`) — external entry → frontend, host-based routing.
8. Verify: data persists across a `kubectl delete pod mysql-0`; config comes from
   the ConfigMap; creds come from the Secret; app reachable via the Service/Ingress.

**Deliverables:** PV/PVC YAML, ConfigMap YAML, Secret YAML, Deployment YAML,
Service YAML, screenshots of running pods and the app being reached.
**Elevate:** mount the ConfigMap as a volume, inject Secret as env, add resource
limits, run multiple replicas.

---

## Phase 5 — Project 3.4 · GitOps with ArgoCD

**Manual tasks:** create cluster → install ArgoCD → prepare Git repo of manifests
→ configure ArgoCD Application → deploy via a git commit → verify → test sync.

1. Put **all** the Phase 2–4 manifests in a Git repo (a `k8s/` folder, or
   `base/` + `overlays/` with Kustomize so image tags patch cleanly).
2. Install ArgoCD into the cluster; log into its dashboard.
3. `argocd/application.yaml` — an ArgoCD **Application** pointing at your repo path
   and the `employee-app` namespace, with **auto-sync + self-heal** on.
4. Make a change (bump an image tag / replica count), `git commit && push`, and
   watch ArgoCD sync it to the cluster automatically.
5. Roll back by reverting the commit (git history = deploy history).

**Deliverables:** screenshots of cluster status, the ArgoCD dashboard, the
Application config, a successful synced deploy, and the git repo of manifests.
**Elevate:** auto-sync + self-heal, rollback via git history, multi-app ArgoCD.

---

## Suggested manifest repo layout (for phases 2–5)

```
k8s/
├── namespace/        namespace.yaml
├── mysql/            statefulset.yaml · service.yaml · pv.yaml · pvc.yaml · initdb-configmap.yaml
├── redis/            deployment.yaml · service.yaml
├── backend/          deployment.yaml · service.yaml · hpa.yaml · configmap.yaml
├── frontend/         deployment.yaml · service.yaml
├── config/           app-config (ConfigMap) · db-secret (Secret, DUMMY values)
├── ingress/          ingress.yaml
└── argocd/           application.yaml
```

## Build order checklist

- [ ] **Phase 1** — Dockerfiles (backend, frontend) + docker-compose smoke test + push images
- [ ] **Phase 2 (3.1)** — namespace, ReplicaSet demo, Deployments, scale, rolling update, rollback
- [ ] **Phase 3 (3.2)** — requests/limits, metrics-server, HPA, load test
- [ ] **Phase 4 (3.3)** — MySQL StatefulSet+PV/PVC, Redis, ConfigMap, Secret, Services, Ingress
- [ ] **Phase 5 (3.4)** — Git repo of manifests, ArgoCD install + Application + auto-sync
- [ ] Capture every screenshot/YAML deliverable listed per phase

## Notes / decisions to make

- **Cluster:** any works — `minikube`/`kind` (free, local) or a managed cluster
  (EKS/GKE). The manual accepts "any Kubernetes environment".
- **Registry:** Docker Hub / GHCR is simplest; a local registry if using kind.
- **Ingress:** needs an ingress controller (e.g. `ingress-nginx`) installed first;
  on minikube use `minikube addons enable ingress`.
- **Secrets:** dummy values in git for the capstone — never commit real creds.
- **Cache in prod topology:** the K8s deployment uses the default profile (Redis),
  unlike the `local` profile which skips it — that's deliberate, Redis is a real
  demonstrable tier here.
```
