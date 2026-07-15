-- Manual reference script for PostgreSQL (notification_service database).
-- Not run automatically: the app uses spring.jpa.hibernate.ddl-auto=update,
-- so Hibernate creates/updates these tables itself on startup.
-- Use this only for a from-scratch manual setup or to reset mock data.
-- Run against the notification_service database (create it first if needed,
-- e.g. `CREATE DATABASE notification_service;` as a superuser).

DROP TABLE IF EXISTS notification_log;
DROP TABLE IF EXISTS notification_policy;

CREATE TABLE notification_policy (
    id               SERIAL PRIMARY KEY,
    event_type       VARCHAR(255) NOT NULL,
    subject_template VARCHAR(255) NOT NULL,
    body_template    VARCHAR(255) NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    create_at        TIMESTAMP    NOT NULL,
    update_at        TIMESTAMP
);

CREATE TABLE notification_log (
    id              SERIAL PRIMARY KEY,
    template_id     INT          NOT NULL,
    student_id      INT          NOT NULL,
    student_email   VARCHAR(255),
    channel         VARCHAR(50)  NOT NULL,
    status          VARCHAR(50)  NOT NULL,
    sent_at         TIMESTAMP,
    read_at         TIMESTAMP,
    failure_reason  VARCHAR(500),
    create_at       TIMESTAMP    NOT NULL,
    update_at       TIMESTAMP,
    CONSTRAINT fk_notification_log_template
        FOREIGN KEY (template_id) REFERENCES notification_policy (id)
);

-- event_type values come from NotificationEventType: DUE_SOON, OVERDUE_REMINDER, RETURN_CONFIRMATION
-- channel values come from NotificationChannel: EMAIL, WEBSITE
-- status values come from NotificationStatus: PENDING, SENT, FAILED, UNREAD, READ

INSERT INTO notification_policy (id, event_type, subject_template, body_template, is_active, create_at, update_at) VALUES
(1, 'DUE_SOON',          'Your loan is due soon',         'due_soon_email.html',          TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(2, 'DUE_SOON',          'Reminder: item due tomorrow',   'due_soon_website.html',        TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(3, 'OVERDUE_REMINDER',  'Your loan is overdue',          'overdue_email.html',           TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(4, 'OVERDUE_REMINDER',  'Overdue notice on your account','overdue_website.html',         TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(5, 'RETURN_CONFIRMATION','Thanks for returning your item','return_confirmation_email.html', TRUE,  '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(6, 'RETURN_CONFIRMATION','Return confirmed',              'return_confirmation_website.html', FALSE, '2026-06-01 08:00:00', '2026-06-15 10:00:00');

INSERT INTO notification_log (id, template_id, student_id, student_email, channel, status, sent_at, read_at, failure_reason, create_at, update_at) VALUES
(1,  1, 101, 'alice.nguyen@fpt.edu.vn',  'EMAIL',   'SENT',    '2026-07-08 07:00:05', NULL,                  NULL,                              '2026-07-08 07:00:00', '2026-07-08 07:00:05'),
(2,  2, 101, NULL,                       'WEBSITE', 'READ',    '2026-07-08 07:00:05', '2026-07-08 09:12:31', NULL,                              '2026-07-08 07:00:00', '2026-07-08 09:12:31'),
(3,  1, 102, 'binh.tran@fpt.edu.vn',     'EMAIL',   'SENT',    '2026-07-08 07:00:06', NULL,                  NULL,                              '2026-07-08 07:00:00', '2026-07-08 07:00:06'),
(4,  1, 103, 'chi.le@fpt.edu.vn',        'EMAIL',   'FAILED',  NULL,                  NULL,                  'SMTP timeout',                    '2026-07-08 07:00:00', '2026-07-08 07:00:10'),
(5,  2, 104, NULL,                       'WEBSITE', 'UNREAD',  '2026-07-08 07:00:07', NULL,                  NULL,                              '2026-07-08 07:00:00', '2026-07-08 07:00:07'),
(6,  3, 105, 'duy.pham@fpt.edu.vn',      'EMAIL',   'SENT',    '2026-07-09 08:00:04', NULL,                  NULL,                              '2026-07-09 08:00:00', '2026-07-09 08:00:04'),
(7,  4, 105, NULL,                       'WEBSITE', 'READ',    '2026-07-09 08:00:04', '2026-07-09 08:45:00', NULL,                              '2026-07-09 08:00:00', '2026-07-09 08:45:00'),
(8,  3, 106, 'em.hoang@fpt.edu.vn',      'EMAIL',   'PENDING', NULL,                  NULL,                  NULL,                              '2026-07-11 08:00:00', '2026-07-11 08:00:00'),
(9,  4, 107, NULL,                       'WEBSITE', 'UNREAD',  '2026-07-11 08:00:03', NULL,                  NULL,                              '2026-07-11 08:00:00', '2026-07-11 08:00:03'),
(10, 3, 108, 'giang.vu@fpt.edu.vn',      'EMAIL',   'FAILED',  NULL,                  NULL,                  'Invalid recipient address',       '2026-07-11 08:00:00', '2026-07-11 08:00:09'),
(11, 5, 101, 'alice.nguyen@fpt.edu.vn',  'EMAIL',   'SENT',    '2026-07-10 14:30:00', NULL,                  NULL,                              '2026-07-10 14:30:00', '2026-07-10 14:30:00'),
(12, 5, 102, 'binh.tran@fpt.edu.vn',     'EMAIL',   'SENT',    '2026-07-10 15:05:00', NULL,                  NULL,                              '2026-07-10 15:05:00', '2026-07-10 15:05:00'),
(13, 1, 109, 'huy.do@fpt.edu.vn',        'EMAIL',   'PENDING', NULL,                  NULL,                  NULL,                              '2026-07-11 08:00:00', '2026-07-11 08:00:00'),
(14, 2, 110, NULL,                       'WEBSITE', 'UNREAD',  '2026-07-11 08:00:08', NULL,                  NULL,                              '2026-07-11 08:00:00', '2026-07-11 08:00:08'),
(15, 3, 111, 'khanh.mai@fpt.edu.vn',     'EMAIL',   'SENT',    '2026-07-09 08:00:05', NULL,                  NULL,                              '2026-07-09 08:00:00', '2026-07-09 08:00:05');

SELECT setval(pg_get_serial_sequence('notification_policy', 'id'), (SELECT MAX(id) FROM notification_policy));
SELECT setval(pg_get_serial_sequence('notification_log', 'id'), (SELECT MAX(id) FROM notification_log));
