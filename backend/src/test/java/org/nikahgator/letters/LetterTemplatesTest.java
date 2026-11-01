package org.nikahgator.letters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class LetterTemplatesTest {

    private static Map<String, String> values() {
        return LetterTemplates.values("Yusuf Ahmed", "Admissions Office", "a graduate program application",
                "March 3, 2027", "Imam Abdullah", "Masjid Al-Noor", "123 Main St, Gainesville, FL 32601");
    }

    @Test
    void everyTypeRendersWithoutLeftoverPlaceholders() {
        for (LetterTemplates.Type t : LetterTemplates.TYPES) {
            String subject = LetterTemplates.render(t.subject(), values());
            String body = LetterTemplates.render(t.body(), values());
            assertFalse(subject.contains("{{") || body.contains("{{"), t.key());
            assertTrue(body.contains("Imam Abdullah"), t.key());
            assertTrue(body.contains("Masjid Al-Noor"), t.key());
        }
    }

    @Test
    void characterReferenceFillsRecipientAndPurpose() {
        String body = LetterTemplates.render(LetterTemplates.byKey("CHARACTER_REFERENCE").orElseThrow().body(), values());
        assertTrue(body.startsWith("To Admissions Office,"));
        assertTrue(body.contains("character reference for Yusuf Ahmed"));
        assertTrue(body.contains("a graduate program application."));
    }

    @Test
    void unknownKeysBecomeEmptyAndValuesAreNotRescanned() {
        Map<String, String> v = LetterTemplates.values("{{imamName}}", "", "", "", "Imam A", "M", "");
        assertEquals("Hi {{imamName}}!", LetterTemplates.render("Hi {{recipientName}}{{nope}}!", v));
        assertEquals("", LetterTemplates.render(null, v));
    }

    @Test
    void blankValuesDoNotLeaveStraySpacesBeforePunctuationAndKeepParagraphs() {
        String out = LetterTemplates.render("Dear {{addressee}} ,\n\nLine  two {{purpose}} .\n\n\n\n\nEnd", values("", "x"));
        assertEquals("Dear x,\n\nLine two.\n\n\nEnd", out);
    }

    @Test
    void controlCharactersInValuesAreDropped() {
        Map<String, String> v = LetterTemplates.values("Bad\u0007 Name\r\n", "", "", "", "", "", "");
        assertEquals("Bad Name", LetterTemplates.render("{{recipientName}}", v));
    }

    @Test
    void lookupByKey() {
        assertTrue(LetterTemplates.byKey("FREE_FORM").isPresent());
        assertTrue(LetterTemplates.byKey("nope").isEmpty());
        assertEquals(7, LetterTemplates.TYPES.size());
    }

    private static Map<String, String> values(String purpose, String addressee) {
        return LetterTemplates.values("", addressee, purpose, "", "", "", "");
    }
}
