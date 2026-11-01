package org.nikahgator.web;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** The "remember this device" cookie: HttpOnly, SameSite=Strict, sent only to /api, Secure when the site is on HTTPS. */
public final class DeviceCookie {
    public static final String NAME = "nikahgator_device";

    private DeviceCookie() {
    }

    public static String read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (NAME.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, String token, int days) {
        boolean https = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, token).httpOnly(true).secure(https)
                .sameSite("Strict").path("/api").maxAge(Duration.ofDays(days)).build().toString());
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
