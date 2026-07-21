-- Adds a BORROWED loan for the Keycloak user a91940da-c7e0-477a-ba59-ca34756ced99
-- (phukak12345@gmail.com), reusing the same UUID seeded for this user in
-- notification-service's V2__seed_notification_data.sql. member_id has no
-- cross-database FK by design (service-owned reference, matching V2 above).
-- Uses catalog copy 1 (book 1, "Nhap mon Lap trinh Java"), which was still
-- AVAILABLE in catalog-service's V2 seed data.

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
    (7, 'a91940da-c7e0-477a-ba59-ca34756ced99', 1, 1, 'PHYSICAL', 'BORROWED',
     '2026-07-20 09:00:00', '2026-08-03 09:00:00', NULL,
     0, 3, 'seed-borrow-007', '2026-07-20 09:00:00', '2026-07-20 09:00:00', 0)
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
    (12, 7, NULL, 'BORROWED', 'SYSTEM', 'Loan created', '2026-07-20 09:00:00')
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
    (7, 7, 'BookBorrowedEvent', '{"loanId":7,"memberId":"a91940da-c7e0-477a-ba59-ca34756ced99","bookId":1,"copyId":1}',
     'PROCESSED', 0, '2026-07-20 09:00:00', '2026-07-20 09:00:05')
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
    (10, 7, 'BORROW_BOOK', 'LOAN_CREATED', 'COMPLETED', 0, '2026-07-20 08:59:58', '2026-07-20 09:00:01')
ON CONFLICT (saga_id) DO NOTHING;

ALTER TABLE loans ALTER COLUMN loan_id RESTART WITH 8;
ALTER TABLE loan_status_history ALTER COLUMN id RESTART WITH 13;
ALTER TABLE saga_outbox ALTER COLUMN event_id RESTART WITH 8;
ALTER TABLE saga_log ALTER COLUMN saga_id RESTART WITH 11;
