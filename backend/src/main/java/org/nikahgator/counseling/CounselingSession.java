package org.nikahgator.counseling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One counseling session. Private: only the imam of the masjid may read it, and the notes only on the detail view. */
@Entity
@Table(name = "counseling_sessions")
@Getter @Setter
public class CounselingSession {
    public static final Set<String> TYPES = Set.of("MARRIAGE", "FAMILY", "YOUTH", "PERSONAL", "OTHER");
    public static final Set<String> STATUSES = Set.of("SCHEDULED", "COMPLETED", "CANCELLED");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "imam_id")
    private Long imamId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "session_type")
    private String sessionType;

    @Column(name = "session_at")
    private LocalDateTime sessionAt;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "follow_up_on")
    private LocalDate followUpOn;

    private String status = "SCHEDULED";
}
