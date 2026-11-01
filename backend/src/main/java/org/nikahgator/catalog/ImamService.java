package org.nikahgator.catalog;

import org.nikahgator.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** One row of imam_services: the imam-level switch for a service. */
@Entity
@Table(name = "imam_services")
@Getter @Setter
public class ImamService extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "service_key")
    private String serviceKey;

    private boolean enabled;
}
