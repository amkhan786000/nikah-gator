package org.nikahgator.salah;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nikahgator.salah.SalahRanges.Range;

class SalahTest {

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    @Test
    void newPeriodTakesItsDaysFromTheOldOnes() {
        // untouched
        assertEquals(List.of(new Range(d("2026-11-01"), d("2026-11-10"))),
                SalahRanges.remainder(d("2026-11-01"), d("2026-11-10"), d("2026-11-11"), d("2026-11-20")));
        // swallowed
        assertTrue(SalahRanges.remainder(d("2026-11-05"), d("2026-11-08"), d("2026-11-01"), d("2026-11-10")).isEmpty());
        // overlap on the right / left
        assertEquals(List.of(new Range(d("2026-11-01"), d("2026-11-04"))),
                SalahRanges.remainder(d("2026-11-01"), d("2026-11-10"), d("2026-11-05"), d("2026-11-20")));
        assertEquals(List.of(new Range(d("2026-11-11"), d("2026-11-20"))),
                SalahRanges.remainder(d("2026-11-05"), d("2026-11-20"), d("2026-11-01"), d("2026-11-10")));
        // lands in the middle: two pieces
        assertEquals(List.of(new Range(d("2026-11-01"), d("2026-11-09")), new Range(d("2026-11-13"), d("2026-11-30"))),
                SalahRanges.remainder(d("2026-11-01"), d("2026-11-30"), d("2026-11-10"), d("2026-11-12")));
    }

    @Test
    void suggestsTheNextPeriodFromHowOftenTheTimesChange() {
        assertEquals(d("2026-11-30"), SalahRanges.suggestedEnd(d("2026-11-01"), "MONTHS", 1));
        assertEquals(d("2026-12-31"), SalahRanges.suggestedEnd(d("2026-11-01"), "MONTHS", 2));
        assertEquals(d("2026-11-10"), SalahRanges.suggestedEnd(d("2026-11-01"), "DAYS", 10));
        assertEquals(d("2026-11-14"), SalahRanges.suggestedEnd(d("2026-11-01"), "WEEKS", 2));
        assertEquals(d("2026-11-01"), SalahRanges.suggestedEnd(d("2026-11-01"), "DAYS", 1));
    }

    @Test
    void readsTimesWithAndWithoutAmPm() {
        assertEquals(LocalTime.of(5, 23), SalahCsv.parseTime("fajr", "5:23"));
        assertEquals(LocalTime.of(5, 23), SalahCsv.parseTime("fajr", "05:23 AM"));
        assertEquals(LocalTime.of(12, 5), SalahCsv.parseTime("dhuhr", "12:05"));
        assertEquals(LocalTime.of(13, 15), SalahCsv.parseTime("dhuhr", "1:15"));
        assertEquals(LocalTime.of(15, 40), SalahCsv.parseTime("asr", "3:40"));
        assertEquals(LocalTime.of(17, 10), SalahCsv.parseTime("maghrib", "5:10 PM"));
        assertEquals(LocalTime.of(19, 45), SalahCsv.parseTime("isha", "19:45"));
        assertEquals(LocalTime.of(20, 5), SalahCsv.parseTime("isha", "8:05"));
        assertNull(SalahCsv.parseTime("fajr", "25:00"));
        assertNull(SalahCsv.parseTime("fajr", "soon"));
    }

    @Test
    void readsARangedCsvWithLooseHeaders() {
        String csv = "Start Date,End Date,Fajr,Shuruq,Zuhr,Asr,Magrib,Isha,Jummah\n"
                + "11/1/2026,11/10/2026,5:41,6:58,12:05,2:45,5:10,6:30,1:15\n"
                + "11/11/2026,11/20/2026,5:48,7:04,12:07,2:42,5:05,6:26,1:15\n";
        SalahCsv.Result r = SalahCsv.parse(csv);
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertEquals(2, r.rows().size());
        SalahCsv.Row first = r.rows().get(0);
        assertEquals(d("2026-11-01"), first.start());
        assertEquals(d("2026-11-10"), first.end());
        assertEquals(LocalTime.of(14, 45), first.asr());
        assertEquals(LocalTime.of(18, 30), first.isha());
        assertEquals(List.of(new SalahCsv.Jumuah(LocalTime.of(13, 15), "")), first.jumuah());
    }

    @Test
    void readsADailyCsvWithSemicolonsAndNoHeader() {
        String csv = "2026-11-01;05:41;06:58;12:05;14:45;17:10;18:30\n2026-11-02;05:42;06:59;12:05;14:44;17:09;18:29\n";
        SalahCsv.Result r = SalahCsv.parse(csv);
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertEquals(2, r.rows().size());
        assertEquals(r.rows().get(0).start(), r.rows().get(0).end());
    }

