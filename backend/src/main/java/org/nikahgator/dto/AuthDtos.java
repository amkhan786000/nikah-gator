package org.nikahgator.dto;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(String email, String password) {
    }

    public record OtpVerifyRequest(Long id, String otp, boolean rememberDevice) {
    }

    public record ForgotPasswordRequest(String email) {
    }

    public record ResetPasswordRequest(String email, String code, String newPassword, String confirmPassword) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
    }

    /** Either {@code otpRequired} with the user id to verify, or a signed-in {@code token}. */
    public record AuthResponse(boolean otpRequired, Long id, String message, String token, String name, String role,
                               Long masjidId, boolean mustChangePassword, int rememberDays) {
        public static AuthResponse otpNeeded(Long id, String message, int rememberDays) {
            return new AuthResponse(true, id, message, null, null, null, null, false, rememberDays);
        }
    }
}
