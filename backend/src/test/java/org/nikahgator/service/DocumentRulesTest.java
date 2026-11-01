package org.nikahgator.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.nikahgator.exception.ApiException;

class DocumentRulesTest {

    @Test
    void sanitizeKeepsOnlyASafeLastSegment() {
        assertEquals("passwd", DocumentService.sanitize("../../etc/passwd"));
        assertEquals("evil.pdf", DocumentService.sanitize("C:\\temp\\evil.pdf"));
        assertEquals("a_b_.pdf", DocumentService.sanitize("a<b>.pdf"));
        assertEquals("file", DocumentService.sanitize(null));
        assertEquals("file", DocumentService.sanitize("..."));
        assertTrue(DocumentService.sanitize("x".repeat(300) + ".pdf").length() <= 100);
        assertTrue(DocumentService.sanitize("x".repeat(300) + ".pdf").endsWith(".pdf"));
    }

    @Test
    void typeComesFromContentAndMustMatchExtension() {
        assertEquals("application/pdf", DocumentService.detectType("%PDF-1.7".getBytes(), "a.PDF"));
        assertEquals("image/jpeg", DocumentService.detectType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}, "a.jpeg"));
        assertEquals("image/png", DocumentService.detectType(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, "a.png"));
        assertThrows(ApiException.class, () -> DocumentService.detectType("%PDF-1.7".getBytes(), "a.png"));
        assertThrows(ApiException.class, () -> DocumentService.detectType("<html>".getBytes(), "a.pdf"));
        assertThrows(ApiException.class, () -> DocumentService.detectType("%PDF-1.7".getBytes(), "noext"));
    }
}
