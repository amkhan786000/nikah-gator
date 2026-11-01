package org.nikahgator.catalog;

import java.util.Map;
import java.util.Set;

import org.nikahgator.entity.AppUser;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects /api/imam/** calls the signed-in imam or staff member may not make: a service the platform admin has
 * switched off, a service the imam did not grant to this staff member, a change with view-only access, or a
 * change to the masjid's own settings by staff.
 */
@Component
public class ServiceGuard implements HandlerInterceptor {

    /** Only the imam may change these (staff may still look). */
    private static final String[] IMAM_ONLY_WRITES = { "/api/imam/masjid", "/api/imam/certificate-template" };

    private final ServiceEntitlements entitlements;

    public ServiceGuard(ServiceEntitlements entitlements) {
        this.entitlements = entitlements;
    }

    /** The error code for a refused call, or null when the call is allowed. */
    static String refusal(String role, String method, String path, Map<ServiceKey, AccessLevel> access) {
        var key = ServiceKey.forPath(path);
        if (key.isEmpty()) {
            return null;
        }
        AccessLevel level = access.get(key.get());
        if (level == null) {
            return "SERVICE_DISABLED";
        }
        if (AppUser.STAFF.equals(role)) {
            boolean read = Set.of("GET", "HEAD", "OPTIONS").contains(method);
            if (!read && level != AccessLevel.MANAGE) {
                return "VIEW_ONLY";
            }
            if (!read) {
                for (String p : IMAM_ONLY_WRITES) {
                    if (path.equals(p) || path.startsWith(p + "/")) {
                        return "IMAM_ONLY";
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof AppUser user)
                || !(AppUser.IMAM.equals(user.getRole()) || AppUser.STAFF.equals(user.getRole()))) {
            return true;
        }
        String path = request.getRequestURI();
        String code = refusal(user.getRole(), request.getMethod(), path, entitlements.accessFor(user));
        if (code == null) {
            return true;
        }
        String label = ServiceKey.forPath(path).map(ServiceKey::label).orElse("This service");
        String message = switch (code) {
            case "VIEW_ONLY" -> "You have view-only access to " + label + ".";
            case "IMAM_ONLY" -> "Only the imam can change this.";
            default -> AppUser.STAFF.equals(user.getRole())
                    ? "You do not have access to " + label + "."
                    : label + " is not enabled for your masjid.";
        };
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\",\"code\":\"" + code + "\"}");
        return false;
    }
}
