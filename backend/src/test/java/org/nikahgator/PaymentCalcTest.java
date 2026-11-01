package org.nikahgator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.nikahgator.service.PaymentCalc;

class PaymentCalcTest {

    @Test
    void statusFollowsPaidAmount() {
        assertEquals("UNPAID", PaymentCalc.status(10000, false, 0));
        assertEquals("PARTIAL", PaymentCalc.status(10000, false, 2500));
        assertEquals("PAID", PaymentCalc.status(10000, false, 10000));
        assertEquals("PAID", PaymentCalc.status(10000, false, 15000));
    }

    @Test
    void waivedAndNoFee() {
        assertEquals("WAIVED", PaymentCalc.status(10000, true, 0));
        assertEquals("WAIVED", PaymentCalc.status(10000, true, 5000));
        assertEquals("NO_FEE", PaymentCalc.status(0, false, 0));
    }

    @Test
    void balanceIsNeverNegative() {
        assertEquals(7500, PaymentCalc.balance(10000, false, 2500));
        assertEquals(0, PaymentCalc.balance(10000, false, 12000));
        assertEquals(0, PaymentCalc.balance(10000, true, 0));
    }
}
