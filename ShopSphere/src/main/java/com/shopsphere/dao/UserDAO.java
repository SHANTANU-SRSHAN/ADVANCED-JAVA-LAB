package com.shopsphere.dao;

import com.shopsphere.config.AppConfig;
import com.shopsphere.model.User;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Data access for user accounts.
 */
public class UserDAO {

    // ------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------

    /**
     * Create a customer account.
     *
     * @return true when the row was inserted
     */
    public boolean registerUser(String fullName, String email,
                                String password) throws SQLException {

        String passwordHash = hash(password);

        String sql = """
            INSERT INTO users (full_name, email, password_hash, role)
            VALUES (?, ?, ?, 'CUSTOMER')
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, passwordHash);

            return statement.executeUpdate() == 1;
        }
    }

    /** True when the e-mail is already taken. */
    public boolean emailExists(String email) throws SQLException {

        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // ------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------

    /**
     * The result of a sign-in attempt.
     *
     * @param user   the account when the sign-in succeeded, otherwise null
     * @param status OK | INVALID | LOCKED | NOT_VERIFIED
     * @param message a message suitable for showing to the customer
     */
    public record AuthResult(User user, String status, String message) {

        public boolean isSuccess() {
            return "OK".equals(status);
        }
    }

    /**
     * Verify an e-mail and password.
     *
     * <p>Failed attempts are counted; after
     * {@link AppConfig#maxFailedLogins()} the account is locked for
     * {@link AppConfig#lockoutMinutes()} minutes. A successful sign-in
     * resets the counter.
     *
     * <p>A stored password hash that is not valid BCrypt (for example a
     * row corrupted by an earlier install) is treated as a failed
     * sign-in rather than an error, so one broken row can never take the
     * whole login page down.
     */
    public AuthResult authenticate(String email, String password)
            throws SQLException {

        String sql = """
                SELECT user_id, full_name, email, password_hash, role,
                       phone, email_verified, failed_login_attempts,
                       locked_until, last_login_at
                FROM users
                WHERE email = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return new AuthResult(null, "INVALID",
                            "Invalid email or password.");
                }

                User user = mapUser(rs);
                String storedHash = rs.getString("password_hash");

                // --- account locked? ---
                LocalDateTime lockedUntil = rs.getTimestamp("locked_until") == null
                        ? null
                        : rs.getTimestamp("locked_until").toLocalDateTime();

