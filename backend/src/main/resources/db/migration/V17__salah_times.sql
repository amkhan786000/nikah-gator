-- Salah timetable. A schedule is a list of date ranges ("periods"), each with one set of prayer times, so a
-- masjid that changes monthly, every 10 days or every 2 weeks (or daily, from a CSV) uses the same model.
CREATE TABLE salah_settings (
    masjid_id      BIGINT PRIMARY KEY,
    change_unit    VARCHAR(10) NOT NULL DEFAULT 'MONTHS',   -- DAYS, WEEKS or MONTHS: how often the times change
    change_every   INT NOT NULL DEFAULT 1,                  -- ... every N of that unit (used to suggest the next period)
    public_enabled TINYINT(1) NOT NULL DEFAULT 1,           -- the shareable link works
    public_days    INT NOT NULL DEFAULT 1,                  -- how many days (from today) the link shows
    show_sunrise   TINYINT(1) NOT NULL DEFAULT 1,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_salah_settings_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id)
);

CREATE TABLE salah_periods (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    masjid_id  BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date   DATE NOT NULL,
    fajr       TIME NOT NULL,
    sunrise    TIME NULL,
    dhuhr      TIME NOT NULL,
    asr        TIME NOT NULL,
    maghrib    TIME NOT NULL,
    isha       TIME NOT NULL,
    jumuah     TIME NULL,
    notes      VARCHAR(200) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_salah_periods_masjid FOREIGN KEY (masjid_id) REFERENCES masjids (id),
    INDEX idx_salah_periods_range (masjid_id, start_date, end_date)
);
