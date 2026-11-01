package org.nikahgator.letters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Letter numbers look like NG-LT-12-2026-00042: masjid id, year, 5 digit sequence per masjid and year. */
public final class LetterNumbers {
    private static final Pattern FORMAT = Pattern.compile("^NG-LT-(\\d{1,12})-(\\d{4})-(\\d{5,9})$");

    private LetterNumbers() {
    }

    public static String format(long masjidId, int year, int seq) {
        if (masjidId < 1 || year < 2000 || year > 9999 || seq < 1) {
            throw new IllegalArgumentException("Invalid letter number parts");
        }
        return "NG-LT-%d-%d-%05d".formatted(masjidId, year, seq);
    }

    public static boolean isValid(String number) {
        return number != null && number.length() <= 40 && FORMAT.matcher(number).matches();
    }

    /** Returns {masjidId, year, seq}, or null when the text is not a letter number. */
    public static long[] parse(String number) {
        if (number == null) return null;
        Matcher m = FORMAT.matcher(number);
        if (!m.matches()) return null;
        return new long[] { Long.parseLong(m.group(1)), Long.parseLong(m.group(2)), Long.parseLong(m.group(3)) };
    }
}
