# Local container stack

The repository uses one shared multi-stage `Dockerfile`. Each Java image selects
its Maven module with the `MODULE` build argument; each application and each
PostgreSQL database still runs in its own container.

## Compose files

| File | Purpose | Published application port |
| --- | --- | --- |
| `docker-compose.catalog.yml` | Standalone Catalog + PostgreSQL | `8081` |
| `docker-compose.member.yml` | Standalone Member + PostgreSQL | `8082` |
| `docker-compose.loan.yml` | Standalone Loan + PostgreSQL | `8083` |
| `docker-compose.fine.yml` | Standalone Fine + PostgreSQL | `8084` |
| `docker-compose.notification.yml` | Standalone Notification + PostgreSQL | `8085` |
| `docker-compose.yml` | Aggregate local microservice stack | Gateway `8080` only |

The standalone files disable Config Server and Eureka so a developer can debug
one service. The aggregate file enables service discovery and native Config
Server configuration from `infra/config-repo`.

## Environment

Copy `.env.example` to `.env` and replace all required placeholders. The
aggregate stack expects a hosted/public Keycloak URL because `localhost` inside
a container refers to that container, not to the Windows host.

Each database has a unique host port (`5437` through `5441`) for tools such as
DBeaver. Container-to-container traffic always uses the database service name
and PostgreSQL port `5432`.

## Aggregate stack

Validate without printing resolved secrets:

```powershell
docker compose --env-file .env -f docker-compose.yml config --quiet
```

Build and start:

```powershell
docker compose --env-file .env -f docker-compose.yml up -d --build
```

Inspect status and logs:

```powershell
docker compose --env-file .env -f docker-compose.yml ps
docker compose --env-file .env -f docker-compose.yml logs -f api-gateway
```

Stop while retaining all database volumes:

```powershell
docker compose --env-file .env -f docker-compose.yml down
```

Do not add `--volumes` unless the local databases should be deleted.

## CI workflows

The five `ci-*-service.yml` workflows trigger only when their service, the
shared module, root Maven build, or shared Docker build files change. They call
`_service-ci.yml`, which runs the module tests and builds its Docker image.
Images are intentionally not pushed and no deployment step is present.
