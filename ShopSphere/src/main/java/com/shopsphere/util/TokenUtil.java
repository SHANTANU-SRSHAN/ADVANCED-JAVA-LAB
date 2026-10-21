package com.shopsphere.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Helpers for the one-time tokens used by "remember me", password reset
 * and e-mail verification.
 *
 * <p>The raw token is only ever shown to the user (in a cookie or a link).
 * The database stores its SHA-256 hash, so a leaked database cannot be
 * used to log in as somebody.
 */
public final class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder()
            .withoutPadding();

    private TokenUtil() {
    }

    /**
     * Create a new random token.
     *
     * @param bytes how many random bytes to draw (32 = 256 bits)
     * @return a URL-safe Base64 string
     */
    public static String newToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return ENCODER.encodeToString(buffer);
    }

    /** A 256-bit token — the default for every token in the app. */
    public static String newToken() {
        return newToken(32);
    }

    /** Human-friendly order number, e.g. {@code SS-20261021-4821}. */
    public static String newOrderNumber() {
        return "SS-" + java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", RANDOM.nextInt(10000));
    }

    /** Short transaction reference shown on the receipt. */
    public static String newReference(String prefix) {
        byte[] buffer = new byte[6];
        RANDOM.nextBytes(buffer);
        return prefix + "-" + ENCODER.encodeToString(buffer)
                .substring(0, 8).toUpperCase();
    }

    /**
     * Hash a token for storage.
     *
     * @return the lower-case hex SHA-256 digest
     */
    public static String hash(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is not available on this JVM.", e);
        }
    }

    /**
     * Compare two strings without leaking timing information.
     */
    public static boolean equalsConstantTime(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
