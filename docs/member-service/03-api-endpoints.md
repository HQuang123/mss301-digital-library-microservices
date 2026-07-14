# API Endpoint Reference & Contracts

All endpoints are hosted relative to the prefix `/api/v1`.

---

## 🔑 Authentication Endpoints

### 1. Register a Member
- **Method**: `POST`
- **Path**: `/api/v1/auth/register`
- **Auth**: Public (None)
- **Request Body (`RegisterRequest`)**:
  ```json
  {
    "email": "user@example.com",
    "password": "SecurePassword123",
    "firstName": "John",
    "lastName": "Doe"
  }
  ```
  - `email` (String): Must be a valid email format, cannot be blank. Unique.
  - `password` (String): Must be between 8 and 64 characters, cannot be blank.
  - `firstName` (String, Optional): Up to 100 characters.
  - `lastName` (String, Optional): Up to 100 characters.
- **Success Response**: `201 Created`
  - Body: **Member Response** (see below). The profile is generated with status `READER` and default limits.
- **Error Responses**:
  - `400 Bad Request`: Input validation failed (e.g. invalid email format, short password).
  - `409 Conflict`: Email is already registered.
  - `503 Service Unavailable`: Keycloak identity server is unreachable.

### 2. Login
- **Method**: `POST`
- **Path**: `/api/v1/auth/login`
- **Auth**: Public (None)
- **Request Body (`LoginRequest`)**:
  ```json
  {
    "username": "user@example.com",
    "password": "SecurePassword123"
  }
  ```
  - `username` (String): Cannot be blank. Identifies the member's email.
  - `password` (String): Cannot be blank.
- **Success Response**: `200 OK`
  - Body: **Token Response** (see below). Contains access and refresh tokens.
- **Error Responses**:
  - `400 Bad Request`: Missing fields.
  - `401 Unauthorized`: Invalid username or password.
  - `403 Forbidden`: Email address not verified (verification link pending) or account disabled.
  - `429 Too Many Requests`: Account locked due to too many failed attempts.

### 3. Logout
- **Method**: `POST`
- **Path**: `/api/v1/auth/logout`
- **Auth**: Bearer Token in `Authorization` header (`Authorization: Bearer <access_token>`)
- **Request Body (`LogoutRequest`)**:
  ```json
  {
    "refreshToken": "eyJhbGciOi..."
  }
  ```
  - `refreshToken` (String): The active refresh token to be revoked. Cannot be blank.
- **Success Response**: `204 No Content` (Empty body).
- **Error Responses**:
  - `400 Bad Request`: Refresh token is invalid or already expired/revoked.
  - `401 Unauthorized`: Access token in Header is missing, expired, or invalid.

---

## 👤 Member Profile Endpoints

### 1. Retrieve Current Member Profile
- **Method**: `GET`
- **Path**: `/api/v1/members/me`
- **Auth**: Bearer Token in `Authorization` header
- **Behavior**: Reads the `sub` (Keycloak UUID) and claims from the JWT. If the profile doesn't exist in the database, it automatically generates a default profile dynamically (JIT provisioning).
- **Success Response**: `200 OK`
  - Body: **Member Response** (see below).

### 2. Update Member Profile
- **Method**: `PATCH`
- **Path**: `/api/v1/members/me`
- **Auth**: Bearer Token in `Authorization` header
- **Request Body (`MemberUpdateRequest`)**:
  ```json
  {
    "firstName": "Johnathan",
    "lastName": "Smith",
    "phone": "+1234567890",
    "avatarKey": "avatars/john-doe.png"
  }
  ```
  - `firstName` (String, Optional): Up to 100 characters.
  - `lastName` (String, Optional): Up to 100 characters.
  - `phone` (String, Optional): Up to 30 characters.
  - `avatarKey` (String, Optional): Key or URL path up to 512 characters.
  - *Note: Only non-null fields in the request body are updated. Structural properties (`email`, `memberCode`, `id`) are immutable.*
- **Success Response**: `200 OK`
  - Body: **Member Response** (see below).
- **Error Responses**:
  - `400 Bad Request`: Validation failure (field lengths exceeded).
  - `404 Not Found`: Profile not found in the DB.

### 3. Retrieve Member Profile by ID (Internal / Inter-service)
- **Method**: `GET`
- **Path**: `/api/v1/members/{memberId}`
- **Auth**: Bearer Token in `Authorization` header
- **Success Response**: `200 OK`
  - Body: **Member Response** (see below).
- **Error Responses**:
  - `404 Not Found`: Member profile with the given UUID does not exist.

### 4. Retrieve All Members (Admin / Librarian only)
- **Method**: `GET`
- **Path**: `/api/v1/members`
- **Auth**: Bearer Token in `Authorization` header. User must have Keycloak roles `admin` or `librarian`.
- **Success Response**: `200 OK`
  - Body: Array of **Member Response** objects.
- **Error Responses**:
  - `403 Forbidden`: User does not possess the required `ADMIN` or `LIBRARIAN` authority.
  - `404 Not Found`: Member list is empty in the database.

### 5. Update Member Status (Admin / Librarian only)
- **Method**: `PUT`
- **Path**: `/api/v1/members/{memberId}/status`
- **Auth**: Bearer Token in `Authorization` header. User must have Keycloak roles `admin` or `librarian`.
- **Request Body (`MemberStatusRequest`)**:
  ```json
  {
    "status": "LOCKED"
  }
  ```
  - `status` (String): Must be one of `UNLOCKED`, `SOFT_LOCKED`, or `LOCKED` (case-insensitive validation). Cannot be blank.
- **Success Response**: `200 OK`
  - Body: **Member Response** (see below).
- **Error Responses**:
  - `400 Bad Request`: Validation failure (status is blank or has invalid string value).
  - `403 Forbidden`: User does not possess the required `ADMIN` or `LIBRARIAN` authority.
  - `404 Not Found`: Target member profile does not exist in the database.

---

## 📦 Data Transfer Object (DTO) Schemas

### Token Response Schema (`TokenResponse`)
Returned upon successful login.
```json
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "eyJhbGciOi...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer"
}
```

### Member Response Schema (`MemberResponse`)
Returned for all successful profile fetches, creations, and updates.
```json
{
  "id": "2bc35a64-28e4-4c8d-bf8d-d790f9ea703f",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "memberType": "READER",
  "memberCode": "LIB-4B52FD6A",
  "borrowingLimit": 5,
  "loanPeriodDays": 14,
  "outstandingBalance": 0.00,
  "avatarKey": "avatars/john-doe.png",
  "status": "UNLOCKED",
  "createdAt": "2026-07-02T08:00:00Z",
  "updatedAt": "2026-07-02T08:15:30Z"
}
```
