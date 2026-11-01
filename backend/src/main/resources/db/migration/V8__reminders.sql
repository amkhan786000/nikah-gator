-- Reminder e-mails: the notifications table gets a kind, retry bookkeeping, and masjids get reminder settings.
ALTER TABLE notifications
    ADD COLUMN kind       VARCHAR(30)  NULL AFTER request_id,         -- CONFIRMATION, COUPLE_7D, COUPLE_1D, IMAM_1D ...
    ADD COLUMN error      VARCHAR(500) NULL,
    ADD COLUMN attempts   INT          NOT NULL DEFAULT 0,
    ADD COLUMN claimed_at TIMESTAMP    NULL,
    ADD COLUMN created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD INDEX idx_notif_request (request_id, masjid_id);
-- status: PENDING, SENDING, SENT, FAILED, CANCELLED. send_at / sent_at / claimed_at are UTC.

ALTER TABLE masjids
    ADD COLUMN reminders_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN reminder_days_before  INT     NOT NULL DEFAULT 7;
