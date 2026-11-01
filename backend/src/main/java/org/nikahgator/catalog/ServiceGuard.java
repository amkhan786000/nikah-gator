package org.nikahgator.catalog;

import org.nikahgator.entity.AppUser;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Rejects /api/imam/** calls for a service the platform admin has switched off for this imam or masjid. */
@Component
public class ServiceGuard implements HandlerInterceptor {

    private final ServiceEntitlements entitlements;

    public ServiceGuard(ServiceEntitlements entitlements) {
        this.entitlements = entitlements;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof AppUser imam) || !AppUser.IMAM.equals(imam.getRole())) {
            return true;
        }
        var key = ServiceKey.forPath(request.getRequestURI());
        if (key.isPresent() && !entitlements.isEnabled(imam, key.get())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + key.get().label() + " is not enabled for your masjid.\","
                    + "\"code\":\"SERVICE_DISABLED\"}");
            return false;
        }
        return true;
    }
}
