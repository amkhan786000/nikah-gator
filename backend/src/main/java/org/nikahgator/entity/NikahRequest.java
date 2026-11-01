package org.nikahgator.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nikah_requests")
@Getter @Setter
public class NikahRequest {
    public static final String REGISTERED = "REGISTERED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "imam_id")
    private Long imamId;

    private String status = REGISTERED;

    @Column(name = "groom_name")
    private String groomName;
    @Column(name = "groom_email")
    private String groomEmail;
    @Column(name = "groom_phone")
    private String groomPhone;
    @Column(name = "groom_father_name")
    private String groomFatherName;

    @Column(name = "bride_name")
    private String brideName;
    @Column(name = "bride_email")
    private String brideEmail;
    @Column(name = "bride_phone")
    private String bridePhone;
    @Column(name = "bride_father_name")
    private String brideFatherName;

    @Column(name = "wali_name")
    private String waliName;
    @Column(name = "wali_relation")
    private String waliRelation;
    @Column(name = "witness1_name")
    private String witness1Name;
    @Column(name = "witness2_name")
    private String witness2Name;
    @Column(name = "mahr_description")
    private String mahrDescription;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
