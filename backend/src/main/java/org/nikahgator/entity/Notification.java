package org.nikahgator.entity;

import org.nikahgator.audit.Auditable;
import org.nikahgator.audit.NoActivityLog;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A scheduled message (e-mail now, SMS later). All timestamps are UTC. */
@Entity
@NoActivityLog
@Table(name = "notifications")
@Getter @Setter
public class Notification extends Auditable {
    public static final String PENDING = "PENDING";
    public static final String SENDING = "SENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "request_id")
    private Long requestId;

    private String kind;
    private String channel = "EMAIL";
    private String recipient;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String status = PENDING;

    @Column(name = "send_at")
    private LocalDateTime sendAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    private String error;
    private int attempts;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
}
