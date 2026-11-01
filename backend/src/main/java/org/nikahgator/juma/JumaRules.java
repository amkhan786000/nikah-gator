package org.nikahgator.juma;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nikahgator.exception.ApiException;
import org.nikahgator.juma.JumaDtos.Request;
import org.nikahgator.juma.JumaDtos.StudentDto;
import org.springframework.http.HttpStatus;

/** Validation and wording of Juma letters; no database or PDF here so it is easy to test. */
public final class JumaRules {
    public static final LocalTime DEFAULT_START = LocalTime.of(12, 30);
    public static final LocalTime DEFAULT_END = LocalTime.of(14, 0);

    private JumaRules() {
    }

    /** The request with text cleaned and defaults applied. Throws 400 for an invalid combination. */
    public static Request normalize(Request in, LocalDate today) {
        LocalTime start = in.windowStart() == null ? DEFAULT_START : in.windowStart();
        LocalTime end = in.windowEnd() == null ? DEFAULT_END : in.windowEnd();
        if (!start.isBefore(end)) throw bad("The Friday time window must end after it starts.");

        LocalDate endDate = in.standing() ? null : in.endDate();
        if (!in.standing()) {
            if (endDate == null) throw bad("Enter the last date, or choose a standing request.");
            if (endDate.isBefore(in.startDate())) throw bad("The last date cannot be before the first date.");
            if (in.startDate().plusYears(2).isBefore(endDate)) throw bad("A date range can cover at most two years.");
        }

        List<StudentDto> students = new ArrayList<>();
        for (StudentDto s : in.students()) {
            String name = clean(s.name());
            if (name.isEmpty()) throw bad("Every student needs a name.");
            students.add(new StudentDto(name, blankToNull(clean(s.grade()))));
        }
        if (students.isEmpty()) throw bad("Add at least one student.");

        return new Request(clean(in.schoolName()), blankToNull(clean(in.schoolAddress())), blankToNull(clean(in.schoolContact())),
                clean(in.addressee()), in.letterDate() == null ? today : in.letterDate(), in.standing(), in.startDate(), endDate,
                start, end, blankToNull(clean(in.note())), students);
    }

    /** "from September 1, 2026 until further notice" or "from ... through ...". */
    public static String period(boolean standing, LocalDate start, LocalDate end) {
        String from = date(start);
        return standing || end == null ? "beginning " + from + " and continuing until further notice"
                : "from " + from + " through " + date(end);
    }

    public static String window(LocalTime start, LocalTime end) {
        return time(start) + " and " + time(end);
    }

    public static String time(LocalTime t) {
        return t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US));
    }

    public static String date(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US));
    }

    static String clean(String s) {
        return s == null ? "" : s.replaceAll("[\\p{Cc}&&[^\\n]]", "").trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
