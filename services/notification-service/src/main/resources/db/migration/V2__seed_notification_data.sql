-- student_id values are Keycloak user UUIDs (no cross-database FK by design).
-- Two sample users:
--   a91940da-c7e0-477a-ba59-ca34756ced99  -> phukak12345@gmail.com
--   3110c407-6ee6-4821-b3ed-d39d2ee88eb3  -> huynguyensteph@gmail.com

INSERT INTO notification_policy (id, event_type, subject_template, body_template, is_active, create_at, update_at) VALUES
(1, 'BOOK_BORROWED',      'Your loan is confirmed — {{bookTitle}}',            'You have successfully borrowed "{{bookTitle}}". Please return it by {{dueDate}}.', TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(2, 'BOOK_BORROWED',      'Loan confirmed: {{bookTitle}}',                     'Your borrow request for "{{bookTitle}}" is confirmed. Due date: {{dueDate}}.',      TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(3, 'DUE_SOON',           'Your loan is due soon',                             'Reminder: "{{bookTitle}}" is due on {{dueDate}}. Please return it on time.',        TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(4, 'DUE_SOON',           'Reminder: item due soon',                           'Your borrowed item "{{bookTitle}}" is due in 3 days ({{dueDate}}).',                TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(5, 'OVERDUE_REMINDER',   'Your loan is overdue',                              '"{{bookTitle}}" was due on {{dueDate}} and has not been returned. Fines may apply.', TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(6, 'OVERDUE_REMINDER',   'Overdue notice on your account',                    'You have an overdue item: "{{bookTitle}}" (due {{dueDate}}). Please return it.',    TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(7, 'RETURN_CONFIRMATION','Thanks for returning your item',                    'You have returned "{{bookTitle}}" on {{returnedAt}}. Thank you!',                   TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(8, 'RETURN_CONFIRMATION','Return confirmed',                                   'Your return of "{{bookTitle}}" has been confirmed on {{returnedAt}}.',              TRUE,  '2026-06-01 08:00:00', '2026-06-15 10:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO notification_log (id, template_id, student_id, student_email, channel, status, sent_at, read_at, failure_reason, create_at, update_at) VALUES
-- BOOK_BORROWED: email + in-app
(1,  1, 'a91940da-c7e0-477a-ba59-ca34756ced99', 'phukak12345@gmail.com',   'EMAIL',   'SENT',    '2026-07-08 07:00:05', NULL,                  NULL,                        '2026-07-08 07:00:00', '2026-07-08 07:00:05'),
(2,  2, 'a91940da-c7e0-477a-ba59-ca34756ced99', NULL,                      'WEBSITE', 'READ',    '2026-07-08 07:00:05', '2026-07-08 09:12:31', NULL,                        '2026-07-08 07:00:00', '2026-07-08 09:12:31'),
(3,  1, '3110c407-6ee6-4821-b3ed-d39d2ee88eb3', 'huynguyensteph@gmail.com','EMAIL',   'SENT',    '2026-07-08 07:00:06', NULL,                  NULL,                        '2026-07-08 07:00:00', '2026-07-08 07:00:06'),
(4,  2, '3110c407-6ee6-4821-b3ed-d39d2ee88eb3', NULL,                      'WEBSITE', 'UNREAD',  '2026-07-08 07:00:06', NULL,                  NULL,                        '2026-07-08 07:00:00', '2026-07-08 07:00:06'),
-- DUE_SOON: email + in-app
(5,  3, 'a91940da-c7e0-477a-ba59-ca34756ced99', 'phukak12345@gmail.com',   'EMAIL',   'FAILED',  NULL,                  NULL,                  'SMTP timeout',              '2026-07-08 07:00:00', '2026-07-08 07:00:10'),
(6,  4, 'a91940da-c7e0-477a-ba59-ca34756ced99', NULL,                      'WEBSITE', 'UNREAD',  '2026-07-08 07:00:10', NULL,                  NULL,                        '2026-07-08 07:00:00', '2026-07-08 07:00:10'),
-- OVERDUE_REMINDER: email + in-app
(7,  5, 'a91940da-c7e0-477a-ba59-ca34756ced99', 'phukak12345@gmail.com',   'EMAIL',   'SENT',    '2026-07-09 08:00:04', NULL,                  NULL,                        '2026-07-09 08:00:00', '2026-07-09 08:00:04'),
(8,  6, 'a91940da-c7e0-477a-ba59-ca34756ced99', NULL,                      'WEBSITE', 'READ',    '2026-07-09 08:00:04', '2026-07-09 08:45:00', NULL,                        '2026-07-09 08:00:00', '2026-07-09 08:45:00'),
(9,  5, '3110c407-6ee6-4821-b3ed-d39d2ee88eb3', 'huynguyensteph@gmail.com','EMAIL',   'PENDING', NULL,                  NULL,                  NULL,                        '2026-07-11 08:00:00', '2026-07-11 08:00:00'),
(10, 6, '3110c407-6ee6-4821-b3ed-d39d2ee88eb3', NULL,                      'WEBSITE', 'UNREAD',  '2026-07-11 08:00:03', NULL,                  NULL,                        '2026-07-11 08:00:00', '2026-07-11 08:00:03'),
-- RETURN_CONFIRMATION: email + in-app
(11, 7, 'a91940da-c7e0-477a-ba59-ca34756ced99', 'phukak12345@gmail.com',   'EMAIL',   'SENT',    '2026-07-10 14:30:00', NULL,                  NULL,                        '2026-07-10 14:30:00', '2026-07-10 14:30:00'),
(12, 8, 'a91940da-c7e0-477a-ba59-ca34756ced99', NULL,                      'WEBSITE', 'READ',    '2026-07-10 14:30:00', '2026-07-10 15:00:00', NULL,                        '2026-07-10 14:30:00', '2026-07-10 15:00:00'),
(13, 7, '3110c407-6ee6-4821-b3ed-d39d2ee88eb3', 'huynguyensteph@gmail.com','EMAIL',   'SENT',    '2026-07-10 15:05:00', NULL,                  NULL,                        '2026-07-10 15:05:00', '2026-07-10 15:05:00'),
(14, 8, '3110c407-6ee6-4821-b3ed-d39d2ee88eb3', NULL,                      'WEBSITE', 'UNREAD',  '2026-07-10 15:05:00', NULL,                  NULL,                        '2026-07-10 15:05:00', '2026-07-10 15:05:00')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE notification_policy ALTER COLUMN id RESTART WITH 9;
ALTER TABLE notification_log ALTER COLUMN id RESTART WITH 15;
