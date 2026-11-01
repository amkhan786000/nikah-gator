package org.nikahgator.letters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LetterNumbersTest {

    @Test
    void formatsMasjidYearAndFiveDigitSequence() {
        assertEquals("NG-LT-12-2026-00042", LetterNumbers.format(12, 2026, 42));
        assertEquals("NG-LT-1-2026-00001", LetterNumbers.format(1, 2026, 1));
        assertEquals("NG-LT-1-2026-123456", LetterNumbers.format(1, 2026, 123456));
    }

    @Test
    void rejectsBadParts() {
        assertThrows(IllegalArgumentException.class, () -> LetterNumbers.format(0, 2026, 1));
        assertThrows(IllegalArgumentException.class, () -> LetterNumbers.format(1, 1999, 1));
        assertThrows(IllegalArgumentException.class, () -> LetterNumbers.format(1, 2026, 0));
    }

    @Test
    void parsesOnlyWellFormedLetterNumbers() {
        assertArrayEquals(new long[] { 12, 2026, 42 }, LetterNumbers.parse("NG-LT-12-2026-00042"));
        assertNull(LetterNumbers.parse("NG-12-2026-00042"));
        assertNull(LetterNumbers.parse("NG-LT-12-2026-42"));
        assertNull(LetterNumbers.parse(null));
        assertTrue(LetterNumbers.isValid("NG-LT-3-2027-00001"));
        assertFalse(LetterNumbers.isValid("NG-LT-3-2027-00001; DROP"));
    }
}
