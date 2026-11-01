package org.nikahgator.divorce;

import org.nikahgator.audit.Auditable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One divorce record of a masjid. A religious record only; sensitive, never exposed publicly. */
@Entity
@Table(name = "divorce_records")
@Getter @Setter
public class Divorce extends Auditable {
    public static final String RECORDED = "RECORDED";
    public static final String REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;
    @Column(name = "nikah_id")
    private Long nikahId;
    @Column(name = "record_number")
    private String recordNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "divorce_type")
    private DivorceType divorceType;
    private String status = RECORDED;

    @Column(name = "groom_name")
    private String groomName;
    @Column(name = "bride_name")
    private String brideName;
    @Column(name = "groom_father_name")
    private String groomFatherName;
    @Column(name = "bride_father_name")
    private String brideFatherName;
    @Column(name = "marriage_date")
    private LocalDate marriageDate;
    @Column(name = "marriage_place")
    private String marriagePlace;
    @Column(name = "divorce_date")
    private LocalDate divorceDate;
    @Column(name = "talaq_number")
    private Integer talaqNumber;
    @Column(name = "iddah_end_date")
    private LocalDate iddahEndDate;
    @Column(name = "revoked_date")
    private LocalDate revokedDate;
    @Column(name = "settlement_notes")
    private String settlementNotes;
    @Column(name = "reconciliation_notes")
    private String reconciliationNotes;
    @Column(name = "witness1_name")
    private String witness1Name;
    @Column(name = "witness2_name")
    private String witness2Name;
    @Column(name = "private_notes")
    private String privateNotes;
    @Column(name = "imam_name")
    private String imamName;

    private boolean voided;
    @Column(name = "void_reason")
    private String voidReason;
    @Column(name = "voided_at")
    private LocalDateTime voidedAt;
    @Column(name = "voided_by")
    private Long voidedBy;
}
