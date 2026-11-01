-- General letters on masjid letterhead (service GENERAL_LETTER).
CREATE TABLE letters (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id        BIGINT       NOT NULL,
    letter_number    VARCHAR(40)  NOT NULL,
    letter_type      VARCHAR(30)  NOT NULL,
    recipient_name   VARCHAR(200) NOT NULL,
    addressee        VARCHAR(300) NULL,
    purpose          VARCHAR(500) NULL,
    subject          VARCHAR(200) NOT NULL,
    body             TEXT         NOT NULL,
    letter_date      DATE         NOT NULL,
    signer_name      VARCHAR(200) NOT NULL DEFAULT '',
    status           VARCHAR(10)  NOT NULL DEFAULT 'ISSUED', -- ISSUED, VOID
    void_reason      VARCHAR(500) NULL,
    voided_at        TIMESTAMP    NULL,
    voided_by        BIGINT       NULL,
    created_by       BIGINT       NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_letters_number UNIQUE (letter_number),
    INDEX idx_letters_masjid (masjid_id, letter_date)
);

CREATE TABLE letter_sequences (
    masjid_id   BIGINT NOT NULL,
    letter_year INT    NOT NULL,
    last_seq    INT    NOT NULL,
    PRIMARY KEY (masjid_id, letter_year)
);
