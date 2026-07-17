# PR4 Release Gate

PR4 adds a repeatable gate for merging the infrastructure hardening branch into `main`.

## Automated checks

The `Release gate` GitHub Actions workflow runs on every pull request and on pushes to `main`:

1. Gateway, Member, and Catalog Maven tests run with Java 21.
2. Catalog starts PostgreSQL 16 through Testcontainers, applies every Flyway migration, validates the JPA schema, and verifies that seed data exists.
3. All Docker Compose files are parsed with required credentials supplied only as CI placeholders.
4. Tracked `.env` files and PEM/OpenSSH private-key markers fail the build.

The Catalog test requires a working Docker daemon. It deliberately fails when Docker is unavailable because PostgreSQL-specific migrations must not be silently tested against H2.

## Local verification

Run these commands from the repository root before pushing:

```powershell
mvn -pl infra/api-gateway,services/member-service -am test
mvn -pl services/catalog-service -am test
docker compose -f docker-compose.yml config --quiet
docker compose -f docker-compose.infra.yml config --quiet
docker compose -f infra/monitoring/docker-compose-infra.yaml config --quiet
```

Docker Compose reads the local `.env` file. Keep `.env` untracked and use `.env.example` only as a template with non-secret placeholders.

## Manual smoke tests

- An unauthenticated protected Gateway route returns `401` with code `AUTHENTICATION_REQUIRED`.
- A non-admin JWT calling an operational Actuator endpoint returns `403` with code `ACCESS_DENIED`.
- Invalid login credentials return `401` with code `INVALID_CREDENTIALS`.
- A registration where Keycloak cannot send verification email returns `503` with code `VERIFICATION_EMAIL_UNAVAILABLE`; retrying does not hit an orphaned-user conflict.
- A valid member JWT can still reach Catalog through the Gateway.
- Circuit-breaker fallback and admin-only Actuator checks from PR3 remain valid.

## Merge criteria

- The GitHub `Release gate` workflow is green.
- No credential value from a developer `.env` file appears in the diff or Git history introduced by the branch.
- Hosted Keycloak has a valid SMTP configuration and the regenerated client secret is present in the deployment secret store.
- The hosted Keycloak issuer used by Gateway and Member Service is identical.
