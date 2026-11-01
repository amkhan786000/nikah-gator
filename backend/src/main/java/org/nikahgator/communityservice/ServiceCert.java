package org.nikahgator.communityservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A certificate of community service hours for a student or volunteer. */
@Entity
@Table(name = "community_service_certs")
@Getter @Setter
public class ServiceCert {
    public static final String VALID = "VALID";
    public static final String VOID = "VOID";
    public static final String STUDENT = "STUDENT";
    public static final String VOLUNTEER = "VOLUNTEER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "cert_number")
    private String certNumber;

    @Column(name = "person_name")
    private String personName;

    @Column(name = "person_type")
    private String personType;

    private String organization;

    private String activities;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private BigDecimal hours;

    private String supervisor;

    private String notes;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    private String status = VALID;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "created_by")
    private Long createdBy;
}
