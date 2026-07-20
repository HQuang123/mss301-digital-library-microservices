# Member Service container example

`docker-compose.member.yml` is a standalone example containing two containers:

- `member-service`: the Spring Boot application built from the shared root `Dockerfile`.
- `member-db`: PostgreSQL 16 with a persistent Docker volume.

They are separate containers on the same `member-network`. The application
connects to `member-db:5432`; `localhost` inside `member-service` would refer to
the application container itself.

The Compose project is named `digilib-member`, so it can run alongside the
existing Keycloak infrastructure without reporting those containers as orphans.

## Start

Copy `.env.example` to `.env` and replace every required placeholder. For the
hosted Keycloak deployment, `KEYCLOAK_BASE_URL` and `KEYCLOAK_ISSUER_URI` must
use its public HTTPS URLs.

Validate the resolved configuration without printing it:

```powershell
docker compose --env-file .env -f docker-compose.member.yml config --quiet
```

Build and start both containers:

```powershell
docker compose --env-file .env -f docker-compose.member.yml up -d --build
```

Inspect their state and Member Service logs:

```powershell
docker compose -f docker-compose.member.yml ps
docker compose -f docker-compose.member.yml logs -f member-service
```

The default host endpoints are:

- Member Service: `http://localhost:8082`
- PostgreSQL for database tools: `localhost:5437`

The PostgreSQL host port is only for tools such as DBeaver. The application
does not use it.

## Stop

Stop the containers but retain database data:

```powershell
docker compose -f docker-compose.member.yml down
```

Only use `down --volumes` when intentionally deleting the local Member database.

## Reuse for another service

For each service, copy the same pattern and change:

1. Compose service and database names.
2. `build.args.MODULE`.
3. Application and database environment-variable prefixes.
4. Internal and host ports.
5. The named database volume.
