package org.nikahgator.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter @Setter
public class Payment {
    public static final String FEE = "FEE";
    public static final String DONATION = "DONATION";
    public static final String PAID = "PAID";
    public static final String VOID = "VOID";
    public static final String REFUNDED = "REFUNDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "amount_cents")
    private int amountCents;

    private String currency = "USD";
    private String kind = FEE;
    private String status = PAID;
    private String method;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(name = "payer_name")
    private String payerName;

    private String note;

    @Column(name = "receipt_number")
    private String receiptNumber;

    /** Reserved for a card processor (e.g. "STRIPE" plus its reference); "MANUAL" for hand-recorded payments. */
    private String provider = "MANUAL";

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(name = "status_changed_by")
    private Long statusChangedBy;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
