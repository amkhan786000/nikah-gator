package org.nikahgator.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.NikahRequest;

/** Plain-text wording of the reminder e-mails. */
public final class ReminderMessages {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US);

    public record Message(String subject, String body) {}

    private ReminderMessages() {}

    public static Message confirmation(Masjid m, NikahRequest n, boolean rescheduled) {
        String when = when(n.getScheduledAt());
        return new Message((rescheduled ? "Your nikah has been updated: " : "Your nikah is scheduled: ") + when,
                greeting(couple(n)) + (rescheduled ? "Your nikah details at " : "Your nikah is scheduled at ") + m.getName()
                        + " on " + when + "." + closing(m));
    }

    public static Message couple(Masjid m, NikahRequest n, int daysBefore) {
        String when = when(n.getScheduledAt());
        String lead = daysBefore == 1 ? "Your nikah is tomorrow" : "Your nikah is in " + daysBefore + " days";
        return new Message(lead + " at " + m.getName(),
                greeting(couple(n)) + "A reminder: " + Character.toLowerCase(lead.charAt(0)) + lead.substring(1) + " at " + m.getName() + ", " + when + "." + closing(m));
    }

    public static Message imam(Masjid m, NikahRequest n, String imamName) {
        String when = when(n.getScheduledAt());
        String phones = Stream.of(n.getGroomPhone(), n.getBridePhone()).filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(", "));
        return new Message("Tomorrow: nikah of " + n.getGroomName() + " and " + n.getBrideName(),
                greeting(imamName) + "A reminder: tomorrow you have a nikah at " + m.getName() + ".\n\n"
                        + "Couple: " + n.getGroomName() + " and " + n.getBrideName() + "\n"
                        + "When: " + when
                        + (phones.isEmpty() ? "" : "\nCouple's phone: " + phones)
                        + "\n\nMay Allah make it easy.\n\nNikah Gator");
    }

    public static Message adminMasjidCreated(String masjidName, String imamName, String imamEmail) {
        return new Message("Masjid " + masjidName + " created",
                "Masjid " + masjidName + " was created with imam " + imamName + " (" + imamEmail + ").");
    }

    private static String greeting(String names) {
        return "Assalamu alaikum " + names + ",\n\n";
    }

    private static String couple(NikahRequest n) {
        return n.getGroomName() + " and " + n.getBrideName();
    }

    private static String closing(Masjid m) {
        StringBuilder sb = new StringBuilder("\n\nMasjid: ").append(m.getName());
        String address = Stream.of(m.getAddressLine(), m.getCity(), join(m.getState(), m.getZip()))
                .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(", "));
        if (!address.isEmpty()) {
            sb.append("\nAddress: ").append(address);
        }
        if (m.getPhone() != null && !m.getPhone().isBlank()) {
            sb.append("\nPhone: ").append(m.getPhone());
        }
        if (m.getEmail() != null && !m.getEmail().isBlank()) {
            sb.append("\nE-mail: ").append(m.getEmail());
        }
        return sb.append("\n\nIf anything needs to change, please contact the masjid. May Allah bless your marriage.\n\n")
                .append(m.getName()).toString();
    }

    private static String join(String state, String zip) {
        return Stream.of(state, zip).filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(" "));
    }

    private static String when(LocalDateTime t) {
        return t == null ? "" : WHEN.format(t);
    }
}
