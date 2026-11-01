package org.nikahgator.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.nikahgator.audit.Actor;
import org.nikahgator.audit.ActivityLogService;
import org.nikahgator.dto.AuthDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.LoginOtp;
import org.nikahgator.entity.PasswordResetCode;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.LoginOtpRepository;
import org.nikahgator.repository.PasswordResetCodeRepository;
import org.nikahgator.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Sign-in (password, then an e-mailed code), forgot/reset password and password change. */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final int MAX_ATTEMPTS = 5;
    static final int LOCK_MINUTES = 15;
    static final int OTP_MINUTES = 5;
    static final int RESET_CODE_MINUTES = 15;
    static final int RESET_CODES_PER_HOUR = 3;

    private final AppUserRepository users;
    private final LoginOtpRepository otps;
    private final PasswordResetCodeRepository resetCodes;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailService email;
    private final TrustedDeviceService devices;
    private final ActivityLogService activity;

    /** Local testing only (LOG_OTP=true): print codes in the backend log so sign-in works without SMTP. */
    @Value("${app.mail.log-otp:false}")
    private boolean logOtp;

    public AuthService(AppUserRepository users, LoginOtpRepository otps, PasswordResetCodeRepository resetCodes,
                       PasswordEncoder encoder, JwtService jwt, EmailService email, TrustedDeviceService devices,
                       ActivityLogService activity) {
        this.users = users;
        this.otps = otps;
        this.resetCodes = resetCodes;
        this.encoder = encoder;
        this.jwt = jwt;
        this.email = email;
        this.devices = devices;
        this.activity = activity;
    }

    private Optional<AppUser> find(String address) {
        return address == null || address.isBlank() ? Optional.empty() : users.findByEmailIgnoreCase(address.trim());
    }

    private static String sixDigits() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** Password check, then the emailed code, unless {@code deviceToken} is this user's remembered, unexpired browser. */
    public AuthResponse login(LoginRequest req, String ip, String deviceToken) {
        AppUser user = find(req.email()).orElseThrow(() -> {
            String tried = req.email() == null ? "" : req.email().trim();
            activity.record(null, ActivityLogService.LOGIN_FAILED, null, "AppUser", null,
                    "Failed sign-in: no account for \"" + (tried.length() > 100 ? tried.substring(0, 100) : tried) + "\"", null);
            return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        });
        requireNotLocked(user);
        if (!user.isEnabled()) {
            loginFailed(user, "Failed sign-in: the account is inactive");
            throw new ApiException(HttpStatus.FORBIDDEN, "Your account is inactive. Please contact the administrator.");
        }
        if (req.password() == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            loginFailed(user, "Failed sign-in: wrong password");
            registerFailure(user);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        clearFailures(user);

        if (devices.isTrusted(user.getId(), deviceToken, ip)) {
            return issueToken(user, "Signed in (remembered device)");
        }

        // Only the newest code works.
        List<LoginOtp> open = otps.findByUserIdAndUsedFalse(user.getId());
        open.forEach(o -> o.setUsed(true));
        otps.saveAll(open);

        String code = sixDigits();
        LoginOtp otp = new LoginOtp();
        otp.setUserId(user.getId());
        otp.setCodeHash(encoder.encode(code));
        otps.save(otp);
        email.send(EmailService.Meta.of(EmailService.OTP, user.getMasjidId()), user.getEmail(), "Your Masjidly sign-in code",
                "Assalamu alaikum " + user.getFullName() + ",\n\nYour sign-in code is " + code + ". It is valid for "
                        + OTP_MINUTES + " minutes.\n\nIf you did not try to sign in, please ignore this e-mail.", code);
        if (logOtp) {
            log.warn("LOG_OTP is on: sign-in code for {} is {}", user.getEmail(), code);
        }
        return AuthResponse.otpNeeded(user.getId(), "A code has been sent to your email. Please enter it to continue.", devices.rememberDays());
    }

    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        ApiException invalid = new ApiException(HttpStatus.BAD_REQUEST, "Invalid code. Please try again.");
        AppUser user = (req.id() == null ? Optional.<AppUser>empty() : users.findById(req.id())).orElseThrow(() -> invalid);
        requireNotLocked(user);
        LoginOtp otp = otps.findFirstByUserIdAndUsedFalseOrderByIdDesc(user.getId()).orElseThrow(() -> invalid);
        if (otp.getCreatedAt().plusMinutes(OTP_MINUTES).isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            otps.save(otp);
            throw new ApiException(HttpStatus.BAD_REQUEST, "The code has expired. Please sign in again.");
        }
        String code = req.otp() == null ? "" : req.otp().trim();
        if (!encoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            if (otp.getAttempts() >= MAX_ATTEMPTS) {
                otp.setUsed(true);
            }
            loginFailed(user, "Failed sign-in: wrong code");
            otps.save(otp);
            registerFailure(user);
            throw invalid;
        }
        otp.setUsed(true);
        otps.save(otp);
        clearFailures(user);
        return issueToken(user, "Signed in");
    }

    private void requireNotLocked(AppUser user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            loginFailed(user, "Failed sign-in: the account is locked");
            long minutes = Math.max(1, Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1);
            throw new ApiException(HttpStatus.LOCKED, "Too many failed attempts. Try again in " + minutes
                    + " minute(s), or reset your password with \"Forgot password\".");
        }
    }

    private void registerFailure(AppUser user) {
        int failures = user.getFailedAttempts() + 1;
        if (failures >= MAX_ATTEMPTS) {
            user.setFailedAttempts(0);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            loginFailed(user, "Account locked for " + LOCK_MINUTES + " minutes after " + MAX_ATTEMPTS + " failed attempts");
        } else {
            user.setFailedAttempts(failures);
        }
        users.save(user);
    }

    private void clearFailures(AppUser user) {
        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            users.save(user);
        }
    }

    private void loginFailed(AppUser user, String why) {
        activity.record(Actor.of(user), ActivityLogService.LOGIN_FAILED, user.getMasjidId(), "AppUser", user.getId(), why, null);
    }

    private AuthResponse issueToken(AppUser user, String how) {
        activity.record(Actor.of(user), ActivityLogService.LOGIN, user.getMasjidId(), "AppUser", user.getId(), how, null);
        String token = jwt.generateToken(user.getId(), Map.of("role", user.getRole(), "name", user.getFullName()));
        return new AuthResponse(false, user.getId(), null, token, user.getFullName(), user.getRole(), user.getMasjidId(),
                user.isMustChangePassword(), devices.rememberDays());
    }

    /** Emails a reset code. Says nothing about whether the address has an account. */
    public void requestPasswordReset(String address) {
        Optional<AppUser> found = find(address);
        if (found.isEmpty() || !found.get().isEnabled()) {
            return;
        }
        AppUser user = found.get();
        if (resetCodes.countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusHours(1)) >= RESET_CODES_PER_HOUR) {
            return;
        }
        List<PasswordResetCode> open = resetCodes.findByUserIdAndUsedFalse(user.getId());
        open.forEach(c -> c.setUsed(true));
        resetCodes.saveAll(open);

        String code = sixDigits();
        PasswordResetCode reset = new PasswordResetCode();
        reset.setUserId(user.getId());
        reset.setCodeHash(encoder.encode(code));
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_CODE_MINUTES));
        resetCodes.save(reset);
        email.send(EmailService.Meta.of(EmailService.PASSWORD, user.getMasjidId()), user.getEmail(), "Reset your Masjidly password",
                "Assalamu alaikum " + user.getFullName() + ",\n\nYour password reset code is " + code + ". It is valid for "
                        + RESET_CODE_MINUTES + " minutes.\n\nIf you did not ask for this, please ignore this e-mail.", code);
        if (logOtp) {
            log.warn("LOG_OTP is on: password reset code for {} is {}", user.getEmail(), code);
        }
    }

    public void resetPassword(ResetPasswordRequest req) {
        ApiException invalid = new ApiException(HttpStatus.BAD_REQUEST, "The code is wrong or has expired. Ask for a new code.");
        AppUser user = find(req.email()).orElseThrow(() -> invalid);
        PasswordResetCode reset = resetCodes.findFirstByUserIdAndUsedFalseOrderByIdDesc(user.getId())
                .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now())).orElseThrow(() -> invalid);
        String code = req.code() == null ? "" : req.code().trim();
        if (!encoder.matches(code, reset.getCodeHash())) {
            reset.setAttempts(reset.getAttempts() + 1);
            if (reset.getAttempts() >= MAX_ATTEMPTS) {
                reset.setUsed(true); // too many guesses: this code is void
            }
            resetCodes.save(reset);
            throw invalid;
        }
        PasswordPolicy.check(req.newPassword(), req.confirmPassword());
        reset.setUsed(true);
        resetCodes.save(reset);
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        user.setLockedUntil(null); // proving access to the mailbox also lifts a sign-in lock
        Actor.runAs(user, () -> users.save(user)); // nobody is signed in: the change is attributed to the account owner
        devices.forgetAll(user.getId()); // a new password: every browser must use a code again
        email.send(EmailService.Meta.of(EmailService.PASSWORD, user.getMasjidId()), user.getEmail(), "Your Masjidly password was changed",
                "Your password was reset using \"Forgot password\". If this wasn't you, contact the administrator straight away.");
    }

    public void changePassword(AppUser user, ChangePasswordRequest req) {
        if (req.currentPassword() == null || !encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The current password is wrong.");
        }
        PasswordPolicy.check(req.newPassword(), req.confirmPassword());
        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a password different from the current one.");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        users.save(user);
        devices.forgetAll(user.getId());
    }
}
