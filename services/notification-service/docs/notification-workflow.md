# Notification Service Workflow

This document describes the intended flow for the notification-service to send
book-loan related emails and create in-app notification records. It builds on
the existing domain model:

- `NotificationPolicy` — a reusable template per `eventType` (subject/body template).
- `NotificationLog` — one row per (policy, student, channel) delivery attempt,
  with status `PENDING → SENT | FAILED` for `EMAIL`, and `UNREAD → READ` for `WEBSITE`.
- `NotificationAggregate` — enforces invariants (active policy, valid student,
  log belongs to the policy that created it) and is the only way to create/transition a `NotificationLog`.
- `NotificationChannel` — `EMAIL`, `WEBSITE`.

Event types to be seeded in `NotificationPolicy` (`event_type` column):

| Event type            | Scenario                                    |
|------------------------|----------------------------------------------|
| `BOOK_BORROWED`        | student borrows a book successfully          |
| `DUE_SOON`             | 3 days before due date                       |
| `OVERDUE_REMINDER`     | daily reminder while a loan is overdue       |
| `RETURN_CONFIRMATION`  | book returned by student                     |

## NotificationResponse shape

All endpoints that return a notification log respond with this JSON shape:

```json
{
  "id": 1,
  "eventType": "BOOK_BORROWED",
  "studentId": "a91940da-c7e0-477a-ba59-ca34756ced99",
  "studentEmail": "phukak12345@gmail.com",
  "channel": "WEBSITE",
  "status": "UNREAD",
  "subject": "Your loan is confirmed — Clean Code",
  "body": "Hi, you have successfully borrowed \"Clean Code\". Due date: 2026-08-09.",
  "createdAt": "2026-07-19T09:15:00",
  "sentAt": null,
  "readAt": null,
  "failureReason": null
}
```

| Field           | Type             | Description                                                                 |
|-----------------|------------------|-----------------------------------------------------------------------------|
| `id`            | `Integer`        | Primary key of the `notification_log` row.                                  |
| `eventType`     | `String`         | One of `BOOK_BORROWED`, `DUE_SOON`, `OVERDUE_REMINDER`, `RETURN_CONFIRMATION`. |
| `studentId`     | `String`         | Keycloak user UUID — matches the `sub` claim in the student's JWT.          |
| `studentEmail`  | `String \| null` | Email address. `null` for `WEBSITE` channel rows.                           |
| `channel`       | `String`         | `EMAIL` or `WEBSITE`.                                                       |
| `status`        | `String`         | `PENDING`, `SENT`, `FAILED` for EMAIL; `UNREAD`, `READ` for WEBSITE.       |
| `subject`       | `String`         | `NotificationPolicy.subject_template` rendered with this log's `templateVariables`, e.g. book title substituted in. Stored on the log at creation time so historical notifications keep their original wording even if the policy is edited later. |
| `body`          | `String`         | `NotificationPolicy.body_template` rendered the same way as `subject`.      |
| `createdAt`     | `LocalDateTime`  | When the `notification_log` row was created (i.e. when the triggering event happened). Distinct from `sentAt` (email delivery) and `readAt` (student read time) — this is the only timestamp `WEBSITE` rows have until they're read. |
| `sentAt`        | `LocalDateTime \| null` | When the email was sent. `null` until delivery succeeds.             |
| `readAt`        | `LocalDateTime \| null` | When the student marked the in-app notification as read.             |
| `failureReason` | `String \| null` | Populated on `FAILED` email logs. `null` otherwise.                         |

`GET /api/notifications/me` and `GET /api/notifications` return a paginated wrapper:

```json
{
  "content": [ /* NotificationResponse[] */ ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

## High-level architecture

```
loan-service ──(REST/Feign or event)──▶ notification-service
                                            │
                                            ├─ NotificationController (REST trigger)
                                            ├─ Scheduled jobs (@Scheduled, cron)
                                            ├─ CreateNewNotificationUseCase
                                            │     └─ NotificationAggregate.createEmailLogFor / createWebsiteLogFor
                                            ├─ NotificationRepository (JPA) ── notification_log / notification_policy tables
                                            └─ Mail sender (spring-boot-starter-mail)
