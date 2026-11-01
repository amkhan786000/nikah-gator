-- Juma request letters (service JUMA_LETTER) and community service certificates (service COMMUNITY_SERVICE).
CREATE TABLE juma_letters (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id      BIGINT       NOT NULL,
    school_name    VARCHAR(200) NOT NULL,
    school_address VARCHAR(300) NULL,
    school_contact VARCHAR(200) NULL,
    addressee      VARCHAR(200) NOT NULL,
    letter_date    DATE         NOT NULL,
    standing       TINYINT(1)   NOT NULL DEFAULT 0,
    start_date     DATE         NOT NULL,
    end_date       DATE         NULL,
    window_start   TIME         NOT NULL,
    window_end     TIME         NOT NULL,
    note           VARCHAR(1000) NULL,
    status         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, VOID
    void_reason    VARCHAR(500) NULL,
    voided_at      TIMESTAMP    NULL,
    voided_by      BIGINT       NULL,
    created_by     BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_juma_masjid (masjid_id, id),
    CONSTRAINT fk_juma_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);

CREATE TABLE juma_letter_students (
    letter_id BIGINT       NOT NULL,
    position  INT          NOT NULL,
    name      VARCHAR(150) NOT NULL,
    grade     VARCHAR(40)  NULL,
    PRIMARY KEY (letter_id, position),
    CONSTRAINT fk_juma_students FOREIGN KEY (letter_id) REFERENCES juma_letters (id) ON DELETE CASCADE
);

CREATE TABLE community_service_certs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id    BIGINT        NOT NULL,
    cert_number  VARCHAR(40)   NOT NULL,
    person_name  VARCHAR(200)  NOT NULL,
    person_type  VARCHAR(10)   NOT NULL, -- STUDENT, VOLUNTEER
    organization VARCHAR(200)  NULL,
    activities   VARCHAR(2000) NOT NULL,
    start_date   DATE          NOT NULL,
    end_date     DATE          NOT NULL,
    hours        DECIMAL(7,2)  NOT NULL,
    supervisor   VARCHAR(200)  NOT NULL,
    notes        VARCHAR(1000) NULL,
    issue_date   DATE          NOT NULL,
    status       VARCHAR(10)   NOT NULL DEFAULT 'VALID', -- VALID, VOID
    void_reason  VARCHAR(500)  NULL,
    voided_at    TIMESTAMP     NULL,
    voided_by    BIGINT        NULL,
    created_by   BIGINT        NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_cs_number UNIQUE (cert_number),
    INDEX idx_cs_masjid (masjid_id, id),
    CONSTRAINT fk_cs_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);

-- Race-safe per masjid per year counter (same technique as certificate_sequences).
CREATE TABLE community_service_sequences (
    masjid_id BIGINT NOT NULL,
    cert_year INT    NOT NULL,
    last_seq  INT    NOT NULL,
    PRIMARY KEY (masjid_id, cert_year)
);
