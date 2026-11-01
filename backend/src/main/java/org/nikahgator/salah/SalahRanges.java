package org.nikahgator.salah;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Date-range arithmetic for the schedule (pure, so it is easy to test). */
public final class SalahRanges {
    private SalahRanges() {
    }

    public record Range(LocalDate start, LocalDate end) {
    }

    /**
     * What is left of [existingStart, existingEnd] after the new range [newStart, newEnd] takes its days: nothing,
     * one piece, or two pieces (when the new range lands in the middle).
     */
    public static List<Range> remainder(LocalDate existingStart, LocalDate existingEnd, LocalDate newStart, LocalDate newEnd) {
        List<Range> out = new ArrayList<>();
        if (newEnd.isBefore(existingStart) || newStart.isAfter(existingEnd)) {
            out.add(new Range(existingStart, existingEnd)); // no overlap: untouched
            return out;
        }
        if (existingStart.isBefore(newStart)) {
            out.add(new Range(existingStart, newStart.minusDays(1)));
        }
        if (existingEnd.isAfter(newEnd)) {
            out.add(new Range(newEnd.plusDays(1), existingEnd));
        }
        return out;
    }

    /** The end of a period that starts on {@code start} and lasts "every N unit" (unit DAYS, WEEKS or MONTHS). */
    public static LocalDate suggestedEnd(LocalDate start, String unit, int every) {
        int n = Math.max(1, every);
        return switch (unit == null ? "MONTHS" : unit) {
            case "DAYS" -> start.plusDays(n - 1L);
            case "WEEKS" -> start.plusDays(7L * n - 1);
            default -> start.plusMonths(n).minusDays(1);
        };
    }

    public static long days(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
