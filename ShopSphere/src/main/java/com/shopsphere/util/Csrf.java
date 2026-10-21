package com.shopsphere.util;

import com.shopsphere.config.AppConfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Cross-Site Request Forgery protection.
 *
 * <p>Every state-changing form in the application carries a hidden
 * {@code csrfToken} field. The token is created once per session and
 * compared with a constant-time check before the request is allowed to
 * change anything.
 *
 * <p>A copy is also written to a readable cookie so that JavaScript can
 * pick the token up from a single place.
 */
public final class Csrf {

    /** Session attribute that holds the token for the current session. */
    public static final String SESSION_KEY = "csrfToken";

    /** Name of the form field / request parameter. */
    public static final String PARAMETER = "csrfToken";

    private Csrf() {
    }

    /**
     * Return the token for this session, creating one if needed.
     */
    public static String token(HttpServletRequest request) {

        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SESSION_KEY);

        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }

        String token = TokenUtil.newToken(24);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /**
     * Make sure the request carries the session's token.
     *
     * @return true when the request is safe to process
     */
    public static boolean isValid(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object expected = session.getAttribute(SESSION_KEY);
        if (!(expected instanceof String) || expected.toString().isBlank()) {
            return false;
        }

        String supplied = request.getParameter(PARAMETER);
        if (supplied == null || supplied.isBlank()) {
            // Fall back to the header, for fetch()/XHR calls.
            supplied = request.getHeader("X-CSRF-Token");
        }

        return TokenUtil.equalsConstantTime(expected.toString(), supplied);
    }

    /**
     * Write the token into a readable cookie so JavaScript can read it.
     */
    public static void writeCookie(HttpServletRequest request,
                                   HttpServletResponse response) {

        String token = token(request);

        jakarta.servlet.http.Cookie cookie =
                new jakarta.servlet.http.Cookie(
                        AppConfig.csrfCookieName(), token);

        cookie.setPath(request.getContextPath().isEmpty()
                ? "/" : request.getContextPath());
        cookie.setHttpOnly(false);       // JavaScript must be able to read it
        cookie.setMaxAge(60 * 60 * 24);  // one day
        response.addCookie(cookie);
    }
}
