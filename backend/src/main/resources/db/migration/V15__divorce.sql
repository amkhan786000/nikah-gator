-- Divorce records (religious record; not a civil decree) and their per-masjid, per-year number counter.
CREATE TABLE divorce_records (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id            BIGINT       NOT NULL,
    nikah_id             BIGINT       NULL,     -- set when the nikah was recorded in this app
    record_number        VARCHAR(40)  NOT NULL,
    divorce_type         VARCHAR(10)  NOT NULL, -- TALAQ, KHULA, MUTUAL, FASKH
    status               VARCHAR(10)  NOT NULL DEFAULT 'RECORDED', -- RECORDED, REVOKED
    groom_name           VARCHAR(200) NOT NULL,
    bride_name           VARCHAR(200) NOT NULL,
    groom_father_name    VARCHAR(200) NULL,
    bride_father_name    VARCHAR(200) NULL,
    marriage_date        DATE         NULL,
    marriage_place       VARCHAR(300) NULL,
    divorce_date         DATE         NOT NULL,
    talaq_number         INT          NULL,    -- 1, 2 or 3 (TALAQ only)
    iddah_end_date       DATE         NULL,
    revoked_date         DATE         NULL,
    settlement_notes     VARCHAR(2000) NULL,
    reconciliation_notes VARCHAR(2000) NULL,
    witness1_name        VARCHAR(200) NULL,
    witness2_name        VARCHAR(200) NULL,
    private_notes        VARCHAR(2000) NULL,
    imam_name            VARCHAR(160) NULL,
    voided               BOOLEAN      NOT NULL DEFAULT FALSE,
    void_reason          VARCHAR(500) NULL,
    voided_at            TIMESTAMP    NULL,
    voided_by            BIGINT       NULL,
    created_by           BIGINT       NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- at most one non-void divorce per nikah (NULLs never collide)
    active_nikah_id      BIGINT GENERATED ALWAYS AS (IF(voided = 0, nikah_id, NULL)) STORED,
    CONSTRAINT uq_divorce_number UNIQUE (record_number),
    CONSTRAINT uq_divorce_active_nikah UNIQUE (active_nikah_id),
    INDEX idx_divorce_masjid (masjid_id, divorce_date),
    CONSTRAINT fk_divorce_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id),
    CONSTRAINT fk_divorce_nikah FOREIGN KEY (nikah_id) REFERENCES nikah_requests (id)
);

CREATE TABLE divorce_sequences (
    masjid_id BIGINT NOT NULL,
    rec_year  INT    NOT NULL,
    last_seq  INT    NOT NULL,
    PRIMARY KEY (masjid_id, rec_year)
);
