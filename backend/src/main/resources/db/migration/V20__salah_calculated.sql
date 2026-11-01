-- Sunrise and Maghrib no longer have to be typed in: they are calculated from the masjid's location
-- (Maghrib = sunset + a number of minutes the imam chooses). A period's own value, when present, still wins.
-- A masjid can also hold more than one Jumu'ah: the single time becomes a list, stored as 'HH:mm|label;HH:mm|label'.
ALTER TABLE salah_periods MODIFY maghrib TIME NULL;
ALTER TABLE salah_periods ADD COLUMN jumuah_sessions VARCHAR(600) NULL AFTER isha;
UPDATE salah_periods
   SET jumuah_sessions = CONCAT(TIME_FORMAT(jumuah, '%H:%i'), '|')
 WHERE jumuah IS NOT NULL;
ALTER TABLE salah_periods DROP COLUMN jumuah;

ALTER TABLE salah_settings
    ADD COLUMN latitude           DOUBLE NULL,
    ADD COLUMN longitude          DOUBLE NULL,
    ADD COLUMN maghrib_offset_min INT NOT NULL DEFAULT 3;