                if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
                    return new AuthResult(null, "LOCKED",
                            "This account is temporarily locked. Please try again "
                                    + "in " + Math.max(1,
                                    java.time.Duration.between(
                                            LocalDateTime.now(), lockedUntil)
                                            .toMinutes())
                                    + " minute(s).");
                }

                // --- verify the password ---
                if (!passwordMatches(password, storedHash)) {
                    registerFailure(user.getUserId(),
                            rs.getInt("failed_login_attempts"));
                    return new AuthResult(null, "INVALID",
                            "Invalid email or password.");
                }

                // --- success ---
                registerSuccess(user.getUserId());
                user.setFailedLoginAttempts(0);

                return new AuthResult(user, "OK", "Signed in.");
            }
        }
    }

    /**
     * Compare a plain password with a stored BCrypt hash.
     *
     * <p>Returns false (instead of throwing) when the stored hash is
     * missing or malformed.
     */
    public boolean passwordMatches(String password, String storedHash) {

        if (password == null || storedHash == null
                || storedHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, storedHash);
        } catch (IllegalArgumentException e) {
            // The stored hash is not valid BCrypt — treat as a wrong
            // password rather than letting the exception reach the page.
            System.err.println("[ShopSphere] Stored password hash is not "
                    + "valid BCrypt for this account.");
            return false;
        }
    }

    /** Hash a plain password for storage. */
    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /** Change a password after verifying the old one. */
    public boolean changePassword(int userId, String oldPassword,
                                  String newPassword) throws SQLException {

        String sql = "SELECT password_hash FROM users WHERE user_id = ?";

        String storedHash;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                storedHash = rs.getString("password_hash");
            }
        }

        if (!passwordMatches(oldPassword, storedHash)) {
            return false;
        }

        String update =
                "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(update)) {

            statement.setString(1, hash(newPassword));
            statement.setInt(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Set a password directly (used by the reset-password flow). */
    public boolean setPassword(int userId, String newPassword)
            throws SQLException {

        String sql =
                "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, hash(newPassword));
            statement.setInt(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------

    /** Update the name and phone number on an account. */
    public boolean updateProfile(int userId, String fullName, String phone)
            throws SQLException {

        String sql = """
                UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, phone);
            statement.setInt(3, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Change the e-mail address (and mark it unverified again). */
    public boolean updateEmail(int userId, String email)
            throws SQLException {

        String sql = """
                UPDATE users SET email = ?, email_verified = 0 WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setInt(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Mark an e-mail address as verified. */
    public boolean markEmailVerified(int userId) throws SQLException {

        String sql = "UPDATE users SET email_verified = 1 WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }

    /** Load one account by id. */
    public User findById(int userId) throws SQLException {

        String sql = """
                SELECT user_id, full_name, email, password_hash, role,
                       phone, email_verified, failed_login_attempts,
                       locked_until, last_login_at
                FROM users WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    /** Load one account by e-mail. */
    public User findByEmail(String email) throws SQLException {

        String sql = """
                SELECT user_id, full_name, email, password_hash, role,
                       phone, email_verified, failed_login_attempts,
                       locked_until, last_login_at
                FROM users WHERE email = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    // ------------------------------------------------------------
    // Lockout bookkeeping
    // ------------------------------------------------------------

    /** Count one failed sign-in and lock the account when it goes too far. */
    private void registerFailure(int userId, int currentAttempts)
            throws SQLException {

        int attempts = currentAttempts + 1;

        String sql;
        if (attempts >= AppConfig.maxFailedLogins()) {
            sql = """
                    UPDATE users
                    SET failed_login_attempts = 0,
                        locked_until = ?
                    WHERE user_id = ?
                    """;
        } else {
            sql = """
                    UPDATE users
                    SET failed_login_attempts = ?
                    WHERE user_id = ?
                    """;
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (attempts >= AppConfig.maxFailedLogins()) {
                LocalDateTime until = LocalDateTime.now()
                        .plusMinutes(AppConfig.lockoutMinutes());
                statement.setTimestamp(1, Timestamp.valueOf(until));
                statement.setInt(2, userId);
            } else {
                statement.setInt(1, attempts);
                statement.setInt(2, userId);
            }

            statement.executeUpdate();
        }
    }

    /** Clear the failure counter and record the sign-in time. */
    private void registerSuccess(int userId) throws SQLException {

        String sql = """
                UPDATE users
                SET failed_login_attempts = 0,
                    locked_until = NULL,
                    last_login_at = ?
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setTimestamp(1,
                    Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(2, userId);

            statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------
    // Admin helpers
    // ------------------------------------------------------------

    /** Number of customer accounts. */
    public int countCustomers() throws SQLException {

        String sql = "SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    /** Every account, newest first (admin user list). */
    public java.util.List<User> findAll() throws SQLException {

        String sql = """
                SELECT user_id, full_name, email, password_hash, role,
                       phone, email_verified, failed_login_attempts,
                       locked_until, last_login_at
                FROM users
                ORDER BY user_id DESC
                """;

        java.util.List<User> users = new java.util.ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }

        return users;
    }

    /** Change a user's role (admin only). */
    public boolean updateRole(int userId, String role) throws SQLException {

        if (!"ADMIN".equals(role) && !"CUSTOMER".equals(role)) {
            return false;
        }

        String sql = "UPDATE users SET role = ? WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, role);
            statement.setInt(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Unlock an account that was locked after failed sign-ins. */
    public boolean unlock(int userId) throws SQLException {

        String sql = """
                UPDATE users
                SET failed_login_attempts = 0, locked_until = NULL
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------

    private User mapUser(ResultSet rs) throws SQLException {

        User user = new User();

        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setPhone(rs.getString("phone"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));

        Timestamp locked = rs.getTimestamp("locked_until");
        if (locked != null) {
            user.setLockedUntil(locked.toLocalDateTime());
        }

        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) {
            user.setLastLoginAt(lastLogin.toLocalDateTime());
        }

        return user;
    }
}
