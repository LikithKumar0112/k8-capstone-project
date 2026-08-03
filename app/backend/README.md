# Employee Management — Backend (Spring Boot)

The data/API tier of the Kubernetes capstone. The `Employee` domain and business
rules (positive + unique id, add/remove/find/list, validation, logging) are
carried over from the Jenkins-capstone `EmployeeService`, now persisted in
**MySQL** and read-cached in **Redis**, exposed over REST.

## Stack
Java 17 · Spring Boot 3.3 · Spring Web · Spring Data JPA · Spring Data Redis ·
Bean Validation · Actuator · Maven

## API
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/api/employees`      | List all (Redis-cached) |
| GET    | `/api/employees/{id}` | Find by id (Redis-cached) |
| POST   | `/api/employees`      | Add — validates positive + unique id → `201` |
| DELETE | `/api/employees/{id}` | Remove → `204` |
| GET    | `/actuator/health`    | Liveness/readiness probe (used by K8s) |

Errors return clean JSON: `400` (bad input), `404` (not found), `409` (duplicate id).

## Run locally
```bash
# Full stack (backend + MySQL + Redis) from the app repo root:
docker compose up --build
curl localhost:8082/actuator/health   # 8080 is reserved for Jenkins on the host

# Just the tests (no infra needed — pure Mockito):
cd backend && mvn test
```

## Configuration (env vars)
| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8082` | HTTP port (8080 reserved for Jenkins) |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/employeedb` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `employee` / `employeepass` | DB creds (K8s Secret) |
| `SPRING_DATA_REDIS_HOST` / `_PORT` | `localhost` / `6379` | Redis (K8s Service) |
| `APP_CORS_ALLOWED_ORIGINS` | `*` | Frontend origin (K8s ConfigMap) |

In Kubernetes these come from a **ConfigMap** (non-secret) and a **Secret** (DB creds).
