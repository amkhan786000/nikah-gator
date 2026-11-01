-- Which services (see org.nikahgator.catalog.ServiceKey) each masjid and each imam may use.
-- A missing row means "use the catalog default" (enabled). A service is usable only if the
-- masjid allows it AND the imam's own override (if any) does not turn it off.
CREATE TABLE masjid_services (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id   BIGINT NOT NULL,
    service_key VARCHAR(40) NOT NULL,
    enabled     TINYINT(1) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_masjid_services UNIQUE (masjid_id, service_key),
    CONSTRAINT fk_masjid_services_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);

CREATE TABLE imam_services (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    service_key VARCHAR(40) NOT NULL,
    enabled     TINYINT(1) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_imam_services UNIQUE (user_id, service_key),
    CONSTRAINT fk_imam_services_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);
