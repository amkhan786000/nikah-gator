package org.nikahgator.lifeevent;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.nikahgator.catalog.ServiceKey;
import org.nikahgator.exception.ApiException;
import org.nikahgator.lifeevent.LifeEventDtos.Input;

class LifeEventRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private static Input in(String name, LocalDate event, LocalDate second, String parents, String witnesses) {
        return new Input(name, null, event, second, LocalTime.of(13, 0), " Masjid ", null, null, null, null, null, null,
                parents, witnesses, null, null);
    }

    @Test
    void numbersCarryTypeMasjidYearAndFiveDigits() {
        assertEquals("NG-JZ-12-2026-00042", LifeEventNumbers.format(LifeEventType.JANAZAH, 12, 2026, 42));
        assertEquals("NG-SH-3-2026-00001", LifeEventNumbers.format(LifeEventType.SHAHADA, 3, 2026, 1));
        assertEquals("NG-BN-3-2027-12345", LifeEventNumbers.format(LifeEventType.BABY_NAMING, 3, 2027, 12345));
        assertTrue(LifeEventNumbers.isValid("NG-JZ-12-2026-00042"));
        assertFalse(LifeEventNumbers.isValid("NG-12-2026-00042"));
        assertFalse(LifeEventNumbers.isValid("NG-XX-12-2026-00042"));
        assertThrows(IllegalArgumentException.class, () -> LifeEventNumbers.format(LifeEventType.JANAZAH, 0, 2026, 1));
    }

    @Test
    void typesMapToTheirServices() {
        assertEquals(ServiceKey.JANAZAH, LifeEventType.JANAZAH.service());
        assertEquals("baby-naming", LifeEventType.BABY_NAMING.slug());
        assertEquals(LifeEventType.SHAHADA, LifeEventType.bySlug("shahada").orElseThrow());
        assertTrue(LifeEventType.bySlug("juma").isEmpty());
        assertEquals(ServiceKey.SHAHADA, ServiceKey.forPath("/api/imam/svc/shahada/5/pdf").orElseThrow());
    }

    @Test
    void janazahNeedsNameDeathAndJanazahDates() {
        Input ok = LifeEventRules.normalize(LifeEventType.JANAZAH, in("  Ali Khan ", LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31), null, null), TODAY);
        assertEquals("Ali Khan", ok.personName());
        assertEquals("Masjid", ok.place());
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.JANAZAH, in(" ", LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31), null, null), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.JANAZAH, in("A", LocalDate.of(2026, 10, 30), null, null, null), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.JANAZAH, in("A", LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), null, null), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.JANAZAH, in("A", LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 29), null, null), TODAY));
    }

    @Test
    void burialCannotPrecedeJanazah() {
        Input bad = new Input("A", null, LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31), null, null,
                LocalDate.of(2026, 10, 30), null, null, null, null, null, null, null, null, null);
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.JANAZAH, bad, TODAY));
    }

    @Test
    void shahadaNeedsAWitnessAndNoFutureDate() {
        assertNotNull(LifeEventRules.normalize(LifeEventType.SHAHADA, in("Sara", LocalDate.of(2026, 10, 1), null, null, "A and B"), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.SHAHADA, in("Sara", LocalDate.of(2026, 10, 1), null, null, "  "), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.SHAHADA, in("Sara", LocalDate.of(2027, 1, 1), null, null, "A"), TODAY));
    }

    @Test
    void babyNamingNeedsParentsAndNamingAfterBirth() {
        LocalDate birth = LocalDate.of(2026, 10, 1);
        assertNotNull(LifeEventRules.normalize(LifeEventType.BABY_NAMING, in("Noor", birth, birth.plusDays(7), "Ali and Sara", null), TODAY));
        assertNotNull(LifeEventRules.normalize(LifeEventType.BABY_NAMING, in("Noor", birth, birth, "Ali and Sara", null), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.BABY_NAMING, in("Noor", birth, birth.plusDays(7), null, null), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.BABY_NAMING, in("Noor", birth, birth.minusDays(1), "A", null), TODAY));
        assertThrows(ApiException.class, () -> LifeEventRules.normalize(LifeEventType.BABY_NAMING, in("Noor", birth, null, "A", null), TODAY));
    }

    @Test
    void controlCharactersAreStripped() {
        assertEquals("AliKhan", LifeEventRules.clean("Ali\u0000\u0007Khan"));
        assertNull(LifeEventRules.clean("  \u0000 "));
    }

    @Test
    void pdfsStartWithPdfHeaderAndVoidedOnesStillRender() {
        var lh = new LifeEventPdf.Letterhead("Masjid Al-Noor", "1 Main St", "555-0100", "#4f7a00", "LETTER");
        for (LifeEventType t : LifeEventType.values()) {
            LifeEvent e = new LifeEvent();
            e.setEventType(t);
            e.setCertNumber(LifeEventNumbers.format(t, 1, 2026, 1));
            e.setPersonName("Test Person");
            e.setEventDate(LocalDate.of(2026, 10, 1));
            e.setSecondaryDate(LocalDate.of(2026, 10, 2));
            e.setSecondaryTime(LocalTime.of(14, 30));
            e.setParents("Ali and Sara");
            e.setWitnesses("A and B");
            e.setAltName("Aisha");
            e.setCemetery("Memorial Gardens");
            e.setBurialDate(LocalDate.of(2026, 10, 2));
            byte[] pdf = LifeEventPdf.render(e, lh, null, null, TODAY);
            assertEquals("%PDF-", new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII), t.name());
            e.setStatus(LifeEvent.VOID);
            assertTrue(LifeEventPdf.render(e, lh, null, null, TODAY).length > 500);
        }
    }
}
