# Fine Service

Microservice responsible for calculating, creating, and managing library fines within the MSS301 Digital Library system.

---

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Domain Model](#domain-model)
- [Business Logic](#business-logic)
- [API Endpoints](#api-endpoints)
- [Configuration](#configuration)
- [Running the Service](#running-the-service)
- [Project Structure](#project-structure)

---

## Overview

`fine-service` handles the lifecycle of fines issued to students who return borrowed books late or lose them. It enforces fine policies (daily overdue rate, lost-book penalty threshold) and tracks the payment status of each fine.

The service follows **Domain-Driven Design (DDD)** with a clean separation between the domain layer, application use-cases, and infrastructure adapters.

---

## Technology Stack

| Concern              | Technology                                    |
|----------------------|-----------------------------------------------|
| Language             | Java 21                                       |
| Framework            | Spring Boot (Web, Data JPA, Validation)       |
| Database             | MySQL                                         |
| Service Discovery    | Spring Cloud Netflix Eureka (client)          |
| Inter-service calls  | Spring Cloud OpenFeign                        |
| API Documentation    | SpringDoc OpenAPI 3 (Swagger UI) `3.0.2`      |
| Boilerplate          | Lombok                                        |
| Build                | Maven (inherits `digilib-parent` POM)         |

---

## Architecture

The service is structured as a layered DDD application:

```
fine-service/
├── api/                          # Inbound HTTP layer
│   ├── controller/               # REST controllers
│   └── dto/                      # Request / response DTOs
├── application/                  # Application layer
│   ├── command/                  # Command objects
│   └── usecase/                  # Use-case interactors
├── config/                       # Spring configuration beans
├── domain/                       # Core domain (no framework deps)
│   ├── aggregate/                # Aggregate roots
│   ├── entity/                   # JPA entities (also domain entities)
│   ├── repository/               # Repository interfaces (port)
│   └── vo/                       # Value objects
└── infrastructure/               # Outbound adapters
    ├── adapter/                  # Repository adapter (implements domain ports)
    ├── persistence/              # Spring Data JPA repositories
    └── specification/            # Query / search criteria
```

> Most layers outside the domain are stubs pending implementation.

---

## Domain Model

### Entities

#### `Fine` (`fines` table)

Represents a single fine issued for a loan.

| Column          | Type              | Notes                                      |
|-----------------|-------------------|--------------------------------------------|
| `id`            | `Integer` (PK)    | Auto-generated                             |
| `policy_id`     | `Integer` (FK)    | References `FinePolicy`; `LAZY` fetch      |
| `loan_id`       | `Integer`         | ID of the loan that triggered the fine     |
| `student_id`    | `Integer`         | ID of the student                          |
| `student_email` | `VARCHAR(100)`    | Email of the student                       |
| `due_date`      | `DATETIME`        | Original book return deadline              |
| `return_date`   | `DATETIME`        | Actual return date (nullable if not yet returned) |
| `amount_due`    | `DOUBLE`          | Calculated fine amount                     |
| `status`        | `VARCHAR(50)`     | `PENDING` / `PAID` / `WAIVED`             |
| `waiver_reason` | `VARCHAR(255)`    | Populated only when status is `WAIVED`     |
| `create_at`     | `DATETIME`        | Set on insert via `@PrePersist`            |
| `update_at`     | `DATETIME`        | Updated on every change via `@PreUpdate`   |

#### `FinePolicy` (`fine_policy` table)

Defines the rules used to calculate fines. Multiple fines can reference the same policy.

| Column                | Type       | Notes                                               |
|-----------------------|------------|-----------------------------------------------------|
| `id`                  | `Integer`  | Auto-generated                                      |
| `daily_rate`          | `DOUBLE`   | Fee charged per overdue day                         |
| `is_active`           | `Boolean`  | Only active policies can be used; defaults to `true`|
| `lost_threshold_days` | `Integer`  | Days overdue before the book is considered lost     |
| `lost_penalty`        | `DOUBLE`   | Extra flat fee added when a book is considered lost |
| `create_at`           | `DATETIME` | Set on insert                                       |
| `update_at`           | `DATETIME` | Updated on every change                             |

---

### Aggregate Root

#### `FineAggregate`

Wraps a `FinePolicy` and encapsulates all business operations. Constructed via the factory method:

```java
FineAggregate aggregate = FineAggregate.create(policy);
```

**Validation on creation:**
- `policy` must not be null and must be active (`isActive == true`).
- `dailyRate` and `lostPenalty` must be ≥ 0.
- `lostThresholdDays` must be > 0.

**Operations:**

| Method | Description |
|---|---|
| `createFineFor(loanId, studentId, studentEmail, dueDate, returnDate)` | Creates a new `Fine` with `PENDING` status and calculated amount. |
| `createFineFor(loanId, studentId, studentEmail, dueDate)` | Same as above with `returnDate = null` (book not yet returned). |
| `recalculate(fine, returnDate)` | Updates `returnDate` and recalculates `amountDue` when a book is returned. |
| `markPaid(fine)` | Transitions status to `PAID`, clears `waiverReason`. |
| `waive(fine, waiverReason)` | Transitions status to `WAIVED`, stores reason. |

All mutating operations validate that the `Fine` belongs to the same policy as this aggregate.

---

### Value Objects

#### `FineRange`

Immutable object representing the interval between `dueDate` and `returnDate`.

| Method | Description |
|---|---|
| `overdueDays()` | Days past due. Uses current timestamp if `returnDate` is null. Returns `0` if not yet overdue. |
| `isOverdue()` | `true` if `overdueDays() > 0`. |
| `isLost(lostThresholdDays)` | `true` if `overdueDays() >= lostThresholdDays`. |

**Constraint:** `returnDate` must not be before `dueDate`.

#### `FineStatus`

```java
enum FineStatus { PENDING, PAID, WAIVED }
```

---

## Business Logic

### Fine Amount Calculation

```
amountDue = overdueDays × dailyRate
           + lostPenalty  (only if overdueDays ≥ lostThresholdDays)
```

- `overdueDays` is calculated via `FineRange.overdueDays()` using day-level granularity (`ChronoUnit.DAYS`).
- If the book has not been returned (`returnDate == null`), the current date is used to compute overdue days.

### State Transitions

```
          createFineFor()
               │
               ▼
           PENDING
          /       \
  markPaid()     waive(reason)
       │               │
       ▼               ▼
     PAID           WAIVED
```

---

## API Endpoints

> The REST layer (`FineController`) is a stub pending implementation.

Swagger UI will be available at:

```
http://localhost:<port>/swagger-ui.html
```

---

## Configuration

`src/main/resources/application.properties`:

```properties
spring.application.name=fine-service
```

Full runtime configuration (datasource URL, Eureka server address, server port) is expected to be supplied by the **config-server** in the microservices setup.

---

## Running the Service

Requires:
- Java 21+
- MySQL instance (connection details from config-server)
- Eureka server running

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

Or use the project-level `.bat` launcher that starts all services sequentially.

---

## Project Structure

```
src/main/java/fu/edu/mss301/digilib/fine/
├── FineServiceApplication.java                          # Entry point
├── api/
│   ├── controller/FineController.java                   # (stub)
│   └── dto/
│       ├── FineCreateRequest.java                       # (stub)
│       └── FineResponse.java                            # (stub)
├── application/
│   ├── command/FineCommand.java                         # (stub)
│   └── usecase/CreateNewFineUseCase.java                # (stub)
├── config/
│   └── HttpClientConfig.java                            # (stub)
├── domain/
│   ├── aggregate/FineAggregate.java                     # Core business logic
│   ├── entity/
│   │   ├── Fine.java                                    # JPA entity
│   │   └── FinePolicy.java                              # JPA entity
│   ├── repository/FineRepository.java                   # Domain port (stub)
│   └── vo/
│       ├── FineRange.java                               # Value object
│       └── FineStatus.java                              # Enum
└── infrastructure/
    ├── adapter/FineRepositoryAdapter.java               # (stub)
    ├── persistence/FineJpaRepository.java               # (stub)
    └── specification/FineSearchCriteria.java            # (stub)
```
