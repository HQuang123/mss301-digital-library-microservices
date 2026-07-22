# Frontend Integration Guidelines

This guide provides practical instructions for frontend developers or agents integrating a web client (e.g., React, Vue, Next.js) with the Member Service.

---

## 💾 1. Token Storage & Lifecycle

When a user logs in successfully via `/api/v1/auth/login`, they receive:
- `access_token` (Short-lived JWT, typically 5 minutes)
- `refresh_token` (Longer-lived OIDC token, e.g., 30 minutes)
- `expires_in` (Access token lifetime in seconds)
- `refresh_expires_in` (Refresh token lifetime in seconds)

### Storage Recommendations:
1. **Access Token**: Store in-memory (state/Context) or in an encrypted session variables slot. Avoid storing it in `localStorage` to mitigate Cross-Site Scripting (XSS) risks.
2. **Refresh Token**: Store in a secure, `HttpOnly`, `SameSite=Strict` cookie if handled by a backend-for-frontend (BFF) layer. If building a single-page application (SPA) directly targeting the service, store it in memory or secure storage.

### Authenticating Outbound Requests:
All protected routes require attaching the access token as a `Bearer` token in the `Authorization` header:
```http
Authorization: Bearer <access_token>
```

---

## 🔄 2. Token Refresh Strategy (Silent Refresh)

Do not let the user session freeze when the short-lived `access_token` expires.
- **Approach**: Set up an Axios/Fetch **Request interceptor** or background timer that checks the access token expiry.
- **Action**: Before the access token expires (or when the API returns a `401 Unauthorized`), call Keycloak's token endpoint or your gateway to refresh the token using the `refresh_token`.
- **Note**: The client-side logout endpoint `/api/v1/auth/logout` **requires** the `refresh_token` to be explicitly passed in the request body to successfully revoke the session.

---

## 🔍 3. Decoding & Inspecting the JWT claims

The client application can decode the OIDC JWT (using libraries like `jwt-decode` in JS/TS) to inspect claims locally. This helps with client-side routing, user greetings, and role protection.

### Critical Claim Mappings:
- **Unique User ID**: The `sub` claim. This matches the member's profile `id` in the database.
- **User Email**: The `email` claim.
- **Given Name**: The `given_name` claim (maps to `firstName`).
- **Family Name**: The `family_name` claim (maps to `lastName`).
- **Roles Array**: Located in `realm_access.roles`.
  - Check for presence of `"admin"` or `"librarian"` to conditionally show administration dashboards, user management routes, or catalog configuration utilities in your UI.

---

## 🧱 4. Handling Auth Errors in User Interfaces

Implement specific UX flows depending on HTTP error statuses returned by `/api/v1/auth/login`:

### A. E-mail Verification Block (`403 Forbidden`)
If login returns `403` with code `ACCOUNT_SETUP_INCOMPLETE` or `EMAIL_NOT_VERIFIED`:
- **UX Action**: Do **not** redirect the user to the home dashboard.
- **UX Action**: Redirect the user to a **"Verify Your Email"** screen advising them to use the verification email. Do not show a resend button until a rate-limited resend endpoint is implemented.

### B. Invalid Credentials (`401 Unauthorized`)
- **Condition**: Error code `INVALID_CREDENTIALS`.
- **UX Action**: Display a clear warning banner: `"Invalid username or password."`

### C. Rate-Limiting Lockout (`429 Too Many Requests`)
- **Condition**: Error code `TOO_MANY_ATTEMPTS`.
- **UX Action**: Block login attempts and show a countdown or message: `"Too many failed attempts. Please wait before trying again."`

---

## 👤 5. Profile & Session Management Pages

### Registration Flow Page
- Form fields: `email`, `password`, `firstName`, `lastName`.
- Submit to: `POST /api/v1/auth/register`.
- On success: Immediately redirect user to the verification warning screen (since their account is unverified by default).
- On `VERIFICATION_EMAIL_UNAVAILABLE`: Keep the form state and offer retry; the partially created Keycloak user has already been rolled back.

### Account Profile Page
- On load: Fetch user data from `GET /api/v1/members/me`.
- Display properties:
  - Avatar image (fetched from S3 or storage using the `avatarKey`).
  - Personal Information (First Name, Last Name, Phone, Email).
  - Membership Info: Type (`READER`), scanning Code (`memberCode`), current concurrent loan limit (`borrowingLimit`), and default loan duration (`loanPeriodDays`).
  - Financial Info: Outstanding balance (`outstandingBalance`).
- Profile Updates:
  - Form edits on `firstName`, `lastName`, `phone`, and `avatarKey`.
  - Submit payload via `PATCH /api/v1/members/me`.

