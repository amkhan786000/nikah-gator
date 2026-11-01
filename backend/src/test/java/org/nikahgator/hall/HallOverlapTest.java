package org.nikahgator.hall;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class HallOverlapTest {
    private static LocalDateTime t(int h) { return LocalDateTime.of(2026, 12, 5, h, 0); }

    private static HallBooking b(Long id, String space, String status, int from, int to) {
        HallBooking x = new HallBooking();
        x.setId(id);
        x.setSpace(space);
        x.setStatus(status);
        x.setStartsAt(t(from));
        x.setEndsAt(t(to));
        return x;
    }

    @Test
    void touchingRangesDoNotOverlap() {
        assertFalse(HallOverlap.overlaps(t(10), t(12), t(12), t(14)));
        assertFalse(HallOverlap.overlaps(t(12), t(14), t(10), t(12)));
    }

    @Test
    void partialAndContainedRangesOverlap() {
        assertTrue(HallOverlap.overlaps(t(10), t(12), t(11), t(13)));
        assertTrue(HallOverlap.overlaps(t(10), t(14), t(11), t(12)));
        assertTrue(HallOverlap.overlaps(t(11), t(12), t(10), t(14)));
    }

    @Test
    void confirmedBookingInSameSpaceConflicts() {
        HallBooking existing = b(1L, "Main hall", "CONFIRMED", 10, 12);
        assertSame(existing, HallOverlap.firstConflict(b(null, "main hall ", "CONFIRMED", 11, 13), List.of(existing)));
    }

    @Test
    void otherSpaceOrNonConfirmedDoesNotConflict() {
        HallBooking cand = b(null, "Main hall", "CONFIRMED", 11, 13);
        assertNull(HallOverlap.firstConflict(cand, List.of(b(1L, "Classroom", "CONFIRMED", 10, 12))));
        assertNull(HallOverlap.firstConflict(cand, List.of(b(2L, "Main hall", "REQUESTED", 10, 12))));
        assertNull(HallOverlap.firstConflict(cand, List.of(b(3L, "Main hall", "CANCELLED", 10, 12))));
    }

    @Test
    void aBookingNeverConflictsWithItself() {
        HallBooking self = b(5L, "Main hall", "CONFIRMED", 10, 12);
        assertNull(HallOverlap.firstConflict(self, List.of(self)));
    }

    @Test
    void blankSpaceMeansMainHall() {
        assertTrue(HallOverlap.sameSpace(null, "Main hall"));
        assertTrue(HallOverlap.sameSpace(" ", "MAIN HALL"));
    }
}
