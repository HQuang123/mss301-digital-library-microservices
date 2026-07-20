-- Sample loans aligned with Catalog Service migration data.
-- Catalog copy 2 (book 1) and copy 10 (book 7) are BORROWED.
-- Member identifiers are service-owned references and intentionally have no cross-database FK.

INSERT INTO loans (
    loan_id,
    member_id,
    book_id,
    copy_id,
    book_type,
    status,
    borrowed_at,
    due_date,
    returned_at,
    renewal_count,
    max_renewals,
    idempotency_key,
    created_at,
    updated_at,
    version
) VALUES
    (1, 'postman-student-001', 1, 2, 'PHYSICAL', 'BORROWED',
     '2026-07-15 09:00:00', '2026-07-29 09:00:00', NULL,
     0, 3, 'seed-borrow-001', '2026-07-15 09:00:00', '2026-07-15 09:00:00', 0),

    (2, 'sample-student-002', 7, 10, 'PHYSICAL', 'OVERDUE',
     '2026-06-20 10:30:00', '2026-07-04 10:30:00', NULL,
     0, 3, 'seed-borrow-002', '2026-06-20 10:30:00', '2026-07-05 00:05:00', 0),

    (3, 'postman-student-001', 2, 3, 'PHYSICAL', 'RETURNED',
     '2026-06-10 08:15:00', '2026-06-24 08:15:00', '2026-06-22 16:20:00',
     0, 3, 'seed-borrow-003', '2026-06-10 08:15:00', '2026-06-22 16:20:00', 0),

    (4, 'sample-lecturer-001', 5, 8, 'PHYSICAL', 'RETURNED',
     '2026-05-01 14:00:00', '2026-05-29 14:00:00', '2026-06-02 09:10:00',
     1, 3, 'seed-borrow-004', '2026-05-01 14:00:00', '2026-06-02 09:10:00', 0),

    (5, 'sample-lecturer-001', 3, NULL, 'DIGITAL', 'BORROWED',
     '2026-07-12 13:45:00', '2026-08-09 13:45:00', NULL,
     0, 3, 'seed-borrow-005', '2026-07-12 13:45:00', '2026-07-12 13:45:00', 0),

    (6, 'sample-student-003', 6, NULL, 'DIGITAL', 'RETURNED',
     '2026-04-10 11:00:00', '2026-04-24 11:00:00', '2026-04-20 18:00:00',
     0, 3, 'seed-borrow-006', '2026-04-10 11:00:00', '2026-04-20 18:00:00', 0)
ON CONFLICT (loan_id) DO NOTHING;

INSERT INTO loan_status_history (
    id,
    loan_id,
    from_status,
    to_status,
    changed_by,
    reason,
    changed_at
) VALUES
    (1, 1, NULL, 'BORROWED', 'SYSTEM', 'Loan created', '2026-07-15 09:00:00'),
    (2, 2, NULL, 'BORROWED', 'SYSTEM', 'Loan created', '2026-06-20 10:30:00'),
    (3, 2, 'BORROWED', 'OVERDUE', 'SYSTEM', 'Due date exceeded', '2026-07-05 00:05:00'),
    (4, 3, NULL, 'BORROWED', 'SYSTEM', 'Loan created', '2026-06-10 08:15:00'),
    (5, 3, 'BORROWED', 'RETURNED', 'LIBRARIAN', 'Book returned', '2026-06-22 16:20:00'),
    (6, 4, NULL, 'BORROWED', 'SYSTEM', 'Loan created', '2026-05-01 14:00:00'),
    (7, 4, 'BORROWED', 'BORROWED', 'sample-lecturer-001', 'Loan renewed', '2026-05-20 09:00:00'),
    (8, 4, 'BORROWED', 'RETURNED', 'LIBRARIAN', 'Book returned late', '2026-06-02 09:10:00'),
    (9, 5, NULL, 'BORROWED', 'SYSTEM', 'Digital loan created', '2026-07-12 13:45:00'),
    (10, 6, NULL, 'BORROWED', 'SYSTEM', 'Digital loan created', '2026-04-10 11:00:00'),
    (11, 6, 'BORROWED', 'RETURNED', 'sample-student-003', 'Digital loan completed', '2026-04-20 18:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO saga_outbox (
    event_id,
    loan_id,
    event_type,
    payload,
    status,
    retry_count,
    created_at,
    processed_at
) VALUES
    (1, 1, 'BookBorrowedEvent', '{"loanId":1,"memberId":"postman-student-001","bookId":1,"copyId":2}',
     'PROCESSED', 0, '2026-07-15 09:00:00', '2026-07-15 09:00:05'),
    (2, 2, 'LoanOverdueEvent', '{"loanId":2,"memberId":"sample-student-002","bookId":7,"copyId":10}',
     'PENDING', 0, '2026-07-05 00:05:00', NULL),
    (3, 3, 'BookReturnedEvent', '{"loanId":3,"memberId":"postman-student-001","bookId":2,"copyId":3}',
     'PROCESSED', 0, '2026-06-22 16:20:00', '2026-06-22 16:20:04'),
    (4, 4, 'LoanRenewedEvent', '{"loanId":4,"memberId":"sample-lecturer-001","renewalCount":1}',
     'PROCESSED', 0, '2026-05-20 09:00:00', '2026-05-20 09:00:03'),
    (5, 4, 'BookReturnedLateEvent', '{"loanId":4,"memberId":"sample-lecturer-001","bookId":5,"copyId":8}',
     'PROCESSED', 0, '2026-06-02 09:10:00', '2026-06-02 09:10:05'),
    (6, 5, 'BookBorrowedEvent', '{"loanId":5,"memberId":"sample-lecturer-001","bookId":3,"bookType":"DIGITAL"}',
     'PROCESSING', 1, '2026-07-12 13:45:00', NULL)
