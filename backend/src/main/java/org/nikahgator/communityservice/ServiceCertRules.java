package org.nikahgator.communityservice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

import org.nikahgator.communityservice.ServiceCertDtos.Request;
import org.nikahgator.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Validation and wording of community service certificates; no database or PDF so it is easy to test. */
public final class ServiceCertRules {
    public static final BigDecimal MIN_HOURS = new BigDecimal("0.25");
    public static final BigDecimal MAX_HOURS = new BigDecimal("2000");

    private ServiceCertRules() {
    }

    /** The request with text cleaned, hours rounded to 2 places. Throws 400 for an invalid combination. */
    public static Request normalize(Request in, LocalDate today) {
        String type = in.personType() == null ? "" : in.personType().trim().toUpperCase(Locale.ROOT);
        if (!type.equals(ServiceCert.STUDENT) && !type.equals(ServiceCert.VOLUNTEER)) {
            throw bad("Choose whether this is a student or a volunteer.");
        }
        if (in.endDate().isBefore(in.startDate())) throw bad("The last date cannot be before the first date.");
        if (in.endDate().isAfter(today)) throw bad("The service period cannot end in the future.");
        if (in.startDate().plusYears(5).isBefore(in.endDate())) throw bad("The service period can cover at most five years.");
        BigDecimal hours = in.hours().setScale(2, RoundingMode.HALF_UP);
        if (hours.compareTo(MIN_HOURS) < 0 || hours.compareTo(MAX_HOURS) > 0) {
            throw bad("Hours must be between 0.25 and 2000.");
        }
        String name = clean(in.personName());
        String activities = clean(in.activities());
        String supervisor = clean(in.supervisor());
        if (name.isEmpty() || activities.isEmpty() || supervisor.isEmpty()) throw bad("Name, activities and supervisor are required.");
        return new Request(name, type, blankToNull(clean(in.organization())), activities, in.startDate(), in.endDate(), hours,
                supervisor, blankToNull(clean(in.notes())));
    }

    /** "12.5 hours", "1 hour", "8 hours". */
    public static String hoursText(BigDecimal hours) {
        BigDecimal h = hours.stripTrailingZeros();
        String s = h.scale() < 0 ? h.setScale(0).toPlainString() : h.toPlainString();
        return s + (h.compareTo(BigDecimal.ONE) == 0 ? " hour" : " hours");
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
