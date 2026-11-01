package org.nikahgator.audit;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import org.nikahgator.entity.Masjid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA lifecycle callbacks for {@link Auditable} entities: stamps created_by / updated_by with the signed-in user and
 * writes the activity log. Nothing here may throw: a problem with the log must never break the real change.
 */
@Component
public class AuditListener {
    private static final Logger log = LoggerFactory.getLogger(AuditListener.class);

    private final ActivityLogService activity;

    public AuditListener(@Lazy ActivityLogService activity) {
        this.activity = activity;
    }

    @PostLoad
    public void loaded(Object entity) {
        if (entity instanceof Auditable a) {
            try {
                a.setAuditSnapshot(ChangeDiff.snapshot(entity));
            } catch (RuntimeException e) {
                log.warn("Could not snapshot {}", entity.getClass().getSimpleName(), e);
            }
        }
    }

    @PrePersist
    public void creating(Object entity) {
        if (entity instanceof Auditable a) {
            Long uid = Actor.currentUserId();
            if (a.getCreatedBy() == null) {
                a.setCreatedBy(uid);
            }
            a.setUpdatedBy(uid);
        }
    }

    @PreUpdate
    public void updating(Object entity) {
        if (entity instanceof Auditable a) {
            a.setUpdatedBy(Actor.currentUserId());
        }
    }

    @PostPersist
    public void created(Object entity) {
        if (!(entity instanceof Auditable a)) {
            return;
        }
        try {
            a.setAuditSnapshot(ChangeDiff.snapshot(entity));
            if (logged(entity)) {
                String type = entity.getClass().getSimpleName();
                activity.record(ActivityLogService.CREATE, masjidOf(entity), type, idOf(entity),
                        "Created " + ActivityLogService.label(type).toLowerCase() + " #" + idOf(entity), null);
            }
        } catch (RuntimeException e) {
            log.warn("Could not log creation of {}", entity.getClass().getSimpleName(), e);
        }
    }

    @PostUpdate
    public void updated(Object entity) {
        if (!(entity instanceof Auditable a)) {
            return;
        }
        try {
            Map<String, Object> now = ChangeDiff.snapshot(entity);
            Map<String, String[]> changes = ChangeDiff.diff(a.getAuditSnapshot(), now);
            a.setAuditSnapshot(now);
            if (changes.isEmpty() || !logged(entity)) {
                return;
            }
            String type = entity.getClass().getSimpleName();
            String what = ActivityLogService.label(type).toLowerCase() + " #" + idOf(entity);
            if (isVoid(changes)) {
                activity.record(ActivityLogService.VOID, masjidOf(entity), type, idOf(entity), "Voided " + what,
                        ChangeDiff.toJson(changes));
                return;
            }
            String fields = changes.keySet().stream().limit(6).map(ActivityLogService::humanField)
                    .collect(Collectors.joining(", "));
            if (changes.size() > 6) {
                fields += " and " + (changes.size() - 6) + " more";
            }
            activity.record(ActivityLogService.UPDATE, masjidOf(entity), type, idOf(entity),
                    "Updated " + what + " (" + fields + ")", ChangeDiff.toJson(changes));
        } catch (RuntimeException e) {
            log.warn("Could not log change of {}", entity.getClass().getSimpleName(), e);
        }
    }

    @PostRemove
    public void removed(Object entity) {
        if (!(entity instanceof Auditable a) || !logged(entity)) {
            return;
        }
        try {
            String type = entity.getClass().getSimpleName();
            Map<String, Object> last = a.getAuditSnapshot() != null ? a.getAuditSnapshot() : ChangeDiff.snapshot(entity);
            // what was deleted stays readable here, after the record itself is gone
            activity.record(ActivityLogService.DELETE, masjidOf(entity), type, idOf(entity),
                    "Deleted " + ActivityLogService.label(type).toLowerCase() + " #" + idOf(entity),
                    ChangeDiff.toJson(ChangeDiff.deletedValues(last)));
        } catch (RuntimeException e) {
            log.warn("Could not log deletion of {}", entity.getClass().getSimpleName(), e);
        }
    }

    private static boolean logged(Object entity) {
        return !entity.getClass().isAnnotationPresent(NoActivityLog.class);
    }

    /** Voiding is an update that sets the voided flag or the VOID status. */
    static boolean isVoid(Map<String, String[]> changes) {
        String[] voided = changes.get("voided");
        String[] status = changes.get("status");
        return (voided != null && "true".equals(voided[1])) || (status != null && "VOID".equals(status[1]));
    }

    static Long idOf(Object entity) {
        return longProperty(entity, "getId");
    }

    private static Long masjidOf(Object entity) {
        if (entity instanceof Masjid m) {
            return m.getId();
        }
        return longProperty(entity, "getMasjidId"); // null falls back to the signed-in user's masjid
    }

    private static Long longProperty(Object entity, String getter) {
        try {
            Method m = entity.getClass().getMethod(getter);
            return m.invoke(entity) instanceof Long l ? l : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
