package org.nikahgator.audit;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

/**
 * Extend this to make an entity accountable: it gets created_by / updated_by (filled from the signed-in user, null
 * for system jobs), read-only created_at / updated_at (the database fills them), and every create, change and
 * delete is written to the activity log. The table needs the four columns (see V19__activity_log.sql).
 */
@MappedSuperclass
@EntityListeners(AuditListener.class)
@Getter @Setter
public abstract class Auditable {

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** The field values as last loaded or saved; the activity log diffs against it. */
    @Transient
    private Map<String, Object> auditSnapshot;
}
