package org.nikahgator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.nikahgator.service.CertificateNumbers;

class CertificateNumbersTest {

    @Test
    void formatsMasjidYearAndFiveDigitSequence() {
        assertEquals("NG-12-2026-00042", CertificateNumbers.format(12, 2026, 42));
        assertEquals("NG-1-2026-00001", CertificateNumbers.format(1, 2026, 1));
        assertEquals("NG-1-2026-123456", CertificateNumbers.format(1, 2026, 123456));
    }

    @Test
    void rejectsBadParts() {
        assertThrows(IllegalArgumentException.class, () -> CertificateNumbers.format(0, 2026, 1));
        assertThrows(IllegalArgumentException.class, () -> CertificateNumbers.format(1, 2026, 0));
    }

    @Test
    void parsesOnlyWellFormedNumbers() {
        assertArrayEquals(new long[] { 12, 2026, 42 }, CertificateNumbers.parse("NG-12-2026-00042"));
        assertNull(CertificateNumbers.parse("NG-12-2026-42"));
        assertNull(CertificateNumbers.parse("ng-12-2026-00042"));
        assertNull(CertificateNumbers.parse("NG-12-2026-00042' OR 1=1"));
        assertNull(CertificateNumbers.parse(null));
        assertTrue(CertificateNumbers.isValid("NG-3-2027-00001"));
        assertFalse(CertificateNumbers.isValid("../etc/passwd"));
    }
}
