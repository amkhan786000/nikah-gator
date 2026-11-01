package org.nikahgator.security;

import java.io.IOException;
import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository users;
    private final MasjidRepository masjids;

    public JwtAuthFilter(JwtService jwtService, AppUserRepository users, MasjidRepository masjids) {
        this.jwtService = jwtService;
        this.users = users;
        this.masjids = masjids;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AppUser user = users.findById(jwtService.userIdFrom(header.substring(7))).orElse(null);
                if (user != null && user.isEnabled() && masjidIsActive(user)) {
                    var auth = new UsernamePasswordAuthenticationToken(user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    // A temporary password may only be changed (or the user signed out) until it is replaced.
                    if (user.isMustChangePassword() && !allowedBeforePasswordChange(request.getRequestURI())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Please change your password before continuing.\","
                                + "\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
                        return;
                    }
                }
            } catch (Exception ignored) {
                // invalid or expired token: the request continues unauthenticated
            }
        }
        chain.doFilter(request, response);
    }

    /** A deactivated masjid locks out everyone who belongs to it, including staff the imam added. */
    private boolean masjidIsActive(AppUser user) {
        return user.getMasjidId() == null || masjids.findById(user.getMasjidId()).map(m -> m.isActive()).orElse(false);
    }

    private static boolean allowedBeforePasswordChange(String path) {
        return path == null || !path.startsWith("/api/") || path.startsWith("/api/auth/") || path.startsWith("/api/public/");
    }
}
