package org.nikahgator.certificate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.nikahgator.exception.ApiException;

class CertificateRenderTest {
    private final CertificateTemplateService svc = new CertificateTemplateService(null, null, null, "target/ng-test-uploads");

    @Test
    void replacesKnownAndDropsUnknownKeys() {
        assertEquals("Hello Ali, bye.", svc.render("Hello {{ name }}, bye {{nope}}.", Map.of("name", "Ali")));
    }

    @Test
    void injectionLookingValuesStayPlainAndAreNotRescanned() {
        Map<String, String> v = new HashMap<>();
        v.put("a", "{{b}} <script>alert(1)</script> $1 \\");
        v.put("b", "SECRET");
        assertEquals("X {{b}} <script>alert(1)</script> $1 \\", svc.render("X {{a}}", v));
    }

    @Test
    void controlCharactersAreStripped() {
        assertEquals("ab", svc.render("{{a}}", Map.of("a", "a\u0000\u0007b")));
    }

    @Test
    void emptyClausesAreDropped() {
        Map<String, String> v = new HashMap<>();
        v.put("groomName", "Yusuf");
        v.put("brideName", "Maryam");
        v.put("nikahDate", "Saturday, December 5, 2026");
        v.put("nikahTime", "2:30 PM");
        assertEquals("Yusuf with Maryam was solemnized according to the Islamic rites on Saturday, December 5, 2026 at 2:30 PM.",
                svc.render(CertificateTemplate.DEFAULT_BODY, v));
    }

    @Test
    void fullValuesKeepEverything() {
        Map<String, String> v = Map.of("groomName", "A", "groomFather", "B", "brideName", "C", "brideFather", "D", "nikahDate", "d",
                "nikahTime", "t", "mahr", "m", "wali", "W", "waliRelation", "Father", "witnesses", "X and Y");
        assertEquals("A son of B with C daughter of D was solemnized according to the Islamic rites on d at t, with mahr: m. "
                + "Wali: W (Father). Witnesses: X and Y.", svc.render(CertificateTemplate.DEFAULT_BODY, v));
    }

    @Test
    void colorValidation() {
        assertEquals("#4f7a00", CertificateTemplateService.validateColor(null));
        assertEquals("#abcdef", CertificateTemplateService.validateColor("#ABCDEF"));
        for (String bad : new String[] {"red", "#fff", "#12345g", "#1234567", "javascript:1", "#ffffff;x"}) {
            assertThrows(ApiException.class, () -> CertificateTemplateService.validateColor(bad), bad);
        }
    }

    @Test
    void paperValidation() {
        assertEquals("LETTER", CertificateTemplateService.validatePaper(""));
        assertEquals("A4", CertificateTemplateService.validatePaper("a4"));
        assertThrows(ApiException.class, () -> CertificateTemplateService.validatePaper("LEGAL"));
    }

    @Test
    void imageTypeIsDetectedFromBytes() {
        assertEquals("png", CertificateTemplateService.detectExtension(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a}));
        assertEquals("jpg", CertificateTemplateService.detectExtension(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0, 0, 0}));
        assertNull(CertificateTemplateService.detectExtension("<svg></svg>".getBytes()));
    }
}
