package org.nikahgator.lifeevent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One janazah, shahada or baby-naming record of a masjid. */
@Entity
@Table(name = "life_events")
@Getter @Setter
public class LifeEvent {
    public static final String VALID = "VALID";
    public static final String VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private LifeEventType eventType;

    @Column(name = "cert_number")
    private String certNumber;

    private String status = VALID;

    @Column(name = "person_name")
    private String personName;
    @Column(name = "alt_name")
    private String altName;
    @Column(name = "event_date")
    private LocalDate eventDate;
    @Column(name = "secondary_date")
    private LocalDate secondaryDate;
    @Column(name = "secondary_time")
    private LocalTime secondaryTime;
    private String place;
    @Column(name = "burial_date")
    private LocalDate burialDate;
    @Column(name = "burial_time")
    private LocalTime burialTime;
    @Column(name = "burial_place")
    private String burialPlace;
    private String cemetery;
    @Column(name = "contact_name")
    private String contactName;
    @Column(name = "contact_phone")
    private String contactPhone;
    private String parents;
    private String witnesses;
    @Column(name = "imam_name")
    private String imamName;
    private String notes;

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
}
