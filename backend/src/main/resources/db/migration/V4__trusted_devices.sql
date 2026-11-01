-- "Remember this device": a browser that entered the emailed code may skip it until expires_at.
-- Only a SHA-256 hash of the cookie token is stored.
CREATE TABLE trusted_devices (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    device_name  VARCHAR(120),
    ip_address   VARCHAR(64),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP    NOT NULL,
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    INDEX idx_device_user (user_id),
    INDEX idx_device_expiry (expires_at)
);
