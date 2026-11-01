package org.nikahgator.audit;

import java.util.function.Supplier;

import org.nikahgator.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Who is doing this: the signed-in user, or (for sign-in and password reset) the user it is being done for. */
public record Actor(Long userId, String name, String role, Long masjidId) {
    private static final ThreadLocal<Actor> OVERRIDE = new ThreadLocal<>();

    public static Actor of(AppUser u) {
        return new Actor(u.getId(), u.getFullName(), u.getRole(), u.getMasjidId());
    }

    /** The signed-in user, or null for a system job or an anonymous request. */
    public static Actor current() {
        Actor o = OVERRIDE.get();
        if (o != null) {
            return o;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AppUser u ? of(u) : null;
    }

    public static Long currentUserId() {
        Actor a = current();
        return a == null ? null : a.userId();
    }

    /** Runs {@code work} attributed to {@code user}, though nobody is signed in (password reset, sign-in). */
    public static <T> T runAs(AppUser user, Supplier<T> work) {
        Actor before = OVERRIDE.get();
        OVERRIDE.set(of(user));
        try {
            return work.get();
        } finally {
            if (before == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(before);
            }
        }
    }
}
