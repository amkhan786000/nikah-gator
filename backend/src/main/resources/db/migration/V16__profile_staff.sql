-- Profile: a job title the user can set. Staff: people the imam adds to the masjid, each with a
-- View or Manage permission per service (role STAFF in app_users).
ALTER TABLE app_users ADD COLUMN job_title VARCHAR(80) NULL AFTER phone;

CREATE TABLE staff_permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    service_key VARCHAR(40) NOT NULL,
    level       VARCHAR(10) NOT NULL,  -- VIEW or MANAGE
    CONSTRAINT uq_staff_permissions UNIQUE (user_id, service_key),
    CONSTRAINT fk_staff_permissions_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);
