-- Seeds one PENDING fine for manual SePay payment testing.
-- Uses a sentinel loan_id (9000000001) unlikely to collide with real loans;
-- guarded with WHERE NOT EXISTS so re-running the stack doesn't duplicate it.

INSERT INTO fine_policy (daily_rate, is_active, lost_threshold_days, lost_penalty, create_at)
SELECT 5000, TRUE, 30, 100000, NOW()
WHERE NOT EXISTS (SELECT 1 FROM fine_policy WHERE is_active = TRUE);

INSERT INTO fines (
    policy_id, loan_id, book_id, student_id, reason,
    due_date, amount_due, compensation_amount, status, create_at, update_at
)
SELECT
    (SELECT id FROM fine_policy WHERE is_active = TRUE ORDER BY id LIMIT 1),
    9000000001,
    1,
    '3110c407-6ee6-4821-b3ed-d39d2ee88eb3',
    'OVERDUE_RETURN',
    NOW() - INTERVAL '3 days',
    5000,
    0,
    'PENDING',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM fines WHERE loan_id = 9000000001);
