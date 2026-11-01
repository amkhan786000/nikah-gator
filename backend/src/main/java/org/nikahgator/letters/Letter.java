package org.nikahgator.letters;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "letters")
@Getter @Setter
public class Letter {
    public static final String ISSUED = "ISSUED";
    public static final String VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "letter_number")
    private String letterNumber;

    @Column(name = "letter_type")
    private String letterType;

    @Column(name = "recipient_name")
    private String recipientName;

    private String addressee;
    private String purpose;
    private String subject;
    private String body;

    @Column(name = "letter_date")
    private LocalDate letterDate;

    @Column(name = "signer_name")
    private String signerName = "";

    private String status = ISSUED;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
