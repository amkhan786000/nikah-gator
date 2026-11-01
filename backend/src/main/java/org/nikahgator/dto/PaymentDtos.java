package org.nikahgator.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    /** Max single amount: $100,000. */
    public static final int MAX_CENTS = 10_000_000;

    public record MasjidSettings(Long id, String name, String addressLine, String city, String state, String zip,
                                 String phone, String email,
                                 @NotNull @Min(0) @Max(MAX_CENTS) Integer nikahFeeCents,
                                 @NotNull @Min(0) @Max(MAX_CENTS) Integer suggestedDonationCents) {
    }

    public record RecordPayment(
            @NotBlank String kind,
            @NotNull @Min(1) @Max(MAX_CENTS) Integer amountCents,
            @NotBlank String method,
            @NotNull LocalDate paidAt,
            @Size(max = 200) String payerName,
            @Size(max = 500) String note) {
    }

    public record Reason(@NotBlank @Size(max = 500) String reason) {
    }

    public record Waiver(boolean waived, @Size(max = 500) String reason) {
    }

    public record PaymentView(Long id, String kind, int amountCents, String status, String method, LocalDate paidAt,
                              String payerName, String note, String receiptNumber, String statusReason,
                              LocalDateTime statusChangedAt, String receivedByName, LocalDateTime createdAt) {
    }

    public record Summary(int feeCents, boolean feeWaived, String waivedReason, int paidCents, int balanceCents,
                          int donationCents, String status, int suggestedDonationCents, List<PaymentView> payments) {
    }

    /** One row for the nikah list badge. */
    public record StatusRow(Long nikahId, String status, int balanceCents) {
    }

    public record Receipt(String receiptNumber, String masjidName, String masjidAddress, String masjidPhone,
                          String payerName, String groomName, String brideName, String kind, int amountCents,
                          String currency, String method, LocalDate paidAt, String note, String receivedByName) {
    }
}
