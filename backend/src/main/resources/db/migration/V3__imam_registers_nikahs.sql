-- Masjids are created by the platform admin together with their imam; the imam registers nikahs directly.
ALTER TABLE nikah_requests
    ADD COLUMN groom_father_name VARCHAR(200) NULL AFTER groom_phone,
    ADD COLUMN bride_father_name VARCHAR(200) NULL AFTER bride_phone,
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED'; -- REGISTERED, COMPLETED, CANCELLED
