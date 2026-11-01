ALTER TABLE app_users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP NULL;

-- Sign-in codes e-mailed after a correct password (valid 5 minutes, one use).
CREATE TABLE login_otps (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts   INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    INDEX idx_otp_user (user_id, used)
);

-- "Forgot password" codes (valid 15 minutes, one use).
CREATE TABLE password_reset_codes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts   INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    INDEX idx_reset_user (user_id, used)
);

-- Every e-mail the app tried to send; codes and passwords are stored as [hidden].
CREATE TABLE email_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient  VARCHAR(190) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body       TEXT,
    success    BOOLEAN      NOT NULL,
    error      VARCHAR(500),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_created (created_at)
);
