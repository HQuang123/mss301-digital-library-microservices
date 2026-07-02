# Authentication & Lifecycle Flows

The Member Service acts as an OIDC client interface, wrapping complex Keycloak authentication exchanges into easy-to-use endpoints for the client application.

---

## 📝 1. User Registration Saga (With Compensation Rollback)

Registration is handled as a two-step transactional saga to prevent orphaned accounts or inconsistent state across Keycloak and our local PostgreSQL database.

```
[Client Request]
       │
       ▼
 1. createUser() ────► (Keycloak creates user with emailVerified = false)
       │
       ▼
 2. setPassword() ───► (Keycloak configures a non-temporary password)
       │
       ▼
 3. sendVerification() ► (Keycloak dispatches email. Failures here are logged as non-fatal)
       │
       ▼
 4. saveProfile() ───► [Database Profile Creation]
       │
       ├─► Success: Return MemberResponse (201 Created)
       │
       └─► Failure: Trigger Rollback ──► deleteUser() in Keycloak ──► Return 500 Error
```

### Flow Details:
1. **Keycloak Provisioning**: The backend uses its admin credentials (obtained via client credentials grant) to create the user in Keycloak. The user is created as `enabled: true`, but `emailVerified: false`.
2. **Password Configuration**: The password is saved as a permanent (non-temporary) credentials payload.
3. **Verification Dispatch**: An asynchronous request is fired to Keycloak to dispatch its native verification email. If Keycloak fails to dispatch the email (e.g. SMTP configuration error), the saga logs a warning but *does not block* registration.
4. **Database Sync & Rollback**:
   - The user profile is created in the PostgreSQL `member_profiles` table, linking Keycloak's UUID (`sub`) as the primary key `id`.
   - **Compensating Action**: If the database insert fails (e.g. connection timeout, constraints violation), a rollback is triggered. The service issues a `DELETE` command to Keycloak for that user to prevent orphan identities, then returns a `500 Internal Server Error` to the client.

This logic is defined in [AuthService.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/domain/service/AuthService.java#L39-L85).

---

## 🔑 2. User Login Flow

Login uses the **Resource Owner Password Credentials (ROPC)** OIDC grant. The backend securely forwards the client credentials to Keycloak and maps responses to clean HTTP statuses.

### Error Mappings:
Keycloak raw responses can be verbose or leaky. The service intercepts error JSONs and translates them to the following client-facing statuses:

| Keycloak Error Condition / Substring | Local HTTP Code | Client Message |
| :--- | :--- | :--- |
| `invalid_grant` | `401 Unauthorized` | "Invalid username or password." |
| `Account is not fully set up` / `email_not_verified` | `403 Forbidden` | "Your account setup is not complete. Please check your email for a verification link." |
| `Account disabled` | `403 Forbidden` | "Your account has been disabled. Please contact support." |
| `Too many failed attempts` | `429 Too Many Requests` | "Too many failed attempts. Please wait before trying again." |
| Any other system fail | `503 Service Unavailable` | "Authentication service is temporarily unavailable." |

This mapping logic is located in [AuthService.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/domain/service/AuthService.java#L154-L186).

---

## 🚪 3. User Logout Flow

The logout endpoint requires authentication (a valid Bearer JWT in the request headers) **AND** the payload containing the `refresh_token`. It performs two actions:

1. **Token Revocation (`/revoke`)**: Revokes the refresh token so it can no longer be used for silent token renewals.
2. **Backchannel Session Invalidation (`/logout`)**: Contacts Keycloak to terminate the user's active session. This immediately invalidates the access token and any other tokens issued during that session across all devices.

Logout is best-effort. If the session logout fails but the revocation succeeds, the endpoint will still return `204 No Content`.
