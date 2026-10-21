package com.shopsphere.dao;

import com.shopsphere.config.AppConfig;
import com.shopsphere.util.TokenUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * One-time tokens and "remember me" logins.
 *
 * <p>Only the SHA-256 hash of a token is stored, so the database never
 * contains a value that could be replayed as a login.
 */
public class AccountDAO {

    // ------------------------------------------------------------
    // Remember me
    // ------------------------------------------------------------

    /**
     * Create a long-lived login token and return the raw value.
     *
     * <p>The raw value goes into the cookie; only its hash is stored.
     */
    public String createRememberToken(int userId, String userAgent)
            throws SQLException {

        String rawToken = TokenUtil.newToken();
        String hash = TokenUtil.hash(rawToken);

        LocalDateTime expires = LocalDateTime.now()
                .plusDays(AppConfig.rememberMeDays());

        String sql = """
                INSERT INTO remember_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, hash);
            statement.setTimestamp(3, Timestamp.valueOf(expires));

            statement.executeUpdate();
        }

        return rawToken;
    }

    /**
     * Find the account behind a remember-me cookie.
     *
     * @return the user id, or -1 when the token is unknown or expired
     */
    public int findUserByRememberToken(String rawToken)
            throws SQLException {

        if (rawToken == null || rawToken.isBlank()) {
            return -1;
        }

        String sql = """
                SELECT user_id
                FROM remember_tokens
                WHERE token_hash = ? AND expires_at > ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, TokenUtil.hash(rawToken));
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /** Delete one remember-me token (used when signing out). */
    public void deleteRememberToken(String rawToken) throws SQLException {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String sql = "DELETE FROM remember_tokens WHERE token_hash = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, TokenUtil.hash(rawToken));
            statement.executeUpdate();
        }
    }

    /** Delete every remember-me token for a user. */
    public void deleteAllRememberTokens(int userId) throws SQLException {

        String sql = "DELETE FROM remember_tokens WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    /** Remove tokens that have expired. */
    public int purgeExpiredTokens() throws SQLException {

        String sql = "DELETE FROM remember_tokens WHERE expires_at < ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            return statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // Password reset
    // ------------------------------------------------------------

    /**
     * Create a password-reset token and return the raw value.
     */
    public String createPasswordResetToken(int userId)
            throws SQLException {

        // Only one live reset token per account.
        invalidatePasswordResetTokens(userId);

        String rawToken = TokenUtil.newToken();

        LocalDateTime expires = LocalDateTime.now()
                .plusHours(AppConfig.tokenExpiryHours());

        String sql = """
                INSERT INTO password_reset_tokens
                    (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, TokenUtil.hash(rawToken));
            statement.setTimestamp(3, Timestamp.valueOf(expires));

            statement.executeUpdate();
        }

        return rawToken;
    }

    /** The user id behind a reset token, or -1 when it is not usable. */
    public int findUserByResetToken(String rawToken) throws SQLException {

        if (rawToken == null || rawToken.isBlank()) {
            return -1;
        }

        String sql = """
                SELECT user_id
                FROM password_reset_tokens
                WHERE token_hash = ? AND used = 0 AND expires_at > ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, TokenUtil.hash(rawToken));
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /** Mark every reset token of a user as used. */
    public void invalidatePasswordResetTokens(int userId)
            throws SQLException {

        String sql = """
                UPDATE password_reset_tokens SET used = 1 WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // E-mail verification
    // ------------------------------------------------------------

    /**
     * Create an e-mail verification token and return the raw value.
     */
    public String createEmailVerificationToken(int userId)
            throws SQLException {

        String sql = """
                UPDATE email_verification_tokens SET used = 1
                WHERE user_id = ? AND used = 0
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();
        }

        String rawToken = TokenUtil.newToken();

        LocalDateTime expires = LocalDateTime.now()
                .plusHours(AppConfig.tokenExpiryHours());

        String insert = """
                INSERT INTO email_verification_tokens
                    (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(insert)) {

            statement.setInt(1, userId);
            statement.setString(2, TokenUtil.hash(rawToken));
            statement.setTimestamp(3, Timestamp.valueOf(expires));

            statement.executeUpdate();
        }

        return rawToken;
    }

    /** The user id behind a verification token, or -1. */
    public int findUserByVerificationToken(String rawToken)
            throws SQLException {

        if (rawToken == null || rawToken.isBlank()) {
            return -1;
        }

        String sql = """
                SELECT user_id
                FROM email_verification_tokens
                WHERE token_hash = ? AND used = 0 AND expires_at > ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, TokenUtil.hash(rawToken));
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : -1;
            }
        }
    }

    /** Mark a verification token as used. */
    public void useVerificationToken(String rawToken) throws SQLException {

        String sql = """
                UPDATE email_verification_tokens SET used = 1
                WHERE token_hash = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, TokenUtil.hash(rawToken));
            statement.executeUpdate();
        }
    }
}
