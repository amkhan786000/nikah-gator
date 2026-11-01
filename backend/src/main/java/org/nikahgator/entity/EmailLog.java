package org.nikahgator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_log")
@Getter @Setter
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipient;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private boolean success;

    /** SENT or FAILED (success mirrors it). */
    private String status = "SENT";

    /** CERTIFICATE, OTP, PASSWORD, REMINDER, WELCOME or OTHER. */
    private String kind = "OTHER";

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "sent_by_user_id")
    private Long sentByUserId;

    @Column(name = "sent_by_name")
    private String sentByName;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "attachment_name")
    private String attachmentName;
    private String error;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
