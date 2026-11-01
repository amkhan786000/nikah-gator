-- One certificate design per masjid, edited by its imam.
CREATE TABLE certificate_templates (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id             BIGINT NOT NULL,
    title                 VARCHAR(120) NOT NULL DEFAULT 'Nikah Certificate',
    bismillah_enabled     TINYINT(1) NOT NULL DEFAULT 1,
    masjid_display_name   VARCHAR(160) NULL,
    address_text          VARCHAR(300) NULL,
    phone_text            VARCHAR(60) NULL,
    intro_text            VARCHAR(500) NOT NULL,
    body_text             VARCHAR(2000) NOT NULL,
    footer_text           VARCHAR(500) NOT NULL DEFAULT '',
    left_signature_label  VARCHAR(80) NOT NULL DEFAULT 'Imam / Officiant',
    right_signature_label VARCHAR(80) NOT NULL DEFAULT 'Date',
    accent_color          VARCHAR(7) NOT NULL DEFAULT '#4f7a00',
    logo_path             VARCHAR(300) NULL,
    paper_size            VARCHAR(10) NOT NULL DEFAULT 'LETTER',
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_certificate_templates_masjid UNIQUE (masjid_id),
    CONSTRAINT fk_certificate_templates_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);
