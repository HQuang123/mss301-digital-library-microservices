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

| Event type            | Scenario                                   |
|------------------------|---------------------------------------------|
| `DUE_SOON`             | 3 days before due date                      |
| `OVERDUE_REMINDER`     | daily reminder while a loan is overdue       |
| `RETURN_CONFIRMATION`  | book returned by student                    |

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

## a. Reminder — 3 days before due date

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

## Shared pieces to implement

- `NotificationController` — REST endpoints for (c) and any manual/admin trigger of (a)/(b) for testing.
- `NotificationCreateRequest` / `NotificationResponse` DTOs.
- `CreateNewNotificationUseCase` — orchestrates: load policy → `NotificationAggregate` → persist log(s) → send email → mark sent/failed.
- `NotificationRepository` (+ JPA adapter, already scaffolded as `NotificationRepositoryAdapter` / `NotificationJpaRepository`) — needs `existsBy...` methods for dedup checks in (a)/(b).
- `LoanServiceClient` — OpenFeign client to loan-service for `getLoansDueOn(date)` and `getOverdueLoans()` (pom.xml already includes `spring-cloud-starter-openfeign` + Eureka client).
- `MailSenderService` wrapping `JavaMailSender`, rendering `subjectTemplate` / `bodyTemplate` with loan/student variables.
- Two `@Scheduled` job beans (`DueDateReminderJob`, `OverdueReminderJob`) with cron expressions externalized to config (config-server `application.yml`), so schedules can change without a redeploy.
