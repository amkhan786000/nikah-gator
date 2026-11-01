package org.nikahgator.divorce;

import java.time.LocalDate;
import java.util.Locale;

import org.nikahgator.divorce.DivorceDtos.Input;
import org.nikahgator.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Validation of divorce records. Pure functions, no database. */
public final class DivorceRules {
    private DivorceRules() {
    }

    /** About three months after the divorce (the usual iddah of three monthly cycles); only a suggestion. */
    public static LocalDate suggestedIddahEnd(LocalDate divorceDate) {
        return divorceDate.plusMonths(3);
    }

    /** The input with text cleaned (blank -> null) and the type / status upper-cased; throws 400 when invalid. */
    public static Input normalize(Input in, LocalDate today) {
        DivorceType type = parseType(in.divorceType());
        String status = in.status() == null || in.status().isBlank() ? Divorce.RECORDED : in.status().trim().toUpperCase(Locale.ROOT);
        if (!status.equals(Divorce.RECORDED) && !status.equals(Divorce.REVOKED)) throw bad("The status must be Recorded or Revoked.");

        String groom = clean(in.groomName());
        String bride = clean(in.brideName());
        if (groom == null || bride == null) throw bad("The names of the groom and the bride are required.");
        LocalDate date = in.divorceDate();
        if (date == null) throw bad("The date of the divorce is required.");
        if (date.isAfter(today)) throw bad("The date of the divorce cannot be in the future.");
        if (in.marriageDate() != null && in.marriageDate().isAfter(date)) throw bad("The marriage date cannot be after the divorce.");

        Integer talaq = in.talaqNumber();
        if (type == DivorceType.TALAQ) {
            if (talaq == null || talaq < 1 || talaq > 3) throw bad("Choose which talaq this is (1st, 2nd or 3rd).");
        } else {
            talaq = null;
        }
        if (in.iddahEndDate() != null && in.iddahEndDate().isBefore(date)) throw bad("The iddah cannot end before the divorce.");

        LocalDate revoked = in.revokedDate();
        if (status.equals(Divorce.REVOKED)) {
            if (type != DivorceType.TALAQ || talaq == 3) {
                throw bad("Only a first or second talaq can be revoked during the iddah.");
            }
            if (revoked == null) throw bad("The date of the reconciliation is required for a revoked divorce.");
            if (revoked.isBefore(date)) throw bad("The reconciliation cannot be before the divorce.");
            if (revoked.isAfter(today)) throw bad("The reconciliation date cannot be in the future.");
            if (in.iddahEndDate() != null && revoked.isAfter(in.iddahEndDate())) {
                throw bad("A divorce can only be revoked within the iddah.");
            }
        } else {
            revoked = null;
        }

        String w1 = clean(in.witness1Name());
        String w2 = clean(in.witness2Name());
        if (w1 == null && w2 != null) {
            w1 = w2;
            w2 = null;
        }
        return new Input(in.nikahId(), type.name(), status, groom, bride, clean(in.groomFatherName()), clean(in.brideFatherName()),
                in.marriageDate(), clean(in.marriagePlace()), date, talaq, in.iddahEndDate(), revoked,
                clean(in.settlementNotes()), clean(in.reconciliationNotes()), w1, w2, clean(in.privateNotes()));
    }

    static DivorceType parseType(String s) {
        try {
            return DivorceType.valueOf(s == null ? "" : s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw bad("Choose the type of divorce.");
        }
    }

    static String clean(String s) {
        if (s == null) return null;
        String r = s.replace("\r", "").replaceAll("[\\p{Cc}&&[^\\n]]", "").trim();
        return r.isEmpty() ? null : r;
    }

    private static ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
