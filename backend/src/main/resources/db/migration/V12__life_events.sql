-- Janazah, shahada and baby-naming records (one table, told apart by type) and their number counters.
CREATE TABLE life_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id       BIGINT       NOT NULL,
    event_type      VARCHAR(12)  NOT NULL, -- JANAZAH, SHAHADA, BABY_NAMING
    cert_number     VARCHAR(40)  NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'VALID', -- VALID, VOID
    person_name     VARCHAR(200) NOT NULL, -- the deceased, the new Muslim, the child
    alt_name        VARCHAR(200) NULL,     -- chosen Muslim name
    event_date      DATE         NOT NULL, -- date of death / shahada / birth
    secondary_date  DATE         NULL,     -- janazah date / naming date
    secondary_time  TIME         NULL,     -- janazah time
    place           VARCHAR(300) NULL,     -- janazah place / shahada place / naming place
    burial_date     DATE         NULL,
    burial_time     TIME         NULL,
    burial_place    VARCHAR(300) NULL,
    cemetery        VARCHAR(200) NULL,
    contact_name    VARCHAR(200) NULL,
    contact_phone   VARCHAR(60)  NULL,
    parents         VARCHAR(300) NULL,
    witnesses       VARCHAR(300) NULL,
    imam_name       VARCHAR(160) NULL,
    notes           VARCHAR(2000) NULL,
    void_reason     VARCHAR(500) NULL,
    voided_at       TIMESTAMP    NULL,
    voided_by       BIGINT       NULL,
    created_by      BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_life_events_number UNIQUE (cert_number),
    INDEX idx_life_events_masjid (masjid_id, event_type, event_date),
    CONSTRAINT fk_life_events_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);

CREATE TABLE life_event_sequences (
    masjid_id  BIGINT      NOT NULL,
    event_type VARCHAR(12) NOT NULL,
    cert_year  INT         NOT NULL,
    last_seq   INT         NOT NULL,
    PRIMARY KEY (masjid_id, event_type, cert_year)
);
