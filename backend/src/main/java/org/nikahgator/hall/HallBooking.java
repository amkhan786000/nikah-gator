package org.nikahgator.hall;

import org.nikahgator.audit.Auditable;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hall_bookings")
@Getter @Setter
public class HallBooking extends Auditable {
    public static final String REQUESTED = "REQUESTED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";
    public static final Set<String> STATUSES = Set.of(REQUESTED, CONFIRMED, CANCELLED);
    public static final String DEFAULT_SPACE = "Main hall";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    private String title;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    private String space = DEFAULT_SPACE;

    @Column(name = "expected_guests")
    private Integer expectedGuests;

    private String status = REQUESTED;

    @Column(name = "fee_text")
    private String feeText;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