ON CONFLICT (event_id) DO NOTHING;

INSERT INTO saga_log (
    saga_id,
    loan_id,
    saga_type,
    current_step,
    overall_status,
    compensations,
    started_at,
    finished_at
) VALUES
    (1, 1, 'BORROW_BOOK', 'LOAN_CREATED', 'COMPLETED', 0, '2026-07-15 08:59:58', '2026-07-15 09:00:01'),
    (2, 2, 'BORROW_BOOK', 'LOAN_CREATED', 'COMPLETED', 0, '2026-06-20 10:29:58', '2026-06-20 10:30:01'),
    (3, 3, 'BORROW_BOOK', 'LOAN_CREATED', 'COMPLETED', 0, '2026-06-10 08:14:58', '2026-06-10 08:15:01'),
    (4, 3, 'RETURN_BOOK', 'COPY_RELEASED', 'COMPLETED', 0, '2026-06-22 16:19:58', '2026-06-22 16:20:01'),
    (5, 4, 'BORROW_BOOK', 'LOAN_CREATED', 'COMPLETED', 0, '2026-05-01 13:59:58', '2026-05-01 14:00:01'),
    (6, 4, 'RENEW_BOOK', 'DUE_DATE_UPDATED', 'COMPLETED', 0, '2026-05-20 08:59:58', '2026-05-20 09:00:01'),
    (7, 4, 'RETURN_BOOK', 'COPY_RELEASED', 'COMPLETED', 0, '2026-06-02 09:09:58', '2026-06-02 09:10:01'),
    (8, 5, 'BORROW_BOOK', 'DIGITAL_ACCESS_GRANTED', 'COMPLETED', 0, '2026-07-12 13:44:58', '2026-07-12 13:45:01'),
    (9, 6, 'RETURN_BOOK', 'DIGITAL_ACCESS_CLOSED', 'COMPLETED', 0, '2026-04-20 17:59:58', '2026-04-20 18:00:01')
ON CONFLICT (saga_id) DO NOTHING;

INSERT INTO reservations (
    reservation_id,
    member_id,
    book_id,
    status,
    queue_position,
    reserved_at,
    notified_at,
    expires_at
) VALUES
    (1, 1001, 1, 'WAITING', 1, '2026-07-16 08:00:00', NULL, NULL),
    (2, 1002, 7, 'NOTIFIED', 1, '2026-07-10 10:00:00', '2026-07-17 09:00:00', '2026-07-19 09:00:00'),
    (3, 1003, 3, 'COMPLETED', 1, '2026-06-01 14:00:00', '2026-06-05 08:30:00', '2026-06-07 08:30:00')
ON CONFLICT (reservation_id) DO NOTHING;

ALTER TABLE loans ALTER COLUMN loan_id RESTART WITH 7;
ALTER TABLE loan_status_history ALTER COLUMN id RESTART WITH 12;
ALTER TABLE saga_outbox ALTER COLUMN event_id RESTART WITH 7;
ALTER TABLE saga_log ALTER COLUMN saga_id RESTART WITH 10;
ALTER TABLE reservations ALTER COLUMN reservation_id RESTART WITH 4;
