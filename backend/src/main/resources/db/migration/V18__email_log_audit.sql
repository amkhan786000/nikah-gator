-- The e-mail log becomes the masjid's permanent record of everything the app sent.
-- Append-only: rows are never updated or deleted by the app, and there are deliberately NO foreign keys,
-- so entries survive the deletion of users, masjids or documents. Codes and passwords are never stored.
ALTER TABLE email_log
    ADD COLUMN masjid_id        BIGINT       NULL,
    ADD COLUMN sent_by_user_id  BIGINT       NULL,
    ADD COLUMN sent_by_name     VARCHAR(160) NULL,
    ADD COLUMN kind             VARCHAR(30)  NOT NULL DEFAULT 'OTHER',
    ADD COLUMN related_type     VARCHAR(40)  NULL,
    ADD COLUMN related_id       BIGINT       NULL,
    ADD COLUMN attachment_name  VARCHAR(190) NULL,
    ADD COLUMN status           VARCHAR(10)  NOT NULL DEFAULT 'SENT',
    ADD INDEX idx_email_masjid_created (masjid_id, created_at),
    ADD INDEX idx_email_recipient (recipient);

UPDATE email_log SET status = IF(success, 'SENT', 'FAILED');
UPDATE email_log SET kind = 'OTP' WHERE subject LIKE '%sign-in code%';
UPDATE email_log SET kind = 'PASSWORD' WHERE subject LIKE '%password%' AND subject NOT LIKE 'Your Masjidly account for%';
UPDATE email_log SET kind = 'WELCOME' WHERE subject LIKE 'Your Masjidly account for%' OR subject LIKE '%administrator account%';
