package org.nikahgator.entity;

import org.nikahgator.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** The fee agreed for one nikah (snapshot of the masjid default) and whether it was waived. */
@Entity
@Table(name = "nikah_fees")
@Getter @Setter
public class NikahFee extends Auditable {
    @Id
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "fee_cents")
    private int feeCents;

    private boolean waived;

    @Column(name = "waived_reason")
    private String waivedReason;

    /** The id Auditable reports in the activity log. */
    public Long getId() {
        return requestId;
    }
}
