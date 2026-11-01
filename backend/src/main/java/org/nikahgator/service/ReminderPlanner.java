package org.nikahgator.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Works out which reminders a nikah needs and when they go out. Pure logic, no database. */
public final class ReminderPlanner {
    /** Reminders go out at this local time of the day (the masjid's time zone). */
    public static final LocalTime SEND_TIME = LocalTime.of(9, 0);

    public enum Audience { COUPLE, IMAM }

    public record Planned(String kind, Audience audience, int daysBefore, Instant sendAt) {}

    private ReminderPlanner() {}

    /**
     * @param nikahAt       the nikah date and time as written, in the masjid's local time
     * @param coupleEnabled whether the masjid sends reminders to the couple
     * @param daysBefore    the early couple reminder (a day-before reminder is always added)
     * @return reminders still in the future, earliest first; the imam always gets one the day before
     */
    public static List<Planned> plan(LocalDateTime nikahAt, ZoneId zone, Instant now, boolean coupleEnabled, int daysBefore) {
        List<Planned> out = new ArrayList<>();
        if (nikahAt == null) {
            return out;
        }
        Instant nikahInstant = nikahAt.atZone(zone).toInstant();
        List<Integer> coupleDays = new ArrayList<>();
        if (coupleEnabled) {
            if (daysBefore > 1) {
                coupleDays.add(daysBefore);
            }
            coupleDays.add(1);
        }
        for (int d : coupleDays) {
            add(out, "COUPLE_" + d + "D", Audience.COUPLE, d, nikahAt, zone, now, nikahInstant);
        }
        add(out, "IMAM_1D", Audience.IMAM, 1, nikahAt, zone, now, nikahInstant);
        out.sort((a, b) -> a.sendAt().compareTo(b.sendAt()));
        return out;
    }

    private static void add(List<Planned> out, String kind, Audience audience, int days, LocalDateTime nikahAt, ZoneId zone,
                            Instant now, Instant nikahInstant) {
        // ZonedDateTime moves 9:00 forward if a daylight saving gap swallows it
        Instant at = ZonedDateTime.of(nikahAt.toLocalDate().minusDays(days), SEND_TIME, zone).toInstant();
        if (at.isAfter(now) && at.isBefore(nikahInstant)) {
            out.add(new Planned(kind, audience, days, at));
        }
    }
}
