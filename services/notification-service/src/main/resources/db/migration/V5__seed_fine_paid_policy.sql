-- Fine Service calls POST /api/notifications with eventType=FINE_PAID once a
-- SePay webhook successfully marks a fine as paid. Requires an active policy
-- row to exist, or CreateNewNotificationUseCase throws IllegalStateException.
INSERT INTO notification_policy (event_type, subject_template, body_template, is_active, create_at, update_at)
SELECT 'FINE_PAID',
       'Fine payment received',
       'We have received your payment of {{amount}} VND for fine #{{fineId}} on {{paidAt}}. Thank you!',
       TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_policy WHERE event_type = 'FINE_PAID');
