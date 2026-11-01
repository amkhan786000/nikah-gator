-- Who changed it, on every business table: created_by / updated_by hold app_users ids (no foreign key, so a
-- removed user never blocks anything; NULL = a system job or a public form). created_at / updated_at are added
-- where a table lacked them. The application fills created_by/updated_by (org.nikahgator.audit.Auditable);
-- the database fills the timestamps.
--
-- activity_log: the append-only record of every change and sign-in. Deliberately NO foreign keys, so an entry
-- survives the deletion of the record, the user or the masjid it is about. Rows are only ever inserted.

ALTER TABLE masjids
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE app_users
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE nikah_requests
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE checklist_items
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE documents
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE certificates
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE payments
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE notifications
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE nikah_fees
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL;

ALTER TABLE certificate_templates
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE masjid_services
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE imam_services
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE juma_letters
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE community_service_certs
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE life_events
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE counseling_sessions
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE hall_bookings
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE divorce_records
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE letters
    ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE staff_permissions
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL;

CREATE TABLE activity_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id      BIGINT       NULL,
    user_id        BIGINT       NULL,
    user_name      VARCHAR(160) NULL,     -- snapshots: still readable after the user is renamed or removed
    user_role      VARCHAR(30)  NULL,
    action         VARCHAR(20)  NOT NULL, -- CREATE UPDATE DELETE VOID LOGIN LOGIN_FAILED LOGOUT VIEW
    entity_type    VARCHAR(60)  NULL,
    entity_id      BIGINT       NULL,
    summary        TEXT         NOT NULL,
    changed_fields MEDIUMTEXT   NULL,     -- JSON {"field":{"old":..,"new":..}}; secrets are never stored
    ip             VARCHAR(64)  NULL,
    user_agent     VARCHAR(300) NULL,
    created_at     TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_activity_masjid_time (masjid_id, created_at),
    INDEX idx_activity_entity (entity_type, entity_id),
    INDEX idx_activity_user (user_id, created_at)
);
