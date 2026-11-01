package org.nikahgator.salah;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Times travel as "HH:mm" strings (24-hour) so the browser and CSV handling stay simple. */
public final class SalahDtos {
    private SalahDtos() {
    }

    /** One Jumu'ah (a masjid may hold several, for example an English and an Arabic khutbah). */
    public record Session(String time, String label) {
    }

    public record SettingsDto(String changeUnit, int changeEvery, boolean publicEnabled, int publicDays, boolean showSunrise,
                              Double latitude, Double longitude, int maghribOffsetMinutes) {
    }

    /** Today's calculated sun times, so the imam can check the location and the minutes after sunset. */
    public record SunPreview(String sunrise, String sunset, String maghrib) {
    }

    /** Sunrise and Maghrib may be left empty: they are then calculated from the masjid's location. */
    public record PeriodRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotBlank String fajr, String sunrise,
                                @NotBlank String dhuhr, @NotBlank String asr, String maghrib, @NotBlank String isha,
                                List<Session> jumuah, @Size(max = 200) String notes) {
    }

    public record PeriodDto(Long id, LocalDate startDate, LocalDate endDate, String fajr, String sunrise, String dhuhr,
                            String asr, String maghrib, String isha, List<Session> jumuah, String notes, boolean current) {
    }

    /** What the imam's page needs in one call. */
    public record Overview(SettingsDto settings, List<PeriodDto> periods, String slug, String timezone, LocalDate today,
                           LocalDate coveredThrough, Long daysLeft, boolean todayCovered, LocalDate nextStart, LocalDate nextEnd,
                           PeriodDto lastPeriod, SunPreview sunToday) {
    }

    public record ImportRow(int line, LocalDate startDate, LocalDate endDate, String fajr, String sunrise, String dhuhr,
                            String asr, String maghrib, String isha, List<Session> jumuah, String notes) {
    }

    public record ImportResult(boolean committed, int rowCount, int replacedCount, List<ImportRow> rows, List<String> errors) {
    }

    public record Location(Double latitude, Double longitude, String place) {
    }

    // ---- public page ----

    public record PublicDay(LocalDate date, String weekday, boolean today, boolean published, String fajr, String sunrise,
                            String dhuhr, String asr, String maghrib, String isha, List<Session> jumuah, String notes) {
    }

    public record PublicTimes(String masjidName, String address, String phone, String timezone, LocalDate today, String nowTime,
                              boolean showSunrise, List<PublicDay> days) {
    }
}
