# Member Service Documentation

Welcome to the **Member Service** backend system documentation. This set of markdown files is designed as a structured instruction set for the frontend developer or agent to understand the authentication mechanisms, domain model, API contracts, and integration requirements of the Member Service.

## 🗺️ Documentation Roadmap

1. **[01-architecture-overview.md](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/docs/member-service/01-architecture-overview.md)**
   Understand the reactive stack, reactive databases, port assignments, and central Keycloak identity integration.
2. **[02-auth-flow.md](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/docs/member-service/02-auth-flow.md)**
   Explore the step-by-step registration saga (with rollback compensation), Login credentials exchange (ROPC), and Logout/Revocation mechanisms.
3. **[03-api-endpoints.md](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/docs/member-service/03-api-endpoints.md)**
   Browse detailed API contracts, path structures, DTO models, validation constraints, and error response codes.
4. **[04-member-domain.md](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/docs/member-service/04-member-domain.md)**
   Inspect the database schema, entity structures, business rules (limits, member types, loan periods), and Just-In-Time (JIT) profile generation.
5. **[05-frontend-integration.md](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/docs/member-service/05-frontend-integration.md)**
   Follow instructions for the frontend developer/agent: managing JWT lifecycles, route guards using claims, and error handling strategies.

---

## ⚡ Quick System Reference

| Property | Value |
| :--- | :--- |
| **Service Name** | `member-service` |
| **Default Port** | `8081` |
| **Eureka Registry** | Enabled (resolves dynamically) |
| **Database** | PostgreSQL (`digilib_member`) via R2DBC |
| **Auth Provider** | Keycloak (`https://keycloak.huynq.space`) |
| **Keycloak Realm** | `digilib-realm` |
| **OIDC Client ID** | `digilib-auth` |

---

## 🔑 Key Codebase Files

- **Application Config**: [application.yaml](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/resources/application.yaml)
- **Database Schema**: [V1_init_db.sql](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/resources/V1_init_db.sql)
- **Security Logic**: [SecurityConfig.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/config/SecurityConfig.java)
- **Auth REST Layer**: [AuthController.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/api/controller/AuthController.java)
- **Member REST Layer**: [MemberProfileController.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/api/controller/MemberProfileController.java)
