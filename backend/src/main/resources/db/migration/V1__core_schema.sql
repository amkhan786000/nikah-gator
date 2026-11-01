-- Multi-tenant core: every masjid is a tenant; all other rows hang off masjid_id.
-- A masjid self-registers together with its imam. The imam is the manager of that masjid:
-- no board/committee approval step exists, the imam alone approves, schedules and issues.

CREATE TABLE masjids (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug          VARCHAR(80)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    address_line  VARCHAR(255),
    city          VARCHAR(100),
    state         CHAR(2),
    zip           VARCHAR(10),
    phone         VARCHAR(30),
    email         VARCHAR(190),
    timezone      VARCHAR(60)  NOT NULL DEFAULT 'America/New_York',
    nikah_fee_cents INT        NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE app_users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id     BIGINT       NULL,
    email         VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(30)  NOT NULL, -- PLATFORM_ADMIN, IMAM (manages own masjid), COUPLE
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);

CREATE TABLE nikah_requests (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id        BIGINT       NOT NULL,
    requested_by     BIGINT       NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED, DOCS_PENDING, APPROVED (by imam), SCHEDULED, COMPLETED, CANCELLED, REJECTED
    groom_name       VARCHAR(200) NOT NULL,
    groom_email      VARCHAR(190),
    groom_phone      VARCHAR(30),
    bride_name       VARCHAR(200) NOT NULL,
    bride_email      VARCHAR(190),
    bride_phone      VARCHAR(30),
    wali_name        VARCHAR(200),
    wali_relation    VARCHAR(80),
    witness1_name    VARCHAR(200),
    witness2_name    VARCHAR(200),
    mahr_description VARCHAR(500),
    preferred_date   DATE,
    scheduled_at     DATETIME NULL,
    imam_id          BIGINT NULL,
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id),
    CONSTRAINT fk_req_user   FOREIGN KEY (requested_by) REFERENCES app_users (id),
    CONSTRAINT fk_req_imam   FOREIGN KEY (imam_id) REFERENCES app_users (id),
    INDEX idx_req_masjid_status (masjid_id, status),
    INDEX idx_req_scheduled (masjid_id, scheduled_at)
);

CREATE TABLE checklist_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id  BIGINT       NOT NULL,
    code        VARCHAR(60)  NOT NULL, -- MARRIAGE_LICENSE, GROOM_ID, BRIDE_ID, WALI_CONSENT, PREMARITAL_COUNSELING ...
    label       VARCHAR(200) NOT NULL,
    required    BOOLEAN      NOT NULL DEFAULT TRUE,
    done        BOOLEAN      NOT NULL DEFAULT FALSE,
    done_at     TIMESTAMP    NULL,
    CONSTRAINT fk_check_req FOREIGN KEY (request_id) REFERENCES nikah_requests (id) ON DELETE CASCADE
);

CREATE TABLE documents (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id   BIGINT       NOT NULL,
    checklist_id BIGINT       NULL,
    file_name    VARCHAR(255) NOT NULL,
    stored_path  VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    size_bytes   BIGINT,
    uploaded_by  BIGINT       NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_req  FOREIGN KEY (request_id) REFERENCES nikah_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_user FOREIGN KEY (uploaded_by) REFERENCES app_users (id)
);

CREATE TABLE certificates (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id    BIGINT       NOT NULL UNIQUE,
    masjid_id     BIGINT       NOT NULL,
    cert_number   VARCHAR(40)  NOT NULL UNIQUE,
    nikah_date    DATE         NOT NULL,
    issued_by     BIGINT       NOT NULL,
    issued_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pdf_path      VARCHAR(500),
    CONSTRAINT fk_cert_req    FOREIGN KEY (request_id) REFERENCES nikah_requests (id),
    CONSTRAINT fk_cert_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id),
    CONSTRAINT fk_cert_user   FOREIGN KEY (issued_by) REFERENCES app_users (id)
);

CREATE TABLE payments (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id   BIGINT      NOT NULL,
    amount_cents INT         NOT NULL,
    currency     CHAR(3)     NOT NULL DEFAULT 'USD',
    kind         VARCHAR(20) NOT NULL DEFAULT 'FEE', -- FEE, DONATION
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, FAILED, REFUNDED
    provider     VARCHAR(30),
    provider_ref VARCHAR(120),
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_req FOREIGN KEY (request_id) REFERENCES nikah_requests (id)
);

CREATE TABLE notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id  BIGINT       NOT NULL,
    request_id BIGINT       NULL,
    channel    VARCHAR(10)  NOT NULL DEFAULT 'EMAIL', -- EMAIL, SMS
    recipient  VARCHAR(190) NOT NULL,
    subject    VARCHAR(255),
    body       TEXT,
    status     VARCHAR(15)  NOT NULL DEFAULT 'PENDING',
    send_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at    TIMESTAMP    NULL,
    INDEX idx_notif_due (status, send_at)
);
