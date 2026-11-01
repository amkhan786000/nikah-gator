package org.nikahgator.lifeevent;

import java.time.LocalDate;

import org.nikahgator.exception.ApiException;
import org.nikahgator.lifeevent.LifeEventDtos.Input;
import org.springframework.http.HttpStatus;

/** What each service requires and which dates make sense. Pure functions, no database. */
public final class LifeEventRules {
    private LifeEventRules() {
    }

    /** Returns the input with text cleaned (trimmed, control characters removed, blank -> null); throws 400 when invalid. */
    public static Input normalize(LifeEventType type, Input in, LocalDate today) {
        Input c = new Input(clean(in.personName()), clean(in.altName()), in.eventDate(), in.secondaryDate(), in.secondaryTime(),
                clean(in.place()), in.burialDate(), in.burialTime(), clean(in.burialPlace()), clean(in.cemetery()),
                clean(in.contactName()), clean(in.contactPhone()), clean(in.parents()), clean(in.witnesses()),
                clean(in.imamName()), clean(in.notes()));
        if (c.personName() == null) throw bad(nameLabel(type) + " is required.");
        if (c.eventDate() == null) throw bad(dateLabel(type) + " is required.");
        switch (type) {
            case JANAZAH -> {
                if (c.eventDate().isAfter(today)) throw bad("The date of death cannot be in the future.");
                if (c.secondaryDate() == null) throw bad("The date of the janazah is required.");
                if (c.secondaryDate().isBefore(c.eventDate())) throw bad("The janazah cannot be before the date of death.");
                if (c.burialDate() != null && c.burialDate().isBefore(c.secondaryDate())) throw bad("The burial cannot be before the janazah.");
            }
            case SHAHADA -> {
                if (c.eventDate().isAfter(today)) throw bad("The date of shahada cannot be in the future.");
                if (c.witnesses() == null) throw bad("At least one witness is required.");
            }
            case BABY_NAMING -> {
                if (c.eventDate().isAfter(today)) throw bad("The birth date cannot be in the future.");
                if (c.parents() == null) throw bad("The parents' names are required.");
                if (c.secondaryDate() == null) throw bad("The date of naming is required.");
                if (c.secondaryDate().isBefore(c.eventDate())) throw bad("The naming cannot be before the birth date.");
            }
        }
        return c;
    }

    public static String nameLabel(LifeEventType type) {
        return switch (type) {
            case JANAZAH -> "The name of the deceased";
            case SHAHADA -> "The person's name";
            case BABY_NAMING -> "The child's name";
        };
    }

    public static String dateLabel(LifeEventType type) {
        return switch (type) {
            case JANAZAH -> "The date of death";
            case SHAHADA -> "The date of shahada";
            case BABY_NAMING -> "The birth date";
        };
    }

    static String clean(String s) {
        if (s == null) return null;
        String r = s.replace("\r", "").replaceAll("[\\p{Cc}&&[^\\n]]", "").trim();
        return r.isEmpty() ? null : r;
    }

    private static ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
