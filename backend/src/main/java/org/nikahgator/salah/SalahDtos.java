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

    public record SettingsDto(String changeUnit, int changeEvery, boolean publicEnabled, int publicDays, boolean showSunrise) {
    }

    public record PeriodRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotBlank String fajr, String sunrise,
                                @NotBlank String dhuhr, @NotBlank String asr, @NotBlank String maghrib, @NotBlank String isha,
                                String jumuah, @Size(max = 200) String notes) {
    }

    public record PeriodDto(Long id, LocalDate startDate, LocalDate endDate, String fajr, String sunrise, String dhuhr,
                            String asr, String maghrib, String isha, String jumuah, String notes, boolean current) {
    }

    /** What the imam's page needs in one call. */
    public record Overview(SettingsDto settings, List<PeriodDto> periods, String slug, String timezone, LocalDate today,
                           LocalDate coveredThrough, Long daysLeft, boolean todayCovered, LocalDate nextStart, LocalDate nextEnd,
                           PeriodDto lastPeriod) {
    }

    public record ImportRow(int line, LocalDate startDate, LocalDate endDate, String fajr, String sunrise, String dhuhr,
                            String asr, String maghrib, String isha, String jumuah, String notes) {
    }

    public record ImportResult(boolean committed, int rowCount, int replacedCount, List<ImportRow> rows, List<String> errors) {
    }

    // ---- public page ----

    public record PublicDay(LocalDate date, String weekday, boolean today, boolean published, String fajr, String sunrise,
                            String dhuhr, String asr, String maghrib, String isha, String jumuah, String notes) {
    }

    public record PublicTimes(String masjidName, String address, String phone, String timezone, LocalDate today, String nowTime,
                              boolean showSunrise, List<PublicDay> days) {
    }
}
