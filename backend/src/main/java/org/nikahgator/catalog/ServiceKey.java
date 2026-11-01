package org.nikahgator.catalog;

import java.util.Arrays;
import java.util.Optional;

/**
 * Everything an imam can offer. The platform admin switches each one on or off per masjid and per imam.
 * <p>
 * Services other than NIKAH keep their API under {@code /api/imam/svc/<slug>/...}; the guard maps that
 * slug to the key, so a new service is protected as soon as it is listed here.
 */
public enum ServiceKey {
    NIKAH("nikah", "Nikah records & certificates",
            "Register nikahs, checklist, documents, fees, reminders and the nikah certificate.", "mdi-ring", true, true),
    // ---
    JUMA_LETTER("juma", "Juma request letters",
            "Letters for schools to excuse students for Friday (Juma) prayer.", "mdi-school-outline", true, true),
    // ---
    COMMUNITY_SERVICE("community-service", "Community service certificates",
            "Certificates of service hours for students and volunteers.", "mdi-hand-heart", true, true),
    // ---
    JANAZAH("janazah", "Janazah (funeral) records",
            "Record funeral prayers and issue a letter for the family.", "mdi-flower-outline", true, false),
    // ---
    SHAHADA("shahada", "New Muslim (Shahada) certificates",
            "Certificate for someone who has taken their shahada.", "mdi-star-circle", true, false),
    // ---
    BABY_NAMING("baby-naming", "Baby naming & Aqeeqah",
            "Certificate for a baby's naming and aqeeqah.", "mdi-baby-face-outline", true, false),
    // ---
    COUNSELING("counseling", "Counseling sessions",
            "Private log of marriage, family and personal counseling sessions.", "mdi-account-heart-outline", true, false),
    // ---
    HALL_BOOKING("hall-booking", "Hall & event booking",
            "Bookings of the masjid hall and events, without double-booking.", "mdi-calendar-check-outline", true, false),
    // ---
    GENERAL_LETTER("letters", "General letters",
            "Character references, no-objection and other letters on masjid letterhead.", "mdi-email-newsletter", true, false);

    private final String slug;
    private final String label;
    private final String description;
    private final String icon;
    private final boolean defaultEnabled;
    /** False until the service's pages and API exist, so the admin and the menu do not offer it yet. */
    private final boolean available;

    ServiceKey(String slug, String label, String description, String icon, boolean defaultEnabled, boolean available) {
        this.slug = slug;
        this.label = label;
        this.description = description;
        this.icon = icon;
        this.defaultEnabled = defaultEnabled;
        this.available = available;
    }

    public String slug() { return slug; }
    public String label() { return label; }
    public String description() { return description; }
    public String icon() { return icon; }
    public boolean defaultEnabled() { return defaultEnabled; }
    public boolean available() { return available; }

    public static Optional<ServiceKey> bySlug(String slug) {
        return Arrays.stream(values()).filter(k -> k.slug.equals(slug)).findFirst();
    }

    /**
     * The service an {@code /api/imam/...} path belongs to, or empty if the path is not service-specific
     * (for example /api/imam/catalog).
     */
    public static Optional<ServiceKey> forPath(String path) {
        String p = path == null ? "" : path;
        if (p.startsWith("/api/imam/svc/")) {
            String rest = p.substring("/api/imam/svc/".length());
            int slash = rest.indexOf('/');
            return bySlug(slash < 0 ? rest : rest.substring(0, slash));
        }
        for (String prefix : NIKAH_PREFIXES) {
            if (p.equals(prefix) || p.startsWith(prefix + "/")) {
                return Optional.of(NIKAH);
            }
        }
        return Optional.empty();
    }

    /** The nikah module was built before the catalog, so its endpoints keep their original paths. */
    private static final String[] NIKAH_PREFIXES = {
            "/api/imam/nikahs", "/api/imam/checklist-progress", "/api/imam/payments", "/api/imam/masjid",
            "/api/imam/certificate-template"
    };
}
