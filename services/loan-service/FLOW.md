# Loan Service Flows

This document defines the business flows orchestrated by Loan Service and the contracts between Loan Service, Member Service, Catalog Service, Fine Service, and Notification Service.

## Design rules

- The frontend calls Loan Service to borrow, return, renew, or report a lost book.
- Loan Service owns the loan lifecycle and coordinates book-copy status changes related to a loan.
- Fine Service is the source of truth for borrowing eligibility, fine amounts, and payment status.
- Loan Service decides when a fine must be created or updated, but it does not store fine amounts.
- Notification Service is responsible for creating and delivering notifications. A notification failure must not roll back a completed return transaction.
- Internal requests include the `X-Internal-Api-Key` header.
- Fine Service must process requests idempotently to prevent duplicate fines for the same loan and reason.

## Loan statuses

- `BORROWED`: the book is currently on loan.
- `OVERDUE`: the book is still on loan and has passed its due date.
- `RETURNED`: the book has been returned.
- `LOST`: the borrowed book has been reported as lost.

Only loans in `BORROWED` or `OVERDUE` status can be returned or reported as lost.

## Flow 1: Borrow a book

### Frontend API

```http
POST /api/v1/rent-books
Content-Type: application/json
```

```json
{
  "memberId": "member-keycloak-id",
  "bookId": 101,
  "bookType": "PHYSICAL",
  "idempotencyKey": "borrow-20260718-001"
}
```

### Processing steps

1. Loan Service searches for an existing loan by `idempotencyKey`. If one exists, it returns the previous result.
2. Loan Service calls Fine Service:

```http
GET /internal/fines/students/{studentId}/borrow-eligibility
X-Internal-Api-Key: <internal-api-key>
```

3. Fine Service checks whether the member has any `PENDING` fines and returns:

```json
{
  "canBorrow": true,
  "reason": null
}
```

4. If `canBorrow = false`, Loan Service rejects the request with HTTP `409 Conflict`.
5. Loan Service calls Member Service to retrieve `borrowingLimit` and `loanPeriodDays`.
6. Loan Service counts the member's current `BORROWED` and `OVERDUE` loans.
7. For a physical book, Loan Service calls Catalog Service to reserve an available copy.
8. Loan Service creates a loan with `BORROWED` status, records its history, and creates a `LoanCreatedEvent` outbox event.
9. Loan Service returns HTTP `201 Created`.

If loan creation fails after a copy has been reserved, Loan Service calls Catalog Service to release the copy.

## Flow 2: Return a book on time

### Frontend API

```http
POST /api/v1/loans/return
Content-Type: application/json
```

```json
{
  "loanId": 1001,
  "idempotencyKey": "return-20260718-001"
}
```

### Processing steps

1. Loan Service verifies that the loan exists and has `BORROWED` or `OVERDUE` status.
2. Loan Service calculates `overdueDays` from `dueDate` and the actual return time.
3. If `overdueDays = 0`, Loan Service does not call Fine Service.
4. Loan Service changes the status to `RETURNED` and records `returnedAt`.
5. Loan Service calls Catalog Service to change the copy status to `AVAILABLE`.
6. Loan Service creates a `BookReturnedEvent` outbox event.
7. Loan Service calls Notification Service to send a return confirmation:

```http
POST /api/notifications/return-confirmation
X-Internal-Api-Key: <internal-api-key>
Content-Type: application/json
```

```json
{
  "studentId": 123,
  "studentEmail": "student@example.com",
  "bookTitle": "Clean Code",
  "returnedAt": "2026-07-18T10:30:00"
}
```

If Notification Service is unavailable, Loan Service logs a warning and keeps the completed return transaction.

## Flow 3: Return a book late

The initial steps are the same as Flow 2. When `overdueDays > 0`, Loan Service retrieves the member and book information and calls Fine Service **before** updating the loan:

```http
POST /internal/fines/from-overdue-return
X-Internal-Api-Key: <internal-api-key>
Content-Type: application/json
```

```json
{
  "studentId": "STU001",
  "loanId": "1001",
  "bookId": "101",
  "bookCopyId": "5001",
  "bookTitle": "Clean Code",
  "bookValue": 250000,
  "dueDate": "2026-07-14",
  "returnDate": "2026-07-18",
  "overdueDays": 4
}
```

After Fine Service completes successfully:

1. The loan changes to `RETURNED`.
2. The catalog copy changes to `AVAILABLE`.
3. Loan Service creates a `BookReturnedLateEvent` outbox event.
4. Loan Service calls Notification Service to send a return confirmation.

