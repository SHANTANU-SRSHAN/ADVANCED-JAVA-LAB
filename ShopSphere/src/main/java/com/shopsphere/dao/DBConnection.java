package com.shopsphere.dao;

import com.shopsphere.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place for obtaining a database connection.
 *
 * <p>Credentials are no longer hard-coded here — they come from
 * {@link AppConfig}, which reads system properties, environment
 * variables or a {@code shopsphere.properties} file.
 *
 * <p>See {@code shopsphere.properties.example} for the settings and
 * {@code database/README.md} for how to point the app at your MySQL.
 */
public final class DBConnection {

    private DBConnection() {
    }

    /**
     * Open a new connection. The caller is responsible for closing it —
     * the DAOs use try-with-resources.
     */
    public static Connection getConnection() throws SQLException {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver class was not found.", e);
        }

        return DriverManager.getConnection(
                AppConfig.dbUrl(),
                AppConfig.dbUser(),
                AppConfig.dbPassword());
    }

    /**
     * Quick check used by the admin dashboard to show whether the
     * database is reachable.
     */
    public static boolean isReachable() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
