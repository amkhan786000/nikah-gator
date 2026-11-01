package org.nikahgator.lifeevent;

import java.util.regex.Pattern;

/** Numbers look like NG-JZ-12-2026-00042: type code, masjid id, year, 5 digit sequence per masjid, type and year. */
public final class LifeEventNumbers {
    private static final Pattern FORMAT = Pattern.compile("^NG-(JZ|SH|BN)-\\d{1,12}-\\d{4}-\\d{5,9}$");

    private LifeEventNumbers() {
    }

    public static String format(LifeEventType type, long masjidId, int year, int seq) {
        if (masjidId < 1 || year < 2000 || year > 9999 || seq < 1) {
            throw new IllegalArgumentException("Invalid number parts");
        }
        return "NG-%s-%d-%d-%05d".formatted(type.code(), masjidId, year, seq);
    }

    public static boolean isValid(String number) {
        return number != null && number.length() <= 40 && FORMAT.matcher(number).matches();
    }
}
