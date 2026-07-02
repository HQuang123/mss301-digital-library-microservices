# Member Profile Domain Model & Business Rules

The database representation and logical structure of member accounts are modeled under the `MemberProfile` aggregate.

---

## 🗄️ Database Schema (`member_profiles`)

The database uses PostgreSQL. Because this service uses Spring Data R2DBC, schema queries are reactive.

```sql
CREATE TABLE member_profiles (
    id VARCHAR(255) PRIMARY KEY, -- Matches Keycloak sub (UUID) field
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(30),
    member_type VARCHAR(50),
    member_code VARCHAR(50),
    borrowing_limit INTEGER NOT NULL,
    loan_period_days INTEGER NOT NULL,
    outstanding_balance NUMERIC(12, 2),
    avatar_key VARCHAR(512),
    status VARCHAR(50),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

-- Unique constraints to ensure integrity
CREATE UNIQUE INDEX IF NOT EXISTS ux_member_profiles_email
    ON member_profiles (email)
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_member_profiles_member_code
    ON member_profiles (member_code)
    WHERE member_code IS NOT NULL;
```

This is defined in the [V1_init_db.sql](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/resources/V1_init_db.sql) file.

---

## 🧱 MemberProfile Properties

The properties of the domain entity mapped to database columns are:

| Field Name | Type | Description |
| :--- | :--- | :--- |
| `id` | `VARCHAR(255)` | **Primary Key**. Maps directly to Keycloak's unique user identifier (`sub`). Immutable. |
| `email` | `VARCHAR(255)` | User's email. Synced from Keycloak. Unique, Immutable. |
| `firstName` | `VARCHAR(100)` | Member's first name. Editable. |
| `lastName` | `VARCHAR(100)` | Member's last name. Editable. |
| `phone` | `VARCHAR(30)` | Member's telephone number. Editable. |
| `memberType` | `VARCHAR(50)` | Member tier/role class (e.g. `READER`). Defaults to `READER`. |
| `memberCode` | `VARCHAR(50)` | Public identity code for scanning/lookup. Unique, Immutable. |
| `borrowingLimit` | `INTEGER` | Maximum active loans allowed simultaneously. Defaults to `5`. |
| `loanPeriodDays` | `INTEGER` | Duration in days for standard book loans. Defaults to `14`. |
| `outstandingBalance` | `NUMERIC(12,2)`| Active fines/fee balance. Used by the Fine Service. Defaults to `0.00`. |
| `avatarKey` | `VARCHAR(512)` | Key path pointing to stored avatar images. Editable. |
| `status` | `VARCHAR(50)` | Security status of the profile (`UNLOCKED`, `SOFT_LOCKED`, `LOCKED`). Defaults to `UNLOCKED`. |
| `createdAt` | `TIMESTAMPTZ`  | Registration timestamp. Set once on profile generation. |
| `updatedAt` | `TIMESTAMPTZ`  | Profile modification timestamp. Updated automatically on edits. |

---

## ⚡ Business Rules & System Defaults

### 1. Just-In-Time (JIT) Profile Provisioning
If a user registers through Keycloak but does not have a profile, or if a user logs in and the database lacks their record, the service performs **JIT Profile Generation** when the client calls `GET /api/v1/members/me`.
The system dynamically creates and saves a profile with the following default values:

- **First Name Fallback**: If Keycloak claims do not contain a given name, defaults to `"Library"`.
- **Last Name Fallback**: If family name is missing, defaults to `"Member"`.
- **Member Type**: `"READER"`
- **Member Code**: Generated as `LIB-` appended with an 8-character uppercase substring of a random UUID (e.g., `LIB-5A3F9D2E`).
- **Borrowing Limit**: `5` concurrent books.
- **Loan Duration**: `14` days.
- **Outstanding Balance**: `0.00` (Zero).
- **Status**: `"UNLOCKED"`

This logic is located in [MemberProfileService.java](file:///media/simpi/program-files/Program%20Files/Documents/Study/Code/MSS301/mss301-digital-library-microservices/services/member-service/src/main/java/fu/edu/mss301/digilib/member/domain/service/MemberProfileService.java#L28-L50).

### 2. Profile Mutability Rules
- **Editable Fields**: Members can modify their own `firstName`, `lastName`, `phone`, and `avatarKey` via `PATCH /me`.
- **Immutable Fields**: Systems security rules prevent modification of `id`, `email`, `memberType`, `memberCode`, `borrowingLimit`, `loanPeriodDays`, `outstandingBalance`, and `status` by regular users.
- **Role-Based Overrides**: Only staff members with `LIBRARIAN` or `ADMIN` roles can adjust parameters like `borrowingLimit` or `loanPeriodDays` (which are managed via future admin routes) or update the member's security `status`.
