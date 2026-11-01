package org.nikahgator.divorce;

import java.util.regex.Pattern;

/** Divorce record numbers look like NG-DV-12-2026-00042: masjid id, year, 5 digit sequence per masjid and year. */
public final class DivorceNumbers {
    private static final Pattern FORMAT = Pattern.compile("^NG-DV-\\d{1,12}-\\d{4}-\\d{5,9}$");

    private DivorceNumbers() {
    }

    public static String format(long masjidId, int year, int seq) {
        if (masjidId < 1 || year < 2000 || year > 9999 || seq < 1) {
            throw new IllegalArgumentException("Invalid number parts");
        }
        return "NG-DV-%d-%d-%05d".formatted(masjidId, year, seq);
    }

    public static boolean isValid(String number) {
        return number != null && number.length() <= 40 && FORMAT.matcher(number).matches();
    }
}
