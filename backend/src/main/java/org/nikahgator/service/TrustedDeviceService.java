package org.nikahgator.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.nikahgator.entity.TrustedDevice;
import org.nikahgator.repository.TrustedDeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * "Remember this device": after the emailed code, a browser gets a random token (in an HttpOnly cookie) that lets
 * the same user skip the code there until it expires. The password is always still needed.
 */
@Service
public class TrustedDeviceService {
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Remembered(String token, int days) {
    }

    private final TrustedDeviceRepository devices;

    /** How long a remembered browser may skip the code; 0 switches the feature off. */
    @Value("${app.remember-device-days:30}")
    private int days;

    public TrustedDeviceService(TrustedDeviceRepository devices) {
        this.devices = devices;
    }

    public int rememberDays() {
        return Math.max(0, days);
    }

    public Optional<Remembered> remember(Long userId, String userAgent, String ip) {
        if (rememberDays() <= 0 || userId == null) {
            return Optional.empty();
        }
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        TrustedDevice d = new TrustedDevice();
        d.setUserId(userId);
        d.setTokenHash(hash(token));
        d.setDeviceName(deviceName(userAgent));
        d.setIpAddress(ip);
        d.setCreatedAt(LocalDateTime.now());
        d.setLastUsedAt(d.getCreatedAt());
        d.setExpiresAt(d.getCreatedAt().plusDays(rememberDays()));
        devices.save(d);
        return Optional.of(new Remembered(token, rememberDays()));
    }

    public boolean isTrusted(Long userId, String token, String ip) {
        if (token == null || token.isBlank() || userId == null || rememberDays() <= 0) {
            return false;
        }
        Optional<TrustedDevice> found = devices.findByTokenHash(hash(token))
                .filter(d -> d.getUserId().equals(userId) && d.getExpiresAt().isAfter(LocalDateTime.now()));
        found.ifPresent(d -> {
            d.setLastUsedAt(LocalDateTime.now());
            d.setIpAddress(ip);
            devices.save(d);
        });
        return found.isPresent();
    }

    /** Forgets every remembered browser of the user (password changed or reset). */
    public void forgetAll(Long userId) {
        if (userId != null) {
            devices.deleteByUserId(userId);
        }
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void removeExpired() {
        devices.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    static String deviceName(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Unknown browser";
        }
        String browser = ua.contains("Edg/") ? "Edge" : ua.contains("Firefox/") || ua.contains("FxiOS") ? "Firefox"
                : ua.contains("Chrome/") || ua.contains("CriOS") ? "Chrome" : ua.contains("Safari/") ? "Safari" : "Browser";
        String os = ua.contains("iPhone") ? "iPhone" : ua.contains("iPad") ? "iPad" : ua.contains("Android") ? "Android"
                : ua.contains("Windows") ? "Windows" : ua.contains("Mac OS X") || ua.contains("Macintosh") ? "Mac"
                : ua.contains("Linux") ? "Linux" : null;
        return os == null ? browser : browser + " on " + os;
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
