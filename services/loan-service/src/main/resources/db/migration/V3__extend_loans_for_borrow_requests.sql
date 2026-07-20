-- A borrow request is represented by the existing Loan aggregate.
-- PENDING rows do not reserve a copy and do not start the borrowing period.

ALTER TABLE loans ALTER COLUMN borrowed_at DROP NOT NULL;
ALTER TABLE loans ALTER COLUMN due_date DROP NOT NULL;

ALTER TABLE loans ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE loans ADD COLUMN reviewed_by VARCHAR(255);
ALTER TABLE loans ADD COLUMN rejection_reason VARCHAR(500);

ALTER TABLE loans DROP CONSTRAINT ck_loans_status;
ALTER TABLE loans DROP CONSTRAINT ck_loans_due_date;
ALTER TABLE loans DROP CONSTRAINT ck_loans_physical_copy;
ALTER TABLE loan_status_history DROP CONSTRAINT ck_loan_history_from_status;
ALTER TABLE loan_status_history DROP CONSTRAINT ck_loan_history_to_status;

ALTER TABLE loans ADD CONSTRAINT ck_loans_status
    CHECK (status IN (
        'PENDING', 'BORROWED', 'RETURNED', 'OVERDUE', 'LOST', 'REJECTED', 'CANCELLED'
    ));

ALTER TABLE loans ADD CONSTRAINT ck_loans_borrow_period
    CHECK (
        (status IN ('PENDING', 'REJECTED', 'CANCELLED')
            AND borrowed_at IS NULL
            AND due_date IS NULL)
        OR
        (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'LOST')
            AND borrowed_at IS NOT NULL
            AND due_date IS NOT NULL
            AND due_date >= borrowed_at)
    );

ALTER TABLE loans ADD CONSTRAINT ck_loans_physical_copy
    CHECK (
        status IN ('PENDING', 'REJECTED', 'CANCELLED')
        OR book_type <> 'PHYSICAL'
        OR copy_id IS NOT NULL
    );

ALTER TABLE loan_status_history ADD CONSTRAINT ck_loan_history_from_status
    CHECK (
        from_status IS NULL OR from_status IN (
            'PENDING', 'BORROWED', 'RETURNED', 'OVERDUE', 'LOST', 'REJECTED', 'CANCELLED'
        )
    );

ALTER TABLE loan_status_history ADD CONSTRAINT ck_loan_history_to_status
    CHECK (to_status IN (
        'PENDING', 'BORROWED', 'RETURNED', 'OVERDUE', 'LOST', 'REJECTED', 'CANCELLED'
    ));

CREATE UNIQUE INDEX uk_loans_pending_member_book
    ON loans (member_id, book_id)
    WHERE status = 'PENDING';
