package org.nikahgator.hall;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** The double-booking rule, kept free of the database so it can be unit tested. */
public final class HallOverlap {
    private HallOverlap() {}

    /** Half-open ranges: one ending exactly when the next starts does not overlap. */
    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    public static boolean sameSpace(String a, String b) {
        return norm(a).equalsIgnoreCase(norm(b));
    }

    private static String norm(String s) {
        return s == null || s.isBlank() ? HallBooking.DEFAULT_SPACE : s.trim();
    }

    /**
     * The first CONFIRMED booking in {@code existing} (other than the candidate itself) that is in the same space
     * and overlaps the candidate's time range, or null.
     */
    public static HallBooking firstConflict(HallBooking candidate, List<HallBooking> existing) {
        for (HallBooking other : existing) {
            if (!HallBooking.CONFIRMED.equals(other.getStatus())) continue;
            if (candidate.getId() != null && Objects.equals(other.getId(), candidate.getId())) continue;
            if (!sameSpace(other.getSpace(), candidate.getSpace())) continue;
            if (overlaps(candidate.getStartsAt(), candidate.getEndsAt(), other.getStartsAt(), other.getEndsAt())) return other;
        }
        return null;
    }
}
