package org.nikahgator.entity;

import org.nikahgator.audit.AuditIgnore;
import org.nikahgator.audit.Auditable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_users")
@Getter @Setter
public class AppUser extends Auditable {
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String IMAM = "IMAM";
    public static final String COUPLE = "COUPLE";
    /** Added by the imam; sees and manages only the services the imam grants. */
    public static final String STAFF = "STAFF";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "masjid_id")
    private Long masjidId;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(name = "job_title")
    private String jobTitle;
    private String role;
    private boolean enabled = true;

    @Column(name = "must_change_password")
    private boolean mustChangePassword;

    @Column(name = "failed_attempts")
    @AuditIgnore
    private int failedAttempts;

    @Column(name = "locked_until")
    @AuditIgnore
    private LocalDateTime lockedUntil;

}
