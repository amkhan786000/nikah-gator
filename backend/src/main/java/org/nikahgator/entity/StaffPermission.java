package org.nikahgator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_permissions")
@Getter @Setter
public class StaffPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "service_key")
    private String serviceKey;

    /** VIEW or MANAGE */
    private String level;
}
