package org.nikahgator.mail;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.nikahgator.entity.NikahRequest;
import org.nikahgator.exception.ApiException;
import org.nikahgator.service.CertificateService;
import org.nikahgator.service.EmailService;

class DocumentMailerTest {

    @Test
    void acceptsOneToFiveAddressesTrimmedAndDeduplicated() {
        assertEquals(List.of("a@b.org", "c.d+x@mail.example.com"),
                DocumentMailer.parseRecipients(List.of(" a@b.org ", "c.d+x@mail.example.com", "a@b.org")));
        assertEquals(5, DocumentMailer.parseRecipients(List.of("1@a.org", "2@a.org", "3@a.org", "4@a.org", "5@a.org")).size());
    }

    @Test
    void rejectsBadMissingOrTooManyAddresses() {
        assertThrows(ApiException.class, () -> DocumentMailer.parseRecipients(List.of()));
        assertThrows(ApiException.class, () -> DocumentMailer.parseRecipients(null));
        assertThrows(ApiException.class, () -> DocumentMailer.parseRecipients(List.of("not-an-address")));
        assertThrows(ApiException.class, () -> DocumentMailer.parseRecipients(List.of("a@b")));
        assertThrows(ApiException.class, () -> DocumentMailer.parseRecipients(List.of("a@b.org\r\nBcc: x@y.org")));
        assertThrows(ApiException.class, () -> DocumentMailer.parseRecipients(List.of("1@a.org", "2@a.org", "3@a.org", "4@a.org", "5@a.org", "6@a.org")));
    }

    @Test
    void subjectAndBodyNameTheMasjidAndNumberAndKeepTheSendersNote() {
        var doc = new DocumentSource.Doc(1L, "Nikah certificate", "NC-1-2026-0001", "NC-1-2026-0001.pdf", new byte[0], false);
        assertEquals("Nikah certificate NC-1-2026-0001 from Masjid Noor", DocumentMailer.subject("Masjid Noor", doc));
        String body = DocumentMailer.body("Masjid Noor", doc, "Imam Ali", "Barakallahu lakum");
        assertTrue(body.contains("Masjid Noor"));
        assertTrue(body.contains("NC-1-2026-0001"));
        assertTrue(body.contains("Barakallahu lakum"));
        assertFalse(DocumentMailer.body("Masjid Noor", doc, "Imam Ali", null).contains("Message from"));
    }

    @Test
    void headerInjectionInNamesIsFlattened() {
        var doc = new DocumentSource.Doc(1L, "Letter", "L-1", "L-1.pdf", new byte[0], false);
        assertFalse(DocumentMailer.subject("Evil\r\nBcc: x@y.org", doc).contains("\n"));
    }

    @Test
    void secretsNeverReachTheStoredBody() {
        assertEquals("Your code is [hidden].", EmailService.redact(EmailService.OTP, "Your code is 123456.", "123456"));
        assertEquals("Password: [hidden]", EmailService.redact(EmailService.WELCOME, "Password: Tmp-9x", "Tmp-9x"));
        // a secret kind sent without naming its secret keeps no text at all
        assertEquals("[hidden]", EmailService.redact(EmailService.OTP, "Your code is 123456.", null));
        assertEquals("[hidden]", EmailService.redact(EmailService.WELCOME, "Password: Tmp-9x", ""));
        assertEquals("See you Friday", EmailService.redact(EmailService.REMINDER, "See you Friday", null));
    }

    @Test
    void nikahCertificatesNeedACompletedNikah() {
        NikahRequest n = new NikahRequest();
        n.setStatus(NikahRequest.COMPLETED);
        assertDoesNotThrow(() -> CertificateService.requireCompleted(n));
        n.setStatus("SCHEDULED");
        ApiException e = assertThrows(ApiException.class, () -> CertificateService.requireCompleted(n));
        assertEquals(409, e.getStatus().value());
    }
}
