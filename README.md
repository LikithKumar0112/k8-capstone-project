# Employee Management — Developer Hand-off (App + DB)

This is the **application and database as received from the developer** — the raw
source that runs on **localhost**. There is intentionally **no Docker, no
Kubernetes, no CI/CD, no Terraform** here: containerising and deploying this to
Kubernetes is *your* task, and is planned out in [`BLUEPRINT.md`](./BLUEPRINT.md)
against the SkillfyMe **K8 Project manual** (projects 3.1 – 3.4).

## What's in the box

```
project2/
├── app/
│   ├── backend/      # Spring Boot 3.3 REST API (Java 17, Maven) — the application tier
│   └── frontend/     # React 18 + Vite UI — the presentation tier
├── db/
│   └── init.sql      # MySQL schema + seed data — the database
├── README.md         # this file
└── BLUEPRINT.md      # the plan to dockerise + deploy on Kubernetes
```

The domain is a classic **Employee** CRUD: `{ id, name, department, salary }`,
with the rules `id` must be positive and unique. REST API:

| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/employees`      | List all |
| GET    | `/api/employees/{id}` | Find by id |
| POST   | `/api/employees`      | Add (validates positive + unique id) → `201` |
| DELETE | `/api/employees/{id}` | Remove → `204` |
| GET    | `/actuator/health`    | Health (later used for K8s probes) |

## Run it on localhost

You need **JDK 17**, **Maven**, **Node 18+**, and a **MySQL 8** running on
`localhost:3306`.

### 1. Database
Start MySQL locally, then load the schema + seed data:
```bash
mysql -u root -p < db/init.sql
```
This creates the `employeedb` database, the `employee` user, the `employees`
table and a few sample rows.

### 2. Backend (port 8082)
```bash
cd app/backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
# health check:
curl http://localhost:8082/actuator/health
curl http://localhost:8082/api/employees
```
> The `local` profile runs the app against **MySQL only** (an in-memory cache
> stands in for Redis). The *default* profile expects Redis on `localhost:6379`
> — that Redis cache tier is part of the production topology you'll rebuild in
> Kubernetes (see the blueprint). Port is `8082` because `8080` is conventionally
> left for Jenkins.

### 3. Frontend (port 5173)
```bash
cd app/frontend
npm install
npm run dev
```
Open http://localhost:5173 . The Vite dev server proxies `/api` → the backend on
`:8082` (see `vite.config.js`), so the browser only ever talks to one origin.

## Configuration (env vars the app already honours)

These are the knobs you'll later map to Kubernetes **ConfigMaps** and **Secrets**:

| Variable | Default | Becomes in K8s |
|----------|---------|----------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/employeedb` | ConfigMap |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `employee` / `employeepass` | **Secret** |
| `SPRING_DATA_REDIS_HOST` / `_PORT` | `localhost` / `6379` | ConfigMap (Redis Service DNS) |
| `SERVER_PORT` | `8082` | container port |
| `APP_CORS_ALLOWED_ORIGINS` | `*` | ConfigMap |

**Next step:** work through [`BLUEPRINT.md`](./BLUEPRINT.md).
