package org.nikahgator.service;

import java.security.SecureRandom;

import org.nikahgator.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class PasswordPolicy {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";

    private PasswordPolicy() {
    }

    public static void check(String password, String confirm) {
        if (password == null || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The password must be at least 8 characters with letters and numbers.");
        }
        if (!password.equals(confirm)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The passwords do not match.");
        }
    }

    /** 12 letters and digits, always containing both. */
    public static String temporaryPassword() {
        String all = LETTERS + DIGITS;
        StringBuilder sb = new StringBuilder();
        sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length()))).append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        while (sb.length() < 12) {
            sb.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        return sb.toString();
    }
}
