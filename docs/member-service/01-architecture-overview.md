# System & Security Architecture

The Member Service operates as the profile, account configuration, and identity lifecycle coordinator for the Digital Library application.

---

## 🛠️ Technology Stack

Rather than using traditional servlet-based, thread-per-request blocking APIs, the Member Service is built using a modern **reactive, non-blocking microservice stack**:

1. **Spring Boot WebFlux**: Core web framework for asynchronous request handling.
2. **Spring Security Reactive**: Secures endpoints and parses OIDC JSON Web Tokens (JWT).
3. **Spring Data R2DBC**: Reactive Database Connectivity framework interfacing with PostgreSQL.
4. **Keycloak Integration**: Serves as the master Identity Provider (IdP) for storing user credentials, managing OIDC clients, and verifying accounts.
5. **Spring Cloud Eureka Client**: Dynamically registers the service under the name `member-service` on port `8081`.

---

## 🔒 Security & Identity Architecture

The backend implements token-based stateless security, offloading identity verification entirely to Keycloak.

```
┌──────────┐          1. Login Request          ┌────────────────┐
│  Client  │ ─────────────────────────────────> │ Member Service │
│ (Browser)│                                    │  (Port 8081)   │
└──────────┘ <───────────────────────────────── └────────────────┘
      ▲             2. JWT Token Response               │
      │                                                 │ 3. Keycloak Admin
      │                                                 │    API Operations
      │ 4. Authenticated Request                        ▼ (OAuth2 credentials)
      │    Authorization: Bearer <JWT>          ┌────────────────┐
      └────────────────────────────────────────> │    Keycloak    │
                                                 │  (Port 8080)   │
                                                 └────────────────┘
```

### 1. Token Validation Config
The service acts as a JWT resource server. When a client performs an API request (excluding public paths), the Authorization header is validated against Keycloak's JWK public keys:
- **Issuer URI**: `https://keycloak.huynq.space/realms/digilib-realm`
- **JWK Set Certificate URI**: `https://keycloak.huynq.space/realms/digilib-realm/protocol/openid-connect/certs`

This is defined in the [SecurityConfig.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/config/SecurityConfig.java) file.

### 2. Authorization Role Mapping
Keycloak claims are structured differently than Spring's native role structure. In the incoming JWT, Keycloak lists user roles inside the `realm_access.roles` claim as a JSON array:

```json
{
  "realm_access": {
    "roles": ["admin", "librarian", "offline_access"]
  }
}
```

The [SecurityConfig.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/config/SecurityConfig.java) contains a custom `grantedAuthoritiesExtractor` that:
1. Extracts the list of roles from the `realm_access` map.
2. Prefixes each role with `ROLE_` and capitalizes it (e.g. `admin` -> `ROLE_ADMIN`).
3. Registers it within the Spring Reactive Security context.

This enables annotation-based endpoint security, such as:
```java
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
```

---

## 🔌 Public vs. Protected Endpoints

The network endpoints are secured using Spring Exchange Security rules:

- **Public Endpoints** (No Auth Required):
  - `/actuator/**` (Health check and telemetry)
  - `POST /api/v1/auth/register` (Registration endpoint)
  - `POST /api/v1/auth/login` (Login endpoint)
- **Protected Endpoints** (Valid Access JWT Required):
  - `POST /api/v1/auth/logout` (Invalidates session)
  - `GET /api/v1/members/me` (Fetches own profile details)
  - `PATCH /api/v1/members/me` (Updates own profile details)
  - `GET /api/v1/members/{memberId}` (Internal lookup / Inter-service communication)
- **Role-Restricted Endpoints** (Requires `ADMIN` or `LIBRARIAN` role):
  - `GET /api/v1/members` (Retrieves full list of member profiles)
