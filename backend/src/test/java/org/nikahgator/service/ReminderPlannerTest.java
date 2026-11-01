package org.nikahgator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nikahgator.service.ReminderPlanner.Planned;

class ReminderPlannerTest {
    private static final ZoneId NY = ZoneId.of("America/New_York");
    private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

    private static Instant at(String local, ZoneId zone) {
        return LocalDateTime.parse(local).atZone(zone).toInstant();
    }

    @Test
    void farAwayNikahGetsSevenDayAndOneDayCoupleAndImamReminders() {
        List<Planned> p = ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), NY, at("2027-04-01T12:00", NY), true, 7);
        assertEquals(List.of("COUPLE_7D", "COUPLE_1D", "IMAM_1D"), p.stream().map(Planned::kind).toList());
        assertEquals(at("2027-05-08T09:00", NY), p.get(0).sendAt());
        assertEquals("COUPLE_7D", p.get(0).kind());
        assertEquals(at("2027-05-14T09:00", NY), p.get(1).sendAt());
        assertEquals(at("2027-05-14T09:00", NY), p.get(2).sendAt());
    }

    @Test
    void sendsAtNineLocalTimeOfTheMasjidTimezone() {
        List<Planned> ny = ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), NY, at("2027-05-01T00:00", NY), true, 7);
        List<Planned> la = ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), LA, at("2027-05-01T00:00", LA), true, 7);
        ZonedDateTime nyOneDay = ny.get(1).sendAt().atZone(NY);
        ZonedDateTime laOneDay = la.get(1).sendAt().atZone(LA);
        assertEquals(9, nyOneDay.getHour());
        assertEquals(9, laOneDay.getHour());
        assertEquals(3, Duration3h(ny.get(1).sendAt(), la.get(1).sendAt()));
    }

    private static long Duration3h(Instant a, Instant b) {
        return java.time.Duration.between(a, b).toHours();
    }

    @Test
    void skipsRemindersWhoseTimeIsAlreadyPast() {
        // registered 3 days before: no 7-day reminder, the day-before ones remain
        List<Planned> p = ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), NY, at("2027-05-12T10:00", NY), true, 7);
        assertEquals(List.of("COUPLE_1D", "IMAM_1D"), p.stream().map(Planned::kind).toList());
        // registered the day before after 9:00: nothing left to schedule
        assertTrue(ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), NY, at("2027-05-14T09:30", NY), true, 7).isEmpty());
        // nikah already in the past
        assertTrue(ReminderPlanner.plan(LocalDateTime.parse("2020-05-15T14:00"), NY, at("2027-05-14T09:30", NY), true, 7).isEmpty());
    }

    @Test
    void coupleRemindersOffLeavesOnlyTheImam() {
        List<Planned> p = ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), NY, at("2027-04-01T00:00", NY), false, 7);
        assertEquals(List.of("IMAM_1D"), p.stream().map(Planned::kind).toList());
    }

    @Test
    void oneDaySettingGivesASingleCoupleReminder() {
        List<Planned> p = ReminderPlanner.plan(LocalDateTime.parse("2027-05-15T14:00"), NY, at("2027-04-01T00:00", NY), true, 1);
        assertEquals(2, p.size());
        assertEquals(1, p.stream().filter(x -> x.kind().startsWith("COUPLE")).count());
    }

    @Test
    void daylightSavingChangeKeepsNineLocal() {
        // US clocks go forward on 2027-03-14; a nikah on 03-15 gets its day-before reminder at 9:00 EDT
        List<Planned> p = ReminderPlanner.plan(LocalDateTime.parse("2027-03-15T14:00"), NY, at("2027-03-01T00:00", NY), true, 7);
        Planned oneDay = p.stream().filter(x -> x.kind().equals("COUPLE_1D")).findFirst().orElseThrow();
        assertEquals(at("2027-03-14T09:00", NY), oneDay.sendAt());
        assertEquals(13, oneDay.sendAt().atZone(ZoneId.of("UTC")).getHour());
    }
}
