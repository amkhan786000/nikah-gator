package org.nikahgator.web;

import java.util.Map;

import org.nikahgator.audit.ActivityLogService;
import org.nikahgator.dto.AuthDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.service.TrustedDeviceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final TrustedDeviceService devices;
    private final MasjidRepository masjids;
    private final ActivityLogService activity;

    public AuthController(AuthService auth, TrustedDeviceService devices, MasjidRepository masjids,
                          ActivityLogService activity) {
        this.activity = activity;
        this.auth = auth;
        this.devices = devices;
        this.masjids = masjids;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req, HttpServletRequest request) {
        return auth.login(req, DeviceCookie.clientIp(request), DeviceCookie.read(request));
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@RequestBody OtpVerifyRequest req, HttpServletRequest request, HttpServletResponse response) {
        AuthResponse res = auth.verifyOtp(req);
        // "Remember this device": this browser skips the code next time, until the cookie expires.
        if (req.rememberDevice()) {
            devices.remember(res.id(), request.getHeader("User-Agent"), DeviceCookie.clientIp(request))
                    .ifPresent(d -> DeviceCookie.write(request, response, d.token(), d.days()));
        }
        return res;
    }

    /** The answer is the same whether or not the e-mail has an account. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        auth.requestPasswordReset(req.email());
        return Map.of("message", "If that email belongs to an account, a reset code has been sent to it. It is valid for 15 minutes.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req);
        return Map.of("message", "Your password has been changed. You can sign in now.");
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@AuthenticationPrincipal AppUser user, @RequestBody ChangePasswordRequest req) {
        auth.changePassword(user, req);
        return Map.of("message", "Your password has been changed.");
    }

    /** Tokens are stateless, so this only records the sign-out in the activity log. */
    @PostMapping("/logout")
    public Map<String, String> logout(@AuthenticationPrincipal AppUser user) {
        activity.record(ActivityLogService.LOGOUT, user.getMasjidId(), "AppUser", user.getId(), "Signed out", null);
        return Map.of("message", "Signed out.");
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AppUser user) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("id", user.getId());
        out.put("email", user.getEmail());
        out.put("name", user.getFullName());
        out.put("role", user.getRole());
        out.put("phone", user.getPhone());
        out.put("jobTitle", user.getJobTitle());
        out.put("mustChangePassword", user.isMustChangePassword());
        if (user.getMasjidId() != null) {
            masjids.findById(user.getMasjidId()).ifPresent(m -> {
                out.put("masjidName", m.getName());
                out.put("masjidAddress", java.util.stream.Stream.of(m.getAddressLine(), m.getCity(), m.getState(), m.getZip())
                        .filter(x -> x != null && !x.isBlank()).collect(java.util.stream.Collectors.joining(", ")));
            });
        }
        return out;
    }
}
