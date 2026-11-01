package org.nikahgator.letters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The built-in letter types and their starting wording. Templates live in code; the imam edits the rendered text. */
public final class LetterTemplates {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9]*)\\s*}}");

    public record Placeholder(String key, String label) {
    }

    public record Type(String key, String label, String subject, String body) {
    }

    public static final List<Placeholder> PLACEHOLDERS = List.of(
            new Placeholder("recipientName", "Person the letter is about"),
            new Placeholder("addressee", "Who the letter is addressed to"),
            new Placeholder("purpose", "Purpose of the letter"),
            new Placeholder("date", "Letter date"),
            new Placeholder("imamName", "Imam's name"),
            new Placeholder("masjidName", "Masjid name"),
            new Placeholder("masjidAddress", "Masjid address"));

    public static final List<Type> TYPES = List.of(
            new Type("CHARACTER_REFERENCE", "Character reference",
                    "Character reference for {{recipientName}}",
                    "To {{addressee}},\n\n"
                            + "I am writing on behalf of {{masjidName}} to provide a character reference for {{recipientName}}. "
                            + "To my knowledge, {{recipientName}} is a member of good standing in our community and is known to be "
                            + "honest, respectful and trustworthy.\n\n"
                            + "This letter is provided for the following purpose: {{purpose}}.\n\n"
                            + "Please contact the masjid at {{masjidAddress}} should you need any further information.\n\n"
                            + "Sincerely,\n{{imamName}}\n{{masjidName}}"),
            new Type("NO_OBJECTION", "No-objection letter (NOC)",
                    "No-objection letter for {{recipientName}}",
                    "To {{addressee}},\n\n"
                            + "{{masjidName}} has no objection to {{recipientName}} with regard to the following: {{purpose}}.\n\n"
                            + "This letter is issued on {{date}} at the request of {{recipientName}} and does not create any "
                            + "other obligation for the masjid.\n\n"
                            + "Sincerely,\n{{imamName}}\n{{masjidName}}"),
            new Type("MUSLIM_VERIFICATION", "Verification of Muslim faith",
                    "Verification of Muslim faith for {{recipientName}}",
                    "To {{addressee}},\n\n"
                            + "This letter confirms that {{recipientName}} is known to {{masjidName}} as a practicing Muslim.\n\n"
                            + "This verification is issued on {{date}} for the following purpose: {{purpose}}.\n\n"
                            + "Sincerely,\n{{imamName}}\n{{masjidName}}"),
            new Type("EMPLOYER_SUPPORT", "Employer support letter",
                    "Letter to the employer of {{recipientName}}",
                    "To {{addressee}},\n\n"
                            + "I am writing on behalf of {{masjidName}} concerning {{recipientName}}. "
                            + "{{purpose}}\n\n"
                            + "We would be grateful for your kind consideration. Please contact us at {{masjidAddress}} "
                            + "if you need to confirm any detail.\n\n"
                            + "Sincerely,\n{{imamName}}\n{{masjidName}}"),
            new Type("TRAVEL_SUPPORT", "Travel support letter",
                    "Travel support letter for {{recipientName}}",
                    "To {{addressee}},\n\n"
                            + "{{recipientName}} is a member of the {{masjidName}} community. This letter supports the planned "
                            + "travel of {{recipientName}} for the following purpose: {{purpose}}.\n\n"
                            + "Issued on {{date}}.\n\n"
                            + "Sincerely,\n{{imamName}}\n{{masjidName}}"),
            new Type("VISA_SUPPORT", "Visa support letter",
                    "Visa support letter for {{recipientName}}",
                    "To {{addressee}},\n\n"
                            + "I write on behalf of {{masjidName}} in support of the visa application of {{recipientName}}. "
                            + "{{recipientName}} is known to our community and the purpose of the application is: {{purpose}}.\n\n"
                            + "Issued on {{date}}. Please contact the masjid at {{masjidAddress}} to verify this letter.\n\n"
                            + "Sincerely,\n{{imamName}}\n{{masjidName}}"),
            new Type("FREE_FORM", "Free-form letter",
                    "{{purpose}}",
                    "To {{addressee}},\n\n\n\nSincerely,\n{{imamName}}\n{{masjidName}}"));

    private LetterTemplates() {
    }

    public static Optional<Type> byKey(String key) {
        return TYPES.stream().filter(t -> t.key().equals(key)).findFirst();
    }

    /** The values behind the placeholders. Null or blank entries render as empty. */
    public static Map<String, String> values(String recipientName, String addressee, String purpose, String date,
            String imamName, String masjidName, String masjidAddress) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("recipientName", recipientName);
        v.put("addressee", addressee);
        v.put("purpose", purpose);
        v.put("date", date);
        v.put("imamName", imamName);
        v.put("masjidName", masjidName);
        v.put("masjidAddress", masjidAddress);
        return v;
    }

    /**
     * Replaces {{key}} with the plain-text value; unknown keys become empty and substituted values are never
     * re-scanned, so a value containing "{{x}}" stays as typed. Control characters are dropped, newlines kept.
     */
    public static String render(String text, Map<String, String> values) {
        if (text == null) return "";
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String v = values == null ? null : values.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : clean(v)));
        }
        m.appendTail(sb);
        return tidy(sb.toString());
    }

    static String clean(String s) {
        return s.replace("\r", "").replaceAll("[\\p{Cc}&&[^\\n]]", "").trim();
    }

    /** Fixes spacing and punctuation left by blank values; paragraph breaks are kept. */
    static String tidy(String s) {
        String r = s.replace("\r", "");
        r = r.replaceAll("[ \\t]{2,}", " ");
        r = r.replaceAll("[ \\t]+([,.;:])", "$1");
        r = r.replaceAll("[ \\t]+\\n", "\n");
        r = r.replaceAll("\\n{4,}", "\n\n\n");
        return r.trim();
    }
}