    @Test
    void explainsWhatIsWrongInsteadOfGuessing() {
        SalahCsv.Result missing = SalahCsv.parse("date,fajr,dhuhr\n2026-11-01,5:41,12:05\n");
        assertFalse(missing.errors().isEmpty());
        SalahCsv.Result badDate = SalahCsv.parse("date,fajr,dhuhr,asr,maghrib,isha\nsoon,5:41,12:05,2:45,5:10,6:30\n");
        assertTrue(badDate.errors().get(0).startsWith("Line 2"));
        SalahCsv.Result outOfOrder = SalahCsv.parse("date,fajr,dhuhr,asr,maghrib,isha\n2026-11-01,5:41,12:05,2:45,5:10,4:30\n");
        assertTrue(outOfOrder.errors().get(0).contains("not in order"));
        SalahCsv.Result overlap = SalahCsv.parse("start date,end date,fajr,dhuhr,asr,maghrib,isha\n"
                + "2026-11-01,2026-11-10,5:41,12:05,2:45,5:10,6:30\n2026-11-10,2026-11-20,5:48,12:07,2:42,5:05,6:26\n");
        assertTrue(overlap.errors().stream().anyMatch(e -> e.contains("same day")));
        assertFalse(SalahCsv.parse("").errors().isEmpty());
    }

    @Test
    void quotedCellsAndBomAreHandled() {
        String csv = "﻿date,fajr,dhuhr,asr,maghrib,isha,notes\n2026-11-01,5:41,12:05,2:45,5:10,6:30,\"DST ends, clocks back\"\n";
        SalahCsv.Result r = SalahCsv.parse(csv);
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertEquals("DST ends, clocks back", r.rows().get(0).notes());
    }

    private static void assertNear(LocalTime expected, LocalTime actual, int minutes) {
        long diff = Math.abs(java.time.Duration.between(expected, actual).toMinutes());
        assertTrue(diff <= minutes, "expected about " + expected + " but was " + actual);
    }

    @Test
    void sunTimesMatchPublishedValues() {
        var ny = java.time.ZoneId.of("America/New_York");
        // New York on the summer solstice: sunrise 5:25 AM, sunset 8:31 PM (EDT)
        var summer = SolarTimes.of(d("2026-06-21"), 40.7128, -74.0060, ny).orElseThrow();
        assertNear(LocalTime.of(5, 25), summer.sunrise(), 3);
        assertNear(LocalTime.of(20, 31), summer.sunset(), 3);
        // New York on the winter solstice: sunrise 7:17 AM, sunset 4:32 PM (EST)
        var winter = SolarTimes.of(d("2026-12-21"), 40.7128, -74.0060, ny).orElseThrow();
        assertNear(LocalTime.of(7, 17), winter.sunrise(), 3);
        assertNear(LocalTime.of(16, 32), winter.sunset(), 3);
        // Los Angeles in the middle of the year, a different zone and longitude
        var la = SolarTimes.of(d("2026-06-21"), 34.0522, -118.2437, java.time.ZoneId.of("America/Los_Angeles")).orElseThrow();
        assertNear(LocalTime.of(5, 42), la.sunrise(), 3);
        assertNear(LocalTime.of(20, 8), la.sunset(), 3);
        // No sunrise or sunset far north in midsummer
        assertTrue(SolarTimes.of(d("2026-06-21"), 80.0, 0.0, java.time.ZoneId.of("UTC")).isEmpty());
    }

    @Test
    void maghribIsSunsetPlusTheMasjidsMinutes() {
        var sun = SolarTimes.of(d("2026-06-21"), 40.7128, -74.0060, java.time.ZoneId.of("America/New_York")).orElseThrow();
        assertEquals(sun.sunset().plusMinutes(5), sun.sunset().plusMinutes(5));
        assertTrue(sun.sunset().plusMinutes(5).isAfter(sun.sunset()));
    }

    @Test
    void sunriseAndMaghribMayBeLeftOutOfTheFile() {
        String csv = "start date,end date,fajr,dhuhr,asr,isha\n2026-11-01,2026-11-10,5:41,12:05,2:45,6:30\n";
        SalahCsv.Result r = SalahCsv.parse(csv);
        assertTrue(r.errors().isEmpty(), r.errors().toString());
        assertNull(r.rows().get(0).maghrib());
        assertNull(r.rows().get(0).sunrise());
        assertTrue(r.rows().get(0).jumuah().isEmpty());
    }

    @Test
    void readsSeveralJumuahTimes() {
        // all in one cell, with a label
        SalahCsv.Result one = SalahCsv.parse("date,fajr,dhuhr,asr,isha,jumuah\n2026-11-06,5:41,12:05,2:45,6:30,\"1:15 PM (English) | 2:15 PM (Arabic)\"\n");
        assertTrue(one.errors().isEmpty(), one.errors().toString());
        assertEquals(List.of(new SalahCsv.Jumuah(LocalTime.of(13, 15), "English"), new SalahCsv.Jumuah(LocalTime.of(14, 15), "Arabic")),
                one.rows().get(0).jumuah());
        // in several columns
        SalahCsv.Result cols = SalahCsv.parse("date,fajr,dhuhr,asr,isha,jumuah 1,jumuah 2\n2026-11-06,5:41,12:05,2:45,6:30,1:15,2:15\n");
        assertTrue(cols.errors().isEmpty(), cols.errors().toString());
        assertEquals(2, cols.rows().get(0).jumuah().size());
        assertEquals(LocalTime.of(14, 15), cols.rows().get(0).jumuah().get(1).time());
        // a bad time is reported
        assertFalse(SalahCsv.parse("date,fajr,dhuhr,asr,isha,jumuah\n2026-11-06,5:41,12:05,2:45,6:30,soon\n").errors().isEmpty());
    }
}
