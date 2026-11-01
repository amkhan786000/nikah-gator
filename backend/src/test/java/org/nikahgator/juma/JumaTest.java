package org.nikahgator.juma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.nikahgator.juma.JumaDtos.Request;
import org.nikahgator.juma.JumaDtos.StudentDto;
import org.nikahgator.letterhead.Letterhead;
import org.nikahgator.repository.MasjidRepository;
import org.springframework.http.HttpStatus;

class JumaTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

    private static Request req(boolean standing, LocalDate start, LocalDate end, LocalTime ws, LocalTime we, List<StudentDto> students) {
        return new Request(" Oak School ", "1 Main St", null, " Ms. Lee ", null, standing, start, end, ws, we, "  ", students);
    }

    private static final List<StudentDto> ONE = List.of(new StudentDto(" Ali Khan ", " 5 "));

    @Test
    void defaultsTheWindowAndLetterDateAndCleansText() {
        Request r = JumaRules.normalize(req(true, TODAY, LocalDate.of(2030, 1, 1), null, null, ONE), TODAY);
        assertEquals(LocalTime.of(12, 30), r.windowStart());
        assertEquals(LocalTime.of(14, 0), r.windowEnd());
        assertEquals(TODAY, r.letterDate());
        assertEquals("Oak School", r.schoolName());
        assertNull(r.note());
        assertEquals("Ali Khan", r.students().get(0).name());
        assertEquals("5", r.students().get(0).grade());
        assertNull(r.endDate(), "a standing request has no end date");
    }

    @Test
    void rangeNeedsAnOrderedEndDate() {
        assertEquals(HttpStatus.BAD_REQUEST, thrown(req(false, TODAY, null, null, null, ONE)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, thrown(req(false, TODAY, TODAY.minusDays(1), null, null, ONE)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, thrown(req(false, TODAY, TODAY.plusYears(3), null, null, ONE)).getStatus());
        assertDoesNotThrow(() -> JumaRules.normalize(req(false, TODAY, TODAY.plusMonths(9), null, null, ONE), TODAY));
    }

    @Test
    void windowMustBeOrderedAndStudentsRequired() {
        assertThrows(ApiException.class, () -> JumaRules.normalize(
                req(true, TODAY, null, LocalTime.of(14, 0), LocalTime.of(12, 30), ONE), TODAY));
        assertThrows(ApiException.class, () -> JumaRules.normalize(req(true, TODAY, null, null, null, List.of()), TODAY));
        assertThrows(ApiException.class, () -> JumaRules.normalize(
                req(true, TODAY, null, null, null, List.of(new StudentDto("  ", "3"))), TODAY));
    }

    @Test
    void wording() {
        assertEquals("beginning September 1, 2026 and continuing until further notice", JumaRules.period(true, TODAY, null));
        assertEquals("from September 1, 2026 through June 5, 2027", JumaRules.period(false, TODAY, LocalDate.of(2027, 6, 5)));
        assertEquals("12:30 PM and 2:00 PM", JumaRules.window(LocalTime.of(12, 30), LocalTime.of(14, 0)).replace(' ', ' '));
    }

    @Test
    void anotherMasjidsLetterIsNotFound() {
        JumaLetterRepository repo = mock(JumaLetterRepository.class);
        when(repo.findByIdAndMasjidId(7L, 1L)).thenReturn(Optional.of(new JumaLetter()));
        when(repo.findByIdAndMasjidId(7L, 2L)).thenReturn(Optional.empty());
        JumaService svc = new JumaService(repo, mock(MasjidRepository.class), mock(Letterhead.class));
        AppUser other = new AppUser();
        other.setMasjidId(2L);
        for (Runnable call : List.<Runnable>of(() -> svc.get(other, 7L), () -> svc.voidLetter(other, 7L, "x"),
                () -> svc.pdf(other, 7L), () -> svc.update(other, 7L, req(true, TODAY, null, null, null, ONE)))) {
            ApiException e = assertThrows(ApiException.class, call::run);
            assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        }
        verify(repo, never()).save(any());
    }

    @Test
    void voidedLetterCannotBeEditedOrVoidedAgain() {
        JumaLetterRepository repo = mock(JumaLetterRepository.class);
        JumaLetter l = new JumaLetter();
        l.setStatus(JumaLetter.VOID);
        when(repo.findByIdAndMasjidId(7L, 1L)).thenReturn(Optional.of(l));
        JumaService svc = new JumaService(repo, mock(MasjidRepository.class), mock(Letterhead.class));
        AppUser imam = new AppUser();
        imam.setMasjidId(1L);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class,
                () -> svc.update(imam, 7L, req(true, TODAY, null, null, null, ONE))).getStatus());
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.voidLetter(imam, 7L, "again")).getStatus());
    }

    @Test
    void pdfIsGeneratedForActiveAndVoidLetters() {
        Letterhead.Info head = new Letterhead.Info("Masjid Al-Noor", "123 Main St, Gainesville, FL", "555-1234", Color.decode("#4f7a00"), null, false);
        JumaLetter l = new JumaLetter();
        l.setSchoolName("Oak School");
        l.setAddressee("Ms. Lee");
        l.setLetterDate(TODAY);
        l.setStartDate(TODAY);
        l.setWindowStart(LocalTime.of(12, 30));
        l.setWindowEnd(LocalTime.of(14, 0));
        l.setStanding(true);
        l.getStudents().add(new JumaLetter.Student("Ali Khan", "5"));
        byte[] active = JumaPdf.render(l, head, "Imam Abdullah");
        assertEquals("%PDF-", new String(active, 0, 5));
        l.setStatus(JumaLetter.VOID);
        assertEquals("%PDF-", new String(JumaPdf.render(l, head, "Imam Abdullah"), 0, 5));
    }

    private static ApiException thrown(Request r) {
        return assertThrows(ApiException.class, () -> JumaRules.normalize(r, TODAY));
    }
}
