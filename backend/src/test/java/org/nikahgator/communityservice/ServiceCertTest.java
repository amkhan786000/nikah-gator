package org.nikahgator.communityservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.nikahgator.communityservice.ServiceCertDtos.Request;
import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.nikahgator.letterhead.Letterhead;
import org.nikahgator.repository.MasjidRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

class ServiceCertTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

    private static Request req(String type, LocalDate s, LocalDate e, String hours) {
        return new Request(" Sara Ali ", type, " Oak High ", " Cleaning the hall ", s, e, new BigDecimal(hours), " Br. Omar ", null);
    }

    @Test
    void numberFormatAndParsing() {
        assertEquals("NG-CS-12-2026-00042", ServiceCertNumbers.format(12, 2026, 42));
        assertEquals("NG-CS-1-2026-123456", ServiceCertNumbers.format(1, 2026, 123456));
        assertThrows(IllegalArgumentException.class, () -> ServiceCertNumbers.format(0, 2026, 1));
        assertThrows(IllegalArgumentException.class, () -> ServiceCertNumbers.format(1, 2026, 0));
        assertTrue(ServiceCertNumbers.isValid("NG-CS-12-2026-00042"));
        assertFalse(ServiceCertNumbers.isValid("NG-12-2026-00042"), "a nikah number is not a service number");
        assertFalse(ServiceCertNumbers.isValid("NG-CS-12-2026-42"));
        assertArrayEquals(new long[] { 12, 2026, 42 }, ServiceCertNumbers.parse("NG-CS-12-2026-00042"));
        assertNull(ServiceCertNumbers.parse("junk"));
    }

    @Test
    void normalizesAndRoundsHours() {
        Request r = ServiceCertRules.normalize(req("student", TODAY.minusDays(10), TODAY, "12.456"), TODAY);
        assertEquals("STUDENT", r.personType());
        assertEquals(new BigDecimal("12.46"), r.hours());
        assertEquals("Sara Ali", r.personName());
        assertEquals("Oak High", r.organization());
    }

    @Test
    void rejectsBadInput() {
        assertBad(req("TEACHER", TODAY, TODAY, "5"));
        assertBad(req("STUDENT", TODAY, TODAY.minusDays(1), "5"));
        assertBad(req("STUDENT", TODAY, TODAY.plusDays(1), "5"));
        assertBad(req("STUDENT", TODAY.minusYears(6), TODAY, "5"));
        assertBad(req("STUDENT", TODAY, TODAY, "0"));
        assertBad(req("STUDENT", TODAY, TODAY, "2000.01"));
        assertBad(new Request("  ", "STUDENT", null, "x", TODAY, TODAY, BigDecimal.ONE, "y", null));
    }

    @Test
    void hoursWording() {
        assertEquals("1 hour", ServiceCertRules.hoursText(new BigDecimal("1.00")));
        assertEquals("12.5 hours", ServiceCertRules.hoursText(new BigDecimal("12.50")));
        assertEquals("100 hours", ServiceCertRules.hoursText(new BigDecimal("100.00")));
    }

    @Test
    void createAssignsTheNextNumberOfTheMasjidAndYear() {
        ServiceCertRepository repo = mock(ServiceCertRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        MasjidRepository masjids = mock(MasjidRepository.class);
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbc.sql(anyString()).param(anyString(), any()).param(anyString(), any()).query(Integer.class).single()).thenReturn(7);
        ServiceCertService svc = new ServiceCertService(repo, masjids, mock(Letterhead.class), jdbc);
        AppUser imam = new AppUser();
        imam.setId(5L);
        imam.setMasjidId(3L);
        // the masjid lookup falls back to the system clock, so build the dates from it
        LocalDate today = LocalDate.now();
        var view = svc.create(imam, req("VOLUNTEER", today.minusDays(3), today, "4"));
        assertEquals("NG-CS-3-" + today.getYear() + "-00007", view.certNumber());
        assertEquals("VALID", view.status());
    }

    @Test
    void anotherMasjidsCertificateIsNotFound() {
        ServiceCertRepository repo = mock(ServiceCertRepository.class);
        when(repo.findByIdAndMasjidId(9L, 1L)).thenReturn(Optional.of(new ServiceCert()));
        ServiceCertService svc = new ServiceCertService(repo, mock(MasjidRepository.class), mock(Letterhead.class), mock(JdbcClient.class));
        AppUser other = new AppUser();
        other.setMasjidId(2L);
        for (Runnable call : List.<Runnable>of(() -> svc.get(other, 9L), () -> svc.voidCert(other, 9L, "x"),
                () -> svc.pdf(other, 9L), () -> svc.update(other, 9L, req("STUDENT", TODAY, TODAY, "1")))) {
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, call::run).getStatus());
        }
        verify(repo, never()).save(any());
    }

    @Test
    void voidedCertificateIsLocked() {
        ServiceCertRepository repo = mock(ServiceCertRepository.class);
        ServiceCert c = new ServiceCert();
        c.setStatus(ServiceCert.VOID);
        when(repo.findByIdAndMasjidId(9L, 1L)).thenReturn(Optional.of(c));
        ServiceCertService svc = new ServiceCertService(repo, mock(MasjidRepository.class), mock(Letterhead.class), mock(JdbcClient.class));
        AppUser imam = new AppUser();
        imam.setMasjidId(1L);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> svc.voidCert(imam, 9L, "again")).getStatus());
    }

    @Test
    void pdfIsGeneratedForValidAndVoidCertificates() {
        Letterhead.Info head = new Letterhead.Info("Masjid Al-Noor", "123 Main St", "555-1234", Color.decode("#4f7a00"), null, true);
        ServiceCert c = new ServiceCert();
        c.setCertNumber("NG-CS-1-2026-00001");
        c.setPersonName("Sara Ali");
        c.setPersonType(ServiceCert.STUDENT);
        c.setOrganization("Oak High");
        c.setActivities("Cleaning\nServing meals");
        c.setStartDate(TODAY.minusDays(5));
        c.setEndDate(TODAY);
        c.setHours(new BigDecimal("12.50"));
        c.setSupervisor("Br. Omar");
        c.setIssueDate(TODAY);
        assertEquals("%PDF-", new String(ServiceCertPdf.render(c, head, "Imam Abdullah"), 0, 5));
        c.setStatus(ServiceCert.VOID);
        assertEquals("%PDF-", new String(ServiceCertPdf.render(c, head, null), 0, 5));
    }

    private static void assertBad(Request r) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> ServiceCertRules.normalize(r, TODAY)).getStatus());
    }
}
