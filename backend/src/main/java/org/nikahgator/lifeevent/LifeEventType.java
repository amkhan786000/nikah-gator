package org.nikahgator.lifeevent;

import java.util.Arrays;
import java.util.Optional;

import org.nikahgator.catalog.ServiceKey;

/** The three life-event services; each has its own number prefix and API slug. */
public enum LifeEventType {
    JANAZAH("JZ", ServiceKey.JANAZAH, "Janazah Letter"),
    SHAHADA("SH", ServiceKey.SHAHADA, "Certificate of Shahada"),
    BABY_NAMING("BN", ServiceKey.BABY_NAMING, "Certificate of Naming and Aqeeqah");

    private final String code;
    private final ServiceKey service;
    private final String title;

    LifeEventType(String code, ServiceKey service, String title) {
        this.code = code;
        this.service = service;
        this.title = title;
    }

    public String code() { return code; }
    public ServiceKey service() { return service; }
    public String slug() { return service.slug(); }
    public String title() { return title; }

    public static Optional<LifeEventType> bySlug(String slug) {
        return Arrays.stream(values()).filter(t -> t.slug().equals(slug)).findFirst();
    }
}