```

The service does not own loan/due-date data. It is triggered either:
- **push style**: `loan-service` calls a REST endpoint on `notification-service` when a domain event happens (loan created, returned), or publishes a message it consumes.
- **pull style**: `notification-service` runs a scheduled job that calls `loan-service` (via OpenFeign, already a dependency) to fetch loans due in 3 days / overdue loans, then triggers notifications for each.

Scenario (a) and (b) below are naturally scheduled/pull-based since they depend on the clock, not on a single event. Scenario (c) is naturally push-based since it fires exactly when the return happens.

---

## a. Book borrowed — in-app notification

**Trigger:** `NotificationController` REST endpoint, called by loan-service synchronously right after the borrow saga completes successfully.

```
POST /api/notifications
{
  "eventType": "BOOK_BORROWED",
  "studentId": 123,
  "studentEmail": "student@fu.edu.vn",
  "templateVariables": {
    "bookTitle": "Clean Code",
    "dueDate": "2026-08-09"
  }
}
```

1. loan-service calls this endpoint inside its borrow saga completion step.
2. `CreateNewNotificationUseCase` loads active `NotificationPolicy` where `eventType = BOOK_BORROWED`.
3. `NotificationAggregate.create(policy)`:
   - `createWebsiteLogFor(studentId)` → persist as `UNREAD` (drives the in-app badge/bell).
   - `createEmailLogFor(studentId, studentEmail)` → persist as `PENDING`, then send a borrow confirmation email.
4. Returns `NotificationResponse` (log ids + statuses) to loan-service — loan-service ignores the response body but checks for 2xx to confirm delivery was attempted.

**UI flow:** the frontend polls or subscribes to `GET /api/notifications/me?status=UNREAD` after a successful borrow. When the new `UNREAD` row appears, the notification bell badge increments. The student clicks the notification → frontend calls `PATCH /api/notifications/{id}/read` → status transitions to `READ`, `readAt` is stamped.

**Idempotency:** use the loan's idempotency key (passed as a `templateVariable`) to guard against duplicate rows if loan-service retries the call. Check for an existing `NotificationLog` with the same `policy + studentId + loanIdempotencyKey` before creating.

**Policy seed entry required:**

| field             | value                                                                      |
|-------------------|----------------------------------------------------------------------------|
| `event_type`      | `BOOK_BORROWED`                                                            |
| `subject_template`| `Your loan is confirmed — {{bookTitle}}`                                   |
| `body_template`   | `Hi, you have successfully borrowed "{{bookTitle}}". Due date: {{dueDate}}.` |
| `is_active`       | `true`                                                                     |

---

## b. Reminder — 3 days before due date

**Trigger:** scheduled job (e.g. daily at 07:00), not the controller — there is no user action that causes "3 days before due" to happen.

1. `DueDateReminderJob` (`@Scheduled(cron = "0 0 7 * * *")`) fires once a day.
2. Job calls `LoanServiceClient.getLoansDueOn(today + 3 days)` (Feign client to loan-service) to get `{ studentId, studentEmail, bookTitle, dueDate }[]`.
3. For each loan, if no `DUE_SOON` log already exists for this loan (dedup key: `studentId` + `eventType` + `dueDate`/loanId), call `CreateNewNotificationUseCase`:
   - Load active `NotificationPolicy` where `eventType = DUE_SOON`.
   - `NotificationAggregate.create(policy)`
   - `aggregate.createEmailLogFor(studentId, studentEmail)` → persist as `PENDING`.
   - `aggregate.createWebsiteLogFor(studentId)` → persist as `UNREAD` (in-app notification).
4. Render `subjectTemplate`/`bodyTemplate` with loan variables (book title, due date), send via `JavaMailSender`.
5. On success: `aggregate.markSent(log)`. On failure: `aggregate.markFailed(log, reason)` — the job continues to the next student and can retry failed logs on the next run.

**Idempotency:** because this job runs daily and a loan's due date doesn't move, guard against duplicate sends by checking for an existing `NotificationLog` for the same policy + student + loan (or same calendar day) before creating a new one.

---

## b. Overdue reminder — recurring daily job

**Trigger:** scheduled job, separate cron from (a), e.g. daily at 08:00.

1. `OverdueReminderJob` (`@Scheduled(cron = "0 0 8 * * *")`) fires once a day.
2. Job calls `LoanServiceClient.getOverdueLoans()` to get all loans where `dueDate < today` and not yet returned.
3. For each overdue loan, look up active `NotificationPolicy` where `eventType = OVERDUE_REMINDER`, then repeat the same create → send → mark flow as scenario (a):
   - One new `NotificationLog` (`EMAIL`, `PENDING`) is created **every day** the loan stays overdue — this is what makes it "send every day until returned," rather than a single job execution with an internal loop.
   - One new `NotificationLog` (`WEBSITE`, `UNREAD`) per day is optional; typically in-app just shows "N overdue" without spamming a new unread row per day — decide based on product requirement.
4. The job naturally stops sending for a given loan once loan-service reports it as returned (it drops out of `getOverdueLoans()`), so no separate "cancel" logic is required in notification-service.

**Failure handling:** a failed send on one day does not block future days — each day is an independent `NotificationLog`, `markFailed` on that day's log only.

---

## c. Return confirmation

**Trigger:** `NotificationController` REST endpoint (real-time, event-driven), called by loan-service synchronously right after it records the return.

```
POST /api/notifications/return-confirmation
{
  "studentId": 123,
  "studentEmail": "student@fu.edu.vn",
  "bookTitle": "Clean Code",
  "returnedAt": "2026-07-11T10:00:00"
}
```

1. Controller receives the request → maps to `NotificationCreateRequest` (or a dedicated `ReturnConfirmationRequest`) → delegates to `CreateNewNotificationUseCase`.
2. Use case loads active `NotificationPolicy` where `eventType = RETURN_CONFIRMATION`.
3. `NotificationAggregate.create(policy)` → `createEmailLogFor(studentId, studentEmail)` (persist `PENDING`) and `createWebsiteLogFor(studentId)` (persist `UNREAD`).
4. Send email immediately (synchronous call, or hand off to an async `@Async`/queue if latency to the caller matters).
5. `markSent` / `markFailed` accordingly, return `NotificationResponse` (log id + status) to the caller.
6. This also implicitly ends scenario (b)'s daily reminders for that loan, since loan-service no longer reports it as overdue.

---

## d. Mark notification as read

**Trigger:** student clicks a notification in the UI.

```
PATCH /api/notifications/{id}/read
Authorization: Bearer <student JWT>
```

1. Gateway forwards the request with the JWT. Notification-service extracts `studentId` from the JWT subject (or a custom claim) to verify ownership — a student must not be able to mark another student's notification as read.
2. Load `NotificationLog` by `id`. If not found → 404. If `log.studentId ≠ jwtStudentId` → 403.
3. If `log.channel ≠ WEBSITE` → 400 (only in-app notifications are "read"; EMAIL logs do not have a read state meaningful to the student).
4. If `log.status = READ` already → return 200 with the existing log (idempotent, no-op).
5. `NotificationAggregate.markRead(log)` → set `status = READ`, `readAt = now()`, persist.
6. Return updated `NotificationResponse`.

**New use case:** `MarkNotificationReadUseCase` — keeps the read transition out of the controller.

---

## e. Get my notifications

**Trigger:** frontend loads the notification bell / notification list for the logged-in student.

```
GET /api/notifications/me?status=UNREAD&page=0&size=20
Authorization: Bearer <student JWT>
```

1. Extract `studentId` from JWT (same claim used in the borrow/return flows).
2. Query `NotificationLog` where `studentId = :studentId AND channel = WEBSITE` (only in-app rows are surfaced to the student; EMAIL rows are delivery receipts, not user-visible). Optionally filter by `status` if provided.
3. Return paginated `NotificationResponse` sorted by `createAt DESC`.
4. The unread count for the bell badge can be derived from `status = UNREAD` rows in the same response — no separate count endpoint needed.

**Gateway security:** this endpoint is accessible to any authenticated user (student, librarian, admin) — each sees only their own data because the filter is on `studentId` from the JWT, not from the request body.

**New repository method:** `Page<NotificationLog> findByStudentIdAndChannel(String studentId, NotificationChannel channel, Pageable pageable)` with an optional `status` filter.

---

## Shared pieces to implement

- `NotificationEventType` — add `BOOK_BORROWED` to the enum.
- `NotificationController` — add:
  - `PATCH /api/notifications/{id}/read` (scenario d)
  - `GET /api/notifications/me` (scenario e)
  - existing `POST /api/notifications` already covers scenario a (book borrowed) once the policy is seeded.
- `MarkNotificationReadUseCase` — load log → verify ownership → `NotificationAggregate.markRead` → persist.
- `NotificationCreateRequest` / `NotificationResponse` DTOs — `NotificationResponse` needs `readAt` surfaced.
- `CreateNewNotificationUseCase` — orchestrates: load policy → `NotificationAggregate` → persist log(s) → send email → mark sent/failed.
- `NotificationRepository` (+ JPA adapter) — needs:
  - `existsBy...` for dedup (scenarios b, c).
  - `Page<NotificationLog> findByStudentIdAndChannel(...)` for scenario e.
- `LoanServiceClient` — OpenFeign client to loan-service for `getLoansDueOn(date)` and `getOverdueLoans()`.
- `MailSenderService` wrapping `JavaMailSender`, rendering `subjectTemplate` / `bodyTemplate` with template variables.
- Two `@Scheduled` job beans (`DueDateReminderJob`, `OverdueReminderJob`) with cron expressions externalized to config-server.
- **DB migration** — add `BOOK_BORROWED` policy row to the seed migration (or a new `V_next` migration if the table already exists in production).