---

## 🌐 6. Google & Social Identity Providers

Because the architecture delegates OIDC identity management to **Keycloak**, the frontend does not interact directly with Google APIs or Google Credentials. Keycloak abstracts social login:

### A. How Google Authentication is Triggered:
1. When Keycloak is configured with Google under **Identity Providers**, Keycloak's login interface will automatically render a **"Sign in with Google"** button.
2. If the frontend wants to redirect the user directly to Google's authentication page without prompting Keycloak's login dashboard, append the `kc_idp_hint` query parameter to the OIDC redirect URL:
   ```http
   GET https://keycloak.huynq.space/realms/digilib-realm/protocol/openid-connect/auth?client_id=digilib-auth&redirect_uri=<your-frontend-redirect>&response_type=code&scope=openid%20profile%20email&kc_idp_hint=google
   ```

### B. Forcing Google Account Selection (Choosing a Different Account):
By default, if the user already has an active Google session in the browser, Google will log them in automatically with the active account. To force Google to display the **"Choose an account"** screen so the user can select a different account, append the `prompt=select_account` parameter to the OIDC authorization redirect URL:
```http
GET https://keycloak.huynq.space/realms/digilib-realm/protocol/openid-connect/auth?client_id=digilib-auth&redirect_uri=<your-frontend-redirect>&response_type=code&scope=openid%20profile%20email&kc_idp_hint=google&prompt=select_account
```
> [!NOTE]
> Make sure "Forward Prompt" is enabled in the Google Identity Provider settings within the Keycloak Admin Console so Keycloak forwards this parameter to Google's authorization endpoint.

### C. Cleansing Browser Session on Logout (RP-Initiated Logout):
The backchannel logout endpoint `POST /api/v1/auth/logout` revokes tokens and invalidates the session in Keycloak's database, **and now also returns the RP-Initiated Logout URL** that the frontend must redirect the browser to.

> [!IMPORTANT]
> Without this browser redirect, Keycloak's browser cookies (`AUTH_SESSION_ID`, `KEYCLOAK_SESSION`, `KEYCLOAK_IDENTITY`) survive on the Keycloak domain. On the next login attempt, Keycloak sees these cookies and **silently reuses the old session without ever redirecting to Google**. This is why `prompt=select_account` has no effect — the request never reaches Google.

#### Updated Logout API Request:
```http
POST /api/v1/auth/logout
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "refresh_token": "<refresh_token>",
  "id_token": "<id_token>",
  "post_logout_redirect_uri": "http://localhost:5173"
}
```

* `refresh_token` **(required)**: The refresh token to revoke.
* `id_token` *(optional)*: The raw `id_token` received at login. Including this skips Keycloak's logout confirmation screen.
* `post_logout_redirect_uri` *(optional)*: Where Keycloak should redirect the browser after clearing its cookies. **Must** be registered in Keycloak under Clients → `digilib-auth` → "Valid post logout redirect URIs".

#### Updated Logout API Response:
```json
{
  "logout_redirect_url": "http://localhost:8180/realms/digilib-realm/protocol/openid-connect/logout?client_id=digilib-auth&id_token_hint=...&post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A5173"
}
```

#### Complete Frontend Logout Flow:
```javascript
async function logout() {
  // Step 1: Call backend to revoke tokens (returns the redirect URL)
  const response = await fetch('/api/v1/auth/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      refresh_token: refreshToken,
      id_token: idToken,                          // store this at login!
      post_logout_redirect_uri: window.location.origin,
    }),
  });
  const { logout_redirect_url } = await response.json();

  // Step 2: Clear local state
  clearLocalStorage();
  clearSessionStorage();

  // Step 3: Redirect browser to Keycloak to clear its cookies
  window.location.href = logout_redirect_url;
}
```

> [!NOTE]
> The login response (`POST /api/v1/auth/login` and `POST /api/v1/auth/oauth2/exchange`) now includes an `id_token` field. **Store this value** (in-memory or secure storage) so you can pass it back during logout.

### D. Tokens & Profile Generation:
1. After Google authenticates the user, Keycloak registers the profile internally and issues a standard OIDC token (JWT) signed by Keycloak.
2. The frontend attaches this token in the header as a `Bearer` token for all API requests to the Member Service.
3. The first time the user accesses the library, and the frontend requests `GET /api/v1/members/me`, the backend performs **Just-In-Time (JIT) Profile Generation** utilizing the OIDC standard claims parsed from the Google token (`sub`, `email`, `given_name`, `family_name`). This maps the user profile automatically in the local database.

