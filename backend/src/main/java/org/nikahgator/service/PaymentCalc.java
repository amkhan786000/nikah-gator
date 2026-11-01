package org.nikahgator.service;

/** Pure fee/balance rules, kept free of persistence so they are easy to test. */
public final class PaymentCalc {
    public static final String PAID = "PAID";
    public static final String PARTIAL = "PARTIAL";
    public static final String UNPAID = "UNPAID";
    public static final String WAIVED = "WAIVED";
    public static final String NO_FEE = "NO_FEE";

    private PaymentCalc() {
    }

    /** Amount still owed; never negative. */
    public static int balance(int feeCents, boolean waived, int paidCents) {
        return waived ? 0 : Math.max(0, feeCents - paidCents);
    }

    public static String status(int feeCents, boolean waived, int paidCents) {
        if (waived) return WAIVED;
        if (feeCents <= 0) return NO_FEE;
        if (paidCents >= feeCents) return PAID;
        return paidCents > 0 ? PARTIAL : UNPAID;
    }
}
