package org.nikahgator.catalog;

import org.nikahgator.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One row of masjid_services: the masjid-level switch for a service. */
@Entity
@Table(name = "masjid_services")
@Getter @Setter
public class MasjidService extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    @Column(name = "service_key")
    private String serviceKey;

    private boolean enabled;
}
