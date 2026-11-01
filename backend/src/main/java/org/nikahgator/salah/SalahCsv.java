package org.nikahgator.salah;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a timetable CSV. One row is either one day (a "date" column) or a range of days ("start date" and
 * "end date" columns), so the same file works whether a masjid's times change daily, every 10 days, every 2 weeks
 * or monthly. Headers are matched loosely (Zuhr/Dhuhr, Magrib/Maghrib, ...), the delimiter may be a comma,
 * semicolon or tab, and times may be 24-hour or 12-hour (with or without AM/PM).
 */
public final class SalahCsv {
    private SalahCsv() {
    }

    public static final int MAX_ROWS = 800;

    /** One Jumu'ah in the file (a masjid may have several). */
    public record Jumuah(LocalTime time, String label) {
    }

    /** Sunrise and Maghrib are null when the file does not give them: they are then calculated from the masjid's location. */
    public record Row(int line, LocalDate start, LocalDate end, LocalTime fajr, LocalTime sunrise, LocalTime dhuhr,
                      LocalTime asr, LocalTime maghrib, LocalTime isha, List<Jumuah> jumuah, String notes) {
    }

    public record Result(List<Row> rows, List<String> errors) {
    }

    /** The column order assumed when the file has no header row. */
    private static final List<String> DEFAULT_ORDER = List.of("date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha", "jumuah");

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        for (String s : new String[] { "date", "day" }) ALIASES.put(s, "date");
        for (String s : new String[] { "startdate", "start", "from", "begin", "begins" }) ALIASES.put(s, "start");
        for (String s : new String[] { "enddate", "end", "to", "until", "through" }) ALIASES.put(s, "end");
        for (String s : new String[] { "fajr", "fajar", "fajer", "subh", "sobh" }) ALIASES.put(s, "fajr");
        for (String s : new String[] { "sunrise", "shuruq", "shurooq", "shuruk", "ishraq" }) ALIASES.put(s, "sunrise");
        for (String s : new String[] { "dhuhr", "zuhr", "dhur", "duhr", "zuhar", "dhuhar", "zohr" }) ALIASES.put(s, "dhuhr");
        for (String s : new String[] { "asr", "asar", "asir" }) ALIASES.put(s, "asr");
        for (String s : new String[] { "maghrib", "magrib", "maghreb", "magreb", "maghib" }) ALIASES.put(s, "maghrib");
        for (String s : new String[] { "isha", "ishaa", "esha", "isa", "eisha" }) ALIASES.put(s, "isha");
        for (String s : new String[] { "jumuah", "jumah", "jummah", "jumua", "jumma", "friday", "jumuA", "juma", "khutbah" }) ALIASES.put(s.toLowerCase(Locale.ROOT), "jumuah");
        for (String s : new String[] { "notes", "note", "comment", "comments" }) ALIASES.put(s, "notes");
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("uuuu-M-d"), DateTimeFormatter.ofPattern("M/d/uuuu"), DateTimeFormatter.ofPattern("M/d/uu"),
            DateTimeFormatter.ofPattern("M-d-uuuu"), DateTimeFormatter.ofPattern("uuuu/M/d"), DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH), DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH));

    private static final Pattern TIME = Pattern.compile("^(\\d{1,2})[:.](\\d{2})(?::\\d{2})?\\s*([ap])?\\.?m?\\.?$", Pattern.CASE_INSENSITIVE);

    public static Result parse(String text) {
        List<String> errors = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        if (text == null) {
            return new Result(rows, List.of("The file is empty."));
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        List<String> lines = new ArrayList<>(List.of(text.split("\\r?\\n")));
        lines.removeIf(l -> l.isBlank());
        if (lines.isEmpty()) {
            return new Result(rows, List.of("The file is empty."));
        }
        char delimiter = delimiter(lines.get(0));
        List<String> first = split(lines.get(0), delimiter);

        List<String> columns = new ArrayList<>();
        boolean hasHeader = parseDate(first.get(0)) == null;
        int firstData = 0;
        if (hasHeader) {
            for (String h : first) {
                String key = ALIASES.get(h.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""));
                columns.add(key == null ? "" : key);
            }
            firstData = 1;
        } else {
            columns.addAll(DEFAULT_ORDER);
        }
        boolean ranged = columns.contains("start");
        if (!(columns.contains("date") || ranged)) {
            errors.add("The first row must name the columns, with a \"date\" column (one day per row) or \"start date\" and \"end date\" columns (a range per row).");
            return new Result(rows, errors);
        }
        for (String need : new String[] { "fajr", "dhuhr", "asr", "isha" }) {
            if (!columns.contains(need)) {
                errors.add("The file has no \"" + need + "\" column.");
            }
        }
        if (ranged && !columns.contains("end")) {
            errors.add("A file with a \"start date\" column also needs an \"end date\" column.");
        }
        if (!errors.isEmpty()) {
            return new Result(rows, errors);
        }
        if (lines.size() - firstData > MAX_ROWS) {
            return new Result(rows, List.of("The file has more than " + MAX_ROWS + " rows. Split it into smaller files."));
        }

        for (int i = firstData; i < lines.size(); i++) {
            int lineNo = i + 1;
            List<String> cells = split(lines.get(i), delimiter);
            Map<String, String> v = new HashMap<>();
            for (int c = 0; c < columns.size() && c < cells.size(); c++) {
                if (!columns.get(c).isEmpty() && !cells.get(c).isBlank()) {
                    String key = columns.get(c);
                    for (int n = 2; v.containsKey(key) && key.startsWith("jumuah"); n++) {
                        key = "jumuah#" + n; // jumuah 1, jumuah 2 ... columns
                    }
                    v.putIfAbsent(key, cells.get(c).trim());
                }
            }
            int before = errors.size();
            LocalDate start = date(v.get(ranged ? "start" : "date"), ranged ? "start date" : "date", lineNo, errors);
            LocalDate end = ranged ? date(v.get("end"), "end date", lineNo, errors) : start;
            LocalTime fajr = time("fajr", v.get("fajr"), true, lineNo, errors);
            LocalTime sunrise = time("sunrise", v.get("sunrise"), false, lineNo, errors);
            LocalTime dhuhr = time("dhuhr", v.get("dhuhr"), true, lineNo, errors);
            LocalTime asr = time("asr", v.get("asr"), true, lineNo, errors);
            LocalTime maghrib = time("maghrib", v.get("maghrib"), false, lineNo, errors);
            LocalTime isha = time("isha", v.get("isha"), true, lineNo, errors);
            List<Jumuah> jumuah = jumuah(v, lineNo, errors);
            if (errors.size() > before) {
                continue;
            }
            if (end.isBefore(start)) {
                errors.add("Line " + lineNo + ": the end date is before the start date.");
                continue;
            }
            String order = orderProblem(fajr, sunrise, dhuhr, asr, maghrib, isha);
            if (order != null) {
                errors.add("Line " + lineNo + ": " + order);
                continue;
            }
            String notes = v.get("notes");
            rows.add(new Row(lineNo, start, end, fajr, sunrise, dhuhr, asr, maghrib, isha, jumuah,
                    notes == null ? null : notes.substring(0, Math.min(200, notes.length()))));
        }
        // rows of one file must not overlap each other
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> a.start().compareTo(b.start()));
        for (int i = 1; i < sorted.size(); i++) {
            Row a = sorted.get(i - 1), b = sorted.get(i);
            if (!b.start().isAfter(a.end())) {
                errors.add("Lines " + a.line() + " and " + b.line() + " cover the same day (" + b.start() + ").");
            }
        }
        return new Result(rows, errors);
    }

    /** null when Fajr, Sunrise, Dhuhr, Asr, Maghrib and Isha are in the usual order. */
    public static String orderProblem(LocalTime fajr, LocalTime sunrise, LocalTime dhuhr, LocalTime asr, LocalTime maghrib, LocalTime isha) {
        List<LocalTime> t = new ArrayList<>(List.of(fajr));
        if (sunrise != null) t.add(sunrise);
        t.addAll(List.of(dhuhr, asr));
        if (maghrib != null) t.add(maghrib);
        t.add(isha);
        for (int i = 1; i < t.size(); i++) {
            if (!t.get(i).isAfter(t.get(i - 1))) {
                return "the times are not in order (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha). Check AM/PM.";
            }
        }
        return null;
    }

    private static final Pattern SPLIT = Pattern.compile("\\s*(?:\\||&|\\+|/|\\band\\b)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABELLED = Pattern.compile("^(.*?)\\s*\\((.*)\\)\\s*$");

    /** Every Jumu'ah in the row: one cell may list several ("1:15 PM | 2:15 PM (English)"), or there are jumuah 1, jumuah 2 columns. */
    private static List<Jumuah> jumuah(Map<String, String> v, int line, List<String> errors) {
        List<Jumuah> out = new ArrayList<>();
        for (Map.Entry<String, String> e : new java.util.TreeMap<>(v).entrySet()) {
            if (!e.getKey().startsWith("jumuah")) {
                continue;
            }
            for (String part : SPLIT.split(e.getValue())) {
                if (part.isBlank()) {
                    continue;
                }
                Matcher m = LABELLED.matcher(part.trim());
                String when = m.matches() ? m.group(1) : part.trim();
                String label = m.matches() ? m.group(2).trim() : "";
                LocalTime t = parseTime("jumuah", when);
                if (t == null) {
                    errors.add("Line " + line + ": \"" + when + "\" is not a time I can read for jumuah (use 1:15 PM).");
                } else {
                    out.add(new Jumuah(t, label));
                }
            }
        }
        out.sort((a, b) -> a.time().compareTo(b.time()));
        return out;
    }

    private static LocalDate date(String s, String what, int line, List<String> errors) {
        if (s == null) {
            errors.add("Line " + line + ": the " + what + " is missing.");
            return null;
        }
        LocalDate d = parseDate(s);
        if (d == null) {
            errors.add("Line " + line + ": \"" + s + "\" is not a date I can read (use 2026-11-05 or 11/5/2026).");
        }
        return d;
    }

    static LocalDate parseDate(String s) {
        String t = s.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    private static LocalTime time(String prayer, String s, boolean required, int line, List<String> errors) {
        if (s == null) {
            if (required) {
                errors.add("Line " + line + ": the " + prayer + " time is missing.");
            }
            return null;
        }
        LocalTime t = parseTime(prayer, s);
        if (t == null) {
            errors.add("Line " + line + ": \"" + s + "\" is not a time I can read for " + prayer + " (use 5:23 or 5:23 AM).");
        }
        return t;
    }

    /**
     * Parses "5:23", "05:23", "17:45", "5:23 AM", "5:23pm". With no AM/PM the prayer decides: Fajr and sunrise are
     * mornings; Dhuhr 1 to 10 is the afternoon (11 and 12 stay); Asr, Maghrib, Isha and Jumuah 1 to 11 are afternoon/evening.
     */
    static LocalTime parseTime(String prayer, String s) {
        Matcher m = TIME.matcher(s.trim());
        if (!m.matches()) {
            return null;
        }
        int h = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        if (min > 59 || h > 23) {
            return null;
        }
        String ampm = m.group(3);
        if (ampm != null) {
            if (h < 1 || h > 12) {
                return null;
            }
            boolean pm = ampm.equalsIgnoreCase("p");
            h = h % 12 + (pm ? 12 : 0);
        } else if (h <= 12) {
            switch (prayer) {
                case "dhuhr" -> { if (h >= 1 && h <= 10) h += 12; }
                case "asr", "maghrib", "isha", "jumuah" -> { if (h >= 1 && h <= 11) h += 12; }
                default -> { if (h == 12) h = 0; }
            }
        }
        return LocalTime.of(h, min);
    }

    private static char delimiter(String headerLine) {
        int commas = count(headerLine, ','), semis = count(headerLine, ';'), tabs = count(headerLine, '\t');
        if (tabs > commas && tabs >= semis) return '\t';
        return semis > commas ? ';' : ',';
    }

    private static int count(String s, char c) {
        int n = 0;
        for (char x : s.toCharArray()) if (x == c) n++;
        return n;
    }

    /** Splits one line, honouring double quotes. */
    static List<String> split(String line, char delimiter) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == delimiter) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }
}
