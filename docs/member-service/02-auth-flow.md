# Authentication & Lifecycle Flows

The Member Service acts as an OIDC client interface, wrapping complex Keycloak authentication exchanges into easy-to-use endpoints for the client application.

---

## 📝 1. User Registration Saga (With Compensation Rollback)

Registration is handled as a compensating saga to prevent orphaned accounts or inconsistent state across Keycloak and the local PostgreSQL database.

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
 3. sendVerification() ► (Keycloak dispatches the verification email)
       │
       ▼
 4. saveProfile() ───► [Database Profile Creation]
       │
       ├─► Success: Return MemberResponse (201 Created)
       │
       └─► Failure after createUser(): deleteUser() in Keycloak, then return a structured error
```

### Flow Details:
1. **Keycloak Provisioning**: The backend uses its admin credentials (obtained via client credentials grant) to create the user in Keycloak. The user is created as `enabled: true`, but `emailVerified: false`.
2. **Password Configuration**: The password is saved as a permanent (non-temporary) credentials payload.
3. **Verification Dispatch**: Keycloak dispatches its native verification email. If dispatch fails (for example, because SMTP is unavailable), registration returns `503 VERIFICATION_EMAIL_UNAVAILABLE` and deletes the newly created Keycloak user. The API never reports a successful registration when no verification email was sent.
4. **Database Sync & Rollback**:
   - The user profile is created in the PostgreSQL `member_profiles` table, linking Keycloak's UUID (`sub`) as the primary key `id`.
   - **Compensating Action**: If password setup, verification dispatch, or database insertion fails, the service issues a `DELETE` command to Keycloak for that user. A database failure returns `500 REGISTRATION_FAILED`. If the compensating delete also fails, that failure is logged without hiding the original client-facing error.

This logic is defined in `AuthService.java`.

---

## 🔑 2. User Login Flow

Login uses the **Resource Owner Password Credentials (ROPC)** OIDC grant. The backend securely forwards the client credentials to Keycloak and maps responses to clean HTTP statuses.

### Error Mappings:
Keycloak raw responses can be verbose or leaky. The service intercepts error JSONs and translates them to the following client-facing statuses:

| Keycloak condition | HTTP status | Stable error code |
| :--- | :--- | :--- |
| `invalid_grant` | `401 Unauthorized` | `INVALID_CREDENTIALS` |
| `Account is not fully set up` | `403 Forbidden` | `ACCOUNT_SETUP_INCOMPLETE` |
| `email_not_verified` | `403 Forbidden` | `EMAIL_NOT_VERIFIED` |
| `Account disabled` | `403 Forbidden` | `ACCOUNT_DISABLED` |
| Too many failed attempts | `429 Too Many Requests` | `TOO_MANY_ATTEMPTS` |
| Any other identity-provider failure | `503 Service Unavailable` | `AUTHENTICATION_SERVICE_UNAVAILABLE` |

Clients should branch on `code`, not on the human-readable `message`.

---

## 🚪 3. User Logout Flow

The logout endpoint requires authentication (a valid Bearer JWT in the request headers) **AND** the payload containing the `refresh_token`. It performs two actions:

1. **Token Revocation (`/revoke`)**: Revokes the refresh token so it can no longer be used for silent token renewals.
2. **Backchannel Session Invalidation (`/logout`)**: Contacts Keycloak to terminate the user's active session. This immediately invalidates the access token and any other tokens issued during that session across all devices.

Logout is best-effort. If the session logout fails but the revocation succeeds, the endpoint will still return `204 No Content`.
