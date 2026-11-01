package org.nikahgator.divorce;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.nikahgator.catalog.ServiceKey;
import org.nikahgator.divorce.DivorceDtos.Input;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.letterhead.Letterhead;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.NikahRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

class DivorceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

    private static Input in(String type, Integer talaq, String status, LocalDate date, LocalDate iddah, LocalDate revoked) {
        return new Input(null, type, status, " Omar Khan ", "Sara Ali", null, "  ", null, null, date, talaq, iddah, revoked,
                null, null, null, null, null);
    }

    private static Input talaq1() {
        return in("TALAQ", 1, null, TODAY.minusDays(10), null, null);
    }

    private static AppUser imam(long masjid) {
        AppUser u = new AppUser();
        u.setId(5L);
        u.setMasjidId(masjid);
        return u;
    }

    private static DivorceService service(DivorceRepository repo, NikahRequestRepository nikahs) {
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbc.sql(anyString()).param(anyString(), any()).param(anyString(), any()).query(Integer.class).single()).thenReturn(1);
        return new DivorceService(repo, nikahs, mock(MasjidRepository.class), mock(Letterhead.class), jdbc);
    }

    @Test
    void numberFormatAndValidation() {
        assertEquals("NG-DV-12-2026-00042", DivorceNumbers.format(12, 2026, 42));
        assertThrows(IllegalArgumentException.class, () -> DivorceNumbers.format(0, 2026, 1));
        assertTrue(DivorceNumbers.isValid("NG-DV-12-2026-00042"));
        assertFalse(DivorceNumbers.isValid("NG-CS-12-2026-00042"));
        assertFalse(DivorceNumbers.isValid("NG-DV-12-2026-42"));
    }

    @Test
    void normalizesTextAndDefaultsStatus() {
        Input r = DivorceRules.normalize(talaq1(), TODAY);
        assertEquals("Omar Khan", r.groomName());
        assertNull(r.brideFatherName());
        assertEquals("RECORDED", r.status());
        assertEquals("TALAQ", r.divorceType());
    }

    @Test
    void talaqNumberOnlyForTalaq() {
        assertEquals(1, DivorceRules.normalize(in("talaq", 1, null, TODAY, null, null), TODAY).talaqNumber());
        assertNull(DivorceRules.normalize(in("KHULA", 2, null, TODAY, null, null), TODAY).talaqNumber());
        assertBad(in("TALAQ", null, null, TODAY, null, null));
        assertBad(in("TALAQ", 4, null, TODAY, null, null));
    }

    @Test
    void rejectsBadInput() {
        assertBad(in("DIVORCE", null, null, TODAY, null, null));
        assertBad(in("KHULA", null, null, TODAY.plusDays(1), null, null));
        assertBad(in("KHULA", null, null, null, null, null));
        assertBad(in("KHULA", null, null, TODAY, TODAY.minusDays(1), null));
        assertBad(in("KHULA", null, "WHATEVER", TODAY, null, null));
        Input noName = new Input(null, "KHULA", null, " ", "Sara", null, null, null, null, TODAY, null, null, null, null, null, null, null, null);
        assertBad(noName);
        Input marriedAfter = new Input(null, "KHULA", null, "A", "B", null, null, TODAY.plusDays(1), null, TODAY, null, null, null, null, null, null, null, null);
        assertBad(marriedAfter);
    }

    @Test
    void revocationRules() {
        LocalDate d = TODAY.minusDays(30);
        LocalDate iddah = DivorceRules.suggestedIddahEnd(d);
        Input ok = DivorceRules.normalize(in("TALAQ", 2, "REVOKED", d, iddah, d.plusDays(20)), TODAY);
        assertEquals("REVOKED", ok.status());
        assertBad(in("TALAQ", 3, "REVOKED", d, iddah, d.plusDays(20)));              // final talaq cannot be revoked
        assertBad(in("KHULA", null, "REVOKED", d, iddah, d.plusDays(20)));           // only talaq
        assertBad(in("TALAQ", 1, "REVOKED", d, iddah, null));                        // date required
        assertBad(in("TALAQ", 1, "REVOKED", d, iddah, d.minusDays(1)));              // before divorce
        assertBad(in("TALAQ", 1, "REVOKED", d, d.plusDays(10), d.plusDays(11)));     // after iddah
        assertBad(in("TALAQ", 1, "REVOKED", d, null, TODAY.plusDays(1)));            // future
        assertNull(DivorceRules.normalize(in("TALAQ", 1, "RECORDED", d, iddah, d.plusDays(2)), TODAY).revokedDate(), "recorded drops a stray date");
    }

    @Test
    void iddahSuggestionIsAboutThreeMonths() {
        assertEquals(LocalDate.of(2026, 12, 15), DivorceRules.suggestedIddahEnd(LocalDate.of(2026, 9, 15)));
    }

    @Test
    void witnessesAreCompacted() {
        Input base = talaq1();
        Input r = DivorceRules.normalize(new Input(null, "KHULA", null, "A", "B", null, null, null, null, TODAY, null, null, null, null, null,
                " ", " Zayd ", null), TODAY);
        assertEquals("Zayd", r.witness1Name());
        assertNull(r.witness2Name());
        assertNotNull(base);
    }

    @Test
    void nikahFillsBlanksButNotWhatTheImamTyped() {
        NikahRequest n = new NikahRequest();
        n.setGroomName("Omar From Nikah");
        n.setBrideName("Sara From Nikah");
        n.setGroomFatherName("Father G");
        n.setScheduledAt(LocalDateTime.of(2020, 5, 3, 11, 0));
        Input typed = new Input(7L, "KHULA", null, "Typed Groom", null, null, null, null, null, TODAY, null, null, null, null, null, null, null, null);
        Input r = DivorceService.withNikahDefaults(typed, n);
        assertEquals("Typed Groom", r.groomName());
        assertEquals("Sara From Nikah", r.brideName());
        assertEquals("Father G", r.groomFatherName());
        assertEquals(LocalDate.of(2020, 5, 3), r.marriageDate());
    }

    @Test
    void createWithoutNikahAssignsNextNumberOfMasjidAndYear() {
        DivorceRepository repo = mock(DivorceRepository.class);
        when(repo.save(any())).thenAnswer(i -> {
            Divorce d = i.getArgument(0);
            d.setId(1L);
            return d;
        });
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbc.sql(anyString()).param(anyString(), any()).param(anyString(), any()).query(Integer.class).single()).thenReturn(7);
        DivorceService svc = new DivorceService(repo, mock(NikahRequestRepository.class), mock(MasjidRepository.class), mock(Letterhead.class), jdbc);
        LocalDate today = LocalDate.now();
        var v = svc.create(imam(3), in("KHULA", null, null, today.minusDays(2), null, null));
        assertEquals("NG-DV-3-" + today.getYear() + "-00007", v.number());
        assertNull(v.nikahId());
        assertFalse(v.voided());
    }

    @Test
    void secondActiveDivorceForTheSameNikahIsAConflict() {
        DivorceRepository repo = mock(DivorceRepository.class);
        NikahRequestRepository nikahs = mock(NikahRequestRepository.class);
        NikahRequest n = new NikahRequest();
        n.setId(9L);
        when(nikahs.findByIdAndMasjidId(9L, 3L)).thenReturn(Optional.of(n));
        when(repo.findByNikahIdAndMasjidIdAndVoidedFalse(9L, 3L)).thenReturn(Optional.of(new Divorce()));
        Input body = new Input(9L, "KHULA", null, null, null, null, null, null, null, TODAY.minusDays(1), null, null, null, null, null, null, null, null);
        ApiException e = assertThrows(ApiException.class, () -> service(repo, nikahs).create(imam(3), body));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(repo, never()).save(any());
    }

    @Test
    void linkedDivorceKeepsTheNikahUntouched() {
        DivorceRepository repo = mock(DivorceRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        NikahRequestRepository nikahs = mock(NikahRequestRepository.class);
        NikahRequest n = new NikahRequest();
        n.setId(9L);
        n.setGroomName("G");
        n.setBrideName("B");
        n.setStatus(NikahRequest.COMPLETED);
        when(nikahs.findByIdAndMasjidId(9L, 3L)).thenReturn(Optional.of(n));
        Input body = new Input(9L, "KHULA", null, null, null, null, null, null, null, LocalDate.now().minusDays(1), null, null, null, null, null, null, null, null);
        var v = service(repo, nikahs).create(imam(3), body);
        assertEquals(9L, v.nikahId());
        assertEquals("G", v.groomName());
        assertEquals(NikahRequest.COMPLETED, n.getStatus());
        verify(nikahs, never()).save(any());
    }

    @Test
    void anotherMasjidsNikahOrRecordIsNotFound() {
        DivorceRepository repo = mock(DivorceRepository.class);
        NikahRequestRepository nikahs = mock(NikahRequestRepository.class);
        when(repo.findByIdAndMasjidId(9L, 1L)).thenReturn(Optional.of(new Divorce()));
        when(nikahs.findByIdAndMasjidId(9L, 1L)).thenReturn(Optional.of(new NikahRequest()));
        DivorceService svc = service(repo, nikahs);
        AppUser other = imam(2);
        Input withNikah = new Input(9L, "KHULA", null, "A", "B", null, null, null, null, TODAY, null, null, null, null, null, null, null, null);
        for (Runnable call : List.<Runnable>of(() -> svc.get(other, 9L), () -> svc.voidRecord(other, 9L, "x"),
                () -> svc.pdf(other, 9L), () -> svc.update(other, 9L, talaq1()), () -> svc.byNikah(other, 9L),
                () -> svc.create(other, withNikah))) {
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, call::run).getStatus());
        }
        verify(repo, never()).save(any());
    }

    @Test
    void listIsScopedToTheImamsMasjid() {
        DivorceRepository repo = mock(DivorceRepository.class);
        when(repo.findByMasjidIdOrderByDivorceDateDescIdDesc(4L)).thenReturn(List.of());
        service(repo, mock(NikahRequestRepository.class)).list(imam(4));
        verify(repo).findByMasjidIdOrderByDivorceDateDescIdDesc(4L);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void voidedRecordIsLockedAndCannotBeVoidedAgain() {
        DivorceRepository repo = mock(DivorceRepository.class);
        Divorce d = new Divorce();
        d.setVoided(true);
        when(repo.findByIdAndMasjidId(9L, 1L)).thenReturn(Optional.of(d));
        DivorceService svc = service(repo, mock(NikahRequestRepository.class));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.voidRecord(imam(1), 9L, "again")).getStatus());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.update(imam(1), 9L, talaq1())).getStatus());
    }

    @Test
    void imamWithoutMasjidIsForbidden() {
        AppUser none = new AppUser();
        DivorceService svc = service(mock(DivorceRepository.class), mock(NikahRequestRepository.class));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> svc.list(none)).getStatus());
    }

    @Test
    void pdfIsGeneratedAndStatesItIsNotACivilDecree() {
        Letterhead.Info head = new Letterhead.Info("Masjid Al-Noor", "123 Main St", "555-1234", Color.decode("#4f7a00"), null, true);
        Divorce d = new Divorce();
        d.setRecordNumber("NG-DV-1-2026-00001");
        d.setDivorceType(DivorceType.TALAQ);
        d.setTalaqNumber(3);
        d.setGroomName("Omar Khan");
        d.setBrideName("Sara Ali");
        d.setDivorceDate(TODAY);
        d.setPrivateNotes("SECRET-NOTE");
        byte[] pdf = DivorcePdf.render(d, head, TODAY);
        assertEquals("%PDF-", new String(pdf, 0, 5));
        d.setVoided(true);
        assertEquals("%PDF-", new String(DivorcePdf.render(d, head, TODAY), 0, 5));
        assertTrue(DivorcePdf.DISCLAIMER.contains("does not replace a civil divorce decree"));
        assertFalse(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1).contains("SECRET-NOTE"));
    }

    @Test
    void serviceIsRegisteredInTheCatalog() {
        assertEquals(ServiceKey.DIVORCE, ServiceKey.forPath("/api/imam/svc/divorce/by-nikah/3").orElseThrow());
        assertTrue(ServiceKey.DIVORCE.available() && ServiceKey.DIVORCE.defaultEnabled());
    }

    private static void assertBad(Input r) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> DivorceRules.normalize(r, TODAY)).getStatus());
    }
}
