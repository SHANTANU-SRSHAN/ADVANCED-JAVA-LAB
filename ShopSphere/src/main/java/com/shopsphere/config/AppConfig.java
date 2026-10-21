package com.shopsphere.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Application configuration.
 *
 * <p>Values are resolved in this order (first match wins):
 * <ol>
 *   <li>JVM system property — {@code -Dshopsphere.db.url=...}</li>
 *   <li>Environment variable — {@code SHOSPHERE_DB_URL=...}</li>
 *   <li>Properties file — {@code shopsphere.properties} next to the
 *       application, or on the classpath</li>
 *   <li>Built-in default</li>
 * </ol>
 *
 * <p>This keeps database credentials out of the Java source, which is
 * the one thing that should never be committed to a repository.
 */
public final class AppConfig {

    private static final Properties FILE_PROPERTIES = loadProperties();

    private AppConfig() {
    }

    // ------------------------------------------------------------
    // Database
    // ------------------------------------------------------------

    public static String dbUrl() {
        return get("shopsphere.db.url", "SHOSPHERE_DB_URL",
                "jdbc:mysql://localhost:3306/shopsphere"
                        + "?useSSL=false&allowPublicKeyRetrieval=true"
                        + "&serverTimezone=Asia/Kolkata");
    }

    public static String dbUser() {
        return get("shopsphere.db.user", "SHOSPHERE_DB_USER", "root");
    }

    public static String dbPassword() {
        return get("shopsphere.db.password", "SHOSPHERE_DB_PASSWORD", "");
    }

    // ------------------------------------------------------------
    // Shop settings
    // ------------------------------------------------------------

    /** Tax applied to the discounted subtotal, in percent. */
    public static BigDecimal taxPercent() {
        return decimal("shopsphere.tax.percent", "SHOSPHERE_TAX_PERCENT", "5");
    }

    /** Free-shipping threshold — orders above this ship for nothing. */
    public static BigDecimal freeShippingThreshold() {
        return decimal("shopsphere.shipping.free.threshold",
                "SHOSPHERE_FREE_SHIPPING_THRESHOLD", "1999");
    }

    /** How long a "remember me" login stays valid, in days. */
    public static int rememberMeDays() {
        return getInt("shopsphere.remember.days", "SHOSPHERE_REMEMBER_DAYS", 30);
    }

    /** How long a password-reset / e-mail-verification link stays valid. */
    public static int tokenExpiryHours() {
        return getInt("shopsphere.token.hours", "SHOSPHERE_TOKEN_HOURS", 24);
    }

    /** Failed sign-ins before the account is locked. */
    public static int maxFailedLogins() {
        return getInt("shopsphere.login.max.attempts",
                "SHOSPHERE_MAX_FAILED_LOGINS", 5);
    }

    /** How long an account stays locked, in minutes. */
    public static int lockoutMinutes() {
        return getInt("shopsphere.login.lockout.minutes",
                "SHOSPHERE_LOCKOUT_MINUTES", 15);
    }

    /** Name of the "remember me" cookie. */
    public static String rememberCookieName() {
        return get("shopsphere.cookie.remember", "SHOSPHERE_COOKIE_REMEMBER",
                "ss_remember");
    }

    /** Name of the CSRF cookie (a readable copy for JavaScript forms). */
    public static String csrfCookieName() {
        return get("shopsphere.cookie.csrf", "SHOSPHERE_COOKIE_CSRF", "ss_csrf");
    }

    /** Dev flag — when true, e-mail links are printed to the console. */
    public static boolean devMode() {
        return Boolean.parseBoolean(
                get("shopsphere.dev", "SHOSPHERE_DEV", "true"));
    }

    // ------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------

    private static String get(String propertyKey, String envKey,
                              String defaultValue) {

        String value = System.getProperty(propertyKey);

        if (isBlank(value)) {
            value = System.getenv(envKey);
        }

        if (isBlank(value)) {
            value = FILE_PROPERTIES.getProperty(propertyKey);
        }

        return isBlank(value) ? defaultValue : value.trim();
    }

    private static int getInt(String propertyKey, String envKey,
                              int defaultValue) {
        try {
            return Integer.parseInt(get(propertyKey, envKey,
                    String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static BigDecimal decimal(String propertyKey, String envKey,
                                       String defaultValue) {
        try {
            return new BigDecimal(get(propertyKey, envKey, defaultValue));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Properties loadProperties() {

        Properties properties = new Properties();

        // 1) An explicit path given on the command line
        //    -Dshopsphere.config.file=C:\path\to\shopsphere.properties
        String explicit = System.getProperty("shopsphere.config.file");

        if (explicit != null && !explicit.isBlank()) {
            Path path = Path.of(explicit);
            if (Files.exists(path)) {
                if (readInto(properties, path)) {
                    return properties;
                }
            } else {
                System.err.println("[ShopSphere] Config file not found: " + path);
            }
        }

        // 2) Next to the application (the working directory)
        Path working = Path.of("shopsphere.properties");
        if (Files.exists(working) && readInto(properties, working)) {
            return properties;
        }

        // 3) Inside Tomcat's own configuration folder
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.isBlank()) {
            Path inTomcat = Path.of(catalinaBase, "conf", "shopsphere.properties");
            if (Files.exists(inTomcat) && readInto(properties, inTomcat)) {
                return properties;
            }
        }

        // 4) Bundled on the classpath (WEB-INF/classes)
        try (InputStream in = AppConfig.class
                .getClassLoader().getResourceAsStream("shopsphere.properties")) {

            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            System.err.println("[ShopSphere] Could not read classpath "
                    + "shopsphere.properties: " + e.getMessage());
        }

        return properties;
    }

    /** Read one properties file into the given set. */
    private static boolean readInto(Properties properties, Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            return true;
        } catch (IOException e) {
            System.err.println("[ShopSphere] Could not read "
                    + path.toAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }
}
