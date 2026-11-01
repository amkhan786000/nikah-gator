-- Counseling session log (private to the imam of the masjid) and hall/event bookings.
CREATE TABLE counseling_sessions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id     BIGINT NOT NULL,
    imam_id       BIGINT NOT NULL,
    client_name   VARCHAR(200) NOT NULL,
    session_type  VARCHAR(20) NOT NULL,
    session_at    DATETIME NOT NULL,
    duration_min  INT NULL,
    notes         TEXT NULL,
    follow_up_on  DATE NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_counseling_masjid_at (masjid_id, session_at),
    CONSTRAINT fk_counseling_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id),
    CONSTRAINT fk_counseling_imam FOREIGN KEY (imam_id) REFERENCES app_users (id)
);

CREATE TABLE hall_bookings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id       BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    contact_name    VARCHAR(200) NOT NULL,
    contact_phone   VARCHAR(50) NULL,
    contact_email   VARCHAR(200) NULL,
    starts_at       DATETIME NOT NULL,
    ends_at         DATETIME NOT NULL,
    space           VARCHAR(100) NOT NULL DEFAULT 'Main hall',
    expected_guests INT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    fee_text        VARCHAR(200) NULL,
    notes           TEXT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hall_masjid_start (masjid_id, starts_at),
    CONSTRAINT fk_hall_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);
