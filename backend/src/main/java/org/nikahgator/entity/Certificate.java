package org.nikahgator.entity;

import org.nikahgator.audit.Auditable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "certificates")
@Getter @Setter
public class Certificate extends Auditable {
    public static final String VALID = "VALID";
    public static final String VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "cert_number")
    private String certNumber;

    @Column(name = "nikah_date")
    private LocalDate nikahDate;

    @Column(name = "issued_by")
    private Long issuedBy;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private LocalDateTime issuedAt;

    private String status = VALID;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;
}
