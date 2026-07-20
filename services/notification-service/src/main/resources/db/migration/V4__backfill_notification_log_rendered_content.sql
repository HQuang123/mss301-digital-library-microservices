-- Backfills title/message for notification_log rows seeded in V2, before rendered
-- content was captured at creation time. No per-row templateVariables were ever
-- stored for these rows, so the substituted values below (book titles, dates) are
-- reconstructed/made up to match the V2 seed data rather than pulled from real events.
UPDATE notification_log nl
SET title = v.title,
    message = v.message
FROM (VALUES
    (1,  'Your loan is confirmed — Clean Code',
         'You have successfully borrowed "Clean Code". Please return it by 2026-07-22.'),
    (2,  'Loan confirmed: Clean Code',
         'Your borrow request for "Clean Code" is confirmed. Due date: 2026-07-22.'),
    (3,  'Your loan is confirmed — The Pragmatic Programmer',
         'You have successfully borrowed "The Pragmatic Programmer". Please return it by 2026-07-22.'),
    (4,  'Loan confirmed: The Pragmatic Programmer',
         'Your borrow request for "The Pragmatic Programmer" is confirmed. Due date: 2026-07-22.'),
    (5,  'Your loan is due soon',
         'Reminder: "Clean Code" is due on 2026-07-11. Please return it on time.'),
    (6,  'Reminder: item due soon',
         'Your borrowed item "Clean Code" is due in 3 days (2026-07-11).'),
    (7,  'Your loan is overdue',
         '"Clean Code" was due on 2026-07-08 and has not been returned. Fines may apply.'),
    (8,  'Overdue notice on your account',
         'You have an overdue item: "Clean Code" (due 2026-07-08). Please return it.'),
    (9,  'Your loan is overdue',
         '"The Pragmatic Programmer" was due on 2026-07-10 and has not been returned. Fines may apply.'),
    (10, 'Overdue notice on your account',
         'You have an overdue item: "The Pragmatic Programmer" (due 2026-07-10). Please return it.'),
    (11, 'Thanks for returning your item',
         'You have returned "Clean Code" on 2026-07-10 14:30:00. Thank you!'),
    (12, 'Return confirmed',
         'Your return of "Clean Code" has been confirmed on 2026-07-10 14:30:00.'),
    (13, 'Thanks for returning your item',
         'You have returned "The Pragmatic Programmer" on 2026-07-10 15:05:00. Thank you!'),
    (14, 'Return confirmed',
         'Your return of "The Pragmatic Programmer" has been confirmed on 2026-07-10 15:05:00.')
) AS v(id, title, message)
WHERE nl.id = v.id
  AND nl.title IS NULL
  AND nl.message IS NULL;
