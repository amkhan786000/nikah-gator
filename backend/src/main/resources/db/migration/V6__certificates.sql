-- Certificates: void/re-issue support and a race-safe per masjid per year sequence.
ALTER TABLE certificates
    ADD COLUMN status      VARCHAR(10)  NOT NULL DEFAULT 'VALID', -- VALID, VOID
    ADD COLUMN void_reason VARCHAR(500) NULL,
    ADD COLUMN voided_at   TIMESTAMP    NULL,
    ADD COLUMN voided_by   BIGINT       NULL,
    ADD COLUMN valid_request_id BIGINT GENERATED ALWAYS AS (IF(status = 'VALID', request_id, NULL)) STORED;

-- A nikah may have many certificates over time (after a void) but only one valid at a time.
ALTER TABLE certificates ADD INDEX idx_cert_request (request_id);
ALTER TABLE certificates DROP INDEX request_id;
ALTER TABLE certificates ADD UNIQUE KEY uq_cert_valid_request (valid_request_id);
ALTER TABLE certificates ADD INDEX idx_cert_masjid (masjid_id, issued_at);

CREATE TABLE certificate_sequences (
    masjid_id BIGINT NOT NULL,
    cert_year INT    NOT NULL,
    last_seq  INT    NOT NULL,
    PRIMARY KEY (masjid_id, cert_year)
);
