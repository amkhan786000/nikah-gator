package org.nikahgator.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trusted_devices")
@Getter @Setter
public class TrustedDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
