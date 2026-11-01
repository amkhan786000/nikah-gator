package org.nikahgator.web;

import java.util.Map;

import org.nikahgator.dto.AuthDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@RequestBody OtpVerifyRequest req) {
        return auth.verifyOtp(req);
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

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AppUser user) {
        return Map.of("id", user.getId(), "email", user.getEmail(), "name", user.getFullName(), "role", user.getRole(),
                "mustChangePassword", user.isMustChangePassword());
    }
}
