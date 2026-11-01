package org.nikahgator.security;

import java.io.IOException;
import java.util.List;

import org.nikahgator.entity.AppUser;
import org.nikahgator.repository.AppUserRepository;
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

    public JwtAuthFilter(JwtService jwtService, AppUserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AppUser user = users.findById(jwtService.userIdFrom(header.substring(7))).orElse(null);
                if (user != null && user.isEnabled()) {
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

    private static boolean allowedBeforePasswordChange(String path) {
        return path == null || !path.startsWith("/api/") || path.startsWith("/api/auth/") || path.startsWith("/api/public/");
    }
}