Fine Service calculates the fine amount and stores a `PENDING` fine. Loan Service does not store `fineAmount`, `compensationAmount`, or `totalAmount`.

If Fine Service is unavailable or returns an error, Loan Service returns HTTP `502 Bad Gateway` and does not complete the return.

## Flow 4: Loan overdue by more than 30 days

Loan Service runs a scheduled job according to `OVERDUE_FINE_JOB_CRON`, which defaults to `02:15` every day.

### Processing steps

1. Find loans with `BORROWED` or `OVERDUE` status whose `dueDate` is more than 30 days in the past.
2. If a loan still has `BORROWED` status, change it to `OVERDUE`.
3. Retrieve the `memberCode`, book title, and book value from Member Service and Catalog Service.
4. Call Fine Service:

```http
POST /internal/fines/from-overdue-threshold
X-Internal-Api-Key: <internal-api-key>
Content-Type: application/json
```

```json
{
  "studentId": "STU001",
  "loanId": "1001",
  "bookId": "101",
  "bookCopyId": "5001",
  "bookTitle": "Clean Code",
  "bookValue": 250000,
  "overdueDays": 31,
  "compensationEnabled": true
}
```

5. Fine Service creates a new fine or updates the existing fine for the loan.
6. An error for one loan is logged and does not prevent the job from processing the remaining loans.

The job can be disabled with `OVERDUE_FINE_JOB_ENABLED=false`.

## Flow 5: Report a lost book

### Frontend API

```http
POST /api/v1/loans/{loanId}/lost
X-Actor-Id: <member-or-librarian-id>
```

### Processing steps

1. Loan Service verifies that the loan has `BORROWED` or `OVERDUE` status.
2. Loan Service retrieves the member information, book title, and book value.
3. Loan Service calculates `overdueDays`; the value is `0` if the loan is not overdue.
4. Loan Service calls Fine Service before changing the loan status:

```http
POST /internal/fines/from-lost-book
X-Internal-Api-Key: <internal-api-key>
Content-Type: application/json
```

```json
{
  "studentId": "STU001",
  "loanId": "1001",
  "bookId": "101",
  "bookCopyId": "5001",
  "bookTitle": "Clean Code",
  "bookValue": 250000,
  "overdueDays": 10
}
```

5. Fine Service creates a `LOST_BOOK` fine containing any overdue charge and the book compensation amount.
6. Loan Service changes the loan status to `LOST`.
7. Loan Service calls Catalog Service to change the copy status to `LOST`.
8. Loan Service creates a `BookLostEvent` outbox event.

If Fine Service fails, Loan Service returns HTTP `502 Bad Gateway` and does not change the loan status to `LOST`.

## Flow 6: Renew a loan

### Frontend API

```http
PUT /api/v1/loans/{loanId}/renew
X-Actor-Id: <member-or-librarian-id>
```

### Processing steps

1. Loan Service verifies that the loan can be renewed based on its status and renewal count.
2. Update `dueDate` and increment `renewalCount`.
3. Record the status history.
4. Create a `LoanRenewedEvent` outbox event.

The renewal flow currently does not call Fine Service or Notification Service.

## Flow 7: Query loans

```http
GET /api/v1/loans/{loanId}
GET /api/v1/loans?page=0&size=20
GET /api/v1/loans/my-loans?memberId={memberId}
```

These APIs only read Loan Service data and do not call Fine Service or Notification Service.

## Integration error rules

- Business rule violations, such as an unpaid fine or an inactive loan, return HTTP `409 Conflict`.
- A request for a nonexistent loan returns the corresponding Loan Service request error.
- Loan Service must not complete the related business change when a required Fine, Member, or Catalog Service request fails.
- A return-confirmation failure from Notification Service is only logged and does not roll back the loan.

## Service configuration

```properties
CATALOG_SERVICE_URL=http://localhost:8082
MEMBER_SERVICE_URL=http://localhost:8083
LOAN_SERVICE_PORT=8084
NOTIFICATION_SERVICE_URL=http://localhost:8085
FINE_SERVICE_URL=http://localhost:8086
DEFAULT_BOOK_VALUE=250000
OVERDUE_FINE_JOB_ENABLED=true
OVERDUE_FINE_JOB_CRON=0 15 2 * * *
INTERNAL_API_KEY=<shared-secret>
```

Catalog Service may not currently return `bookValue`; in that case, Loan Service uses `DEFAULT_BOOK_VALUE`. Notification Service currently accepts a numeric `studentId`, so Loan Service maps a string member ID to a stable number when sending a notification.
