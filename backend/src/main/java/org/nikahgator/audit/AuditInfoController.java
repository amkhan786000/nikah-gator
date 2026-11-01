package org.nikahgator.audit;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.nikahgator.entity.AppUser;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

/** "Created by X on ..., last changed by Y on ..." for one record, shown on its detail page. */
@RestController
@RequestMapping("/api/imam/audit-info")
public class AuditInfoController {

    public record AuditInfo(String createdBy, LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt) {
    }

    private final EntityManager em;
    private final AppUserRepository users;

    public AuditInfoController(EntityManager em, AppUserRepository users) {
        this.em = em;
        this.users = users;
    }

    @GetMapping("/{entityType}/{id}")
    public AuditInfo get(@AuthenticationPrincipal AppUser me, @PathVariable String entityType, @PathVariable Long id) {
        Class<?> type = em.getMetamodel().getEntities().stream().map(EntityType::getJavaType)
                .filter(c -> Auditable.class.isAssignableFrom(c) && c.getSimpleName().equals(entityType)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        Object e = em.find(type, id);
        if (!(e instanceof Auditable a) || !sameMasjid(e, me)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Not found"); // another masjid's record looks like no record
        }
        return new AuditInfo(name(a.getCreatedBy()), a.getCreatedAt(), name(a.getUpdatedBy()), a.getUpdatedAt());
    }

    private String name(Long userId) {
        return userId == null ? null : users.findById(userId).map(AppUser::getFullName).orElse("Former user");
    }

    /** Records that carry a masjid must be this user's; the few that do not (child rows) are not offered. */
    private static boolean sameMasjid(Object entity, AppUser me) {
        try {
            Method m = entity.getClass().getMethod("getMasjidId");
            return m.invoke(entity) instanceof Long id && id.equals(me.getMasjidId());
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
