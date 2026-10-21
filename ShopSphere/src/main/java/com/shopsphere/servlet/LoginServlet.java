package com.shopsphere.servlet;

import com.shopsphere.config.AppConfig;
import com.shopsphere.dao.AccountDAO;
import com.shopsphere.dao.UserDAO;
import com.shopsphere.model.User;
import com.shopsphere.service.CartService;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Sign-in.
 *
 *   GET  /login  → the sign-in form
 *   POST /login  → verify the password, start the session and, when
 *                  "remember me" was ticked, issue a long-lived
 *                  login cookie.
 *
 * <p>After signing in the guest cart is merged into the customer's
 * account cart, so the cart badge and the cart page agree with each
 * other straight away.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Already signed in? Nothing to do here.
        if (request.getSession(false) != null
                && request.getSession().getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        request.setAttribute("csrfToken", Csrf.token(request));
        request.setAttribute("next", safeNext(request.getParameter("next")));

        request.getRequestDispatcher("/login.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // ---- CSRF ----
        if (!Csrf.isValid(request)) {
            request.setAttribute("error",
                    "Your session expired. Please try again.");
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/login.jsp")
                   .forward(request, response);
            return;
        }

        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");
        String next = safeNext(request.getParameter("next"));
        boolean rememberMe = "on".equals(request.getParameter("rememberMe"))
                || "1".equals(request.getParameter("rememberMe"));

        if (email.isEmpty() || password == null || password.isEmpty()) {
            fail(request, response, email,
                    "Please enter your email and password.");
            return;
        }

        try {
            UserDAO.AuthResult result = userDAO.authenticate(
                    email.toLowerCase(), password);

            if (!result.isSuccess()) {
                fail(request, response, email, result.message());
                return;
            }

            User user = result.user();

            // ---- start the session ----
            HttpSession session = request.getSession(true);
            request.changeSessionId();

            session.setAttribute("user", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userName", user.getFullName());
            session.setAttribute("userRole", user.getRole());
            session.setAttribute("emailVerified", user.isEmailVerified());

            // ---- merge the guest cart into the account cart ----
            int merged = cartService.mergeGuestCartIntoAccount(
                    request, user.getUserId());

            if (merged > 0) {
                session.setAttribute("cartMerged", merged);
            }

            // ---- "remember me" ----
            if (rememberMe) {
                issueRememberCookie(request, response, user);
            } else {
                clearRememberCookie(request, response);
            }

            // ---- where to go next ----
            String target = (next != null && !next.isBlank())
                    ? next
                    : "/products";

            response.sendRedirect(request.getContextPath() + target);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to log in because of a database error.", e);
        }
    }

    // ------------------------------------------------------------

    /** Send the visitor back to the form with a message. */
    private void fail(HttpServletRequest request,
                      HttpServletResponse response,
                      String email, String message)
            throws ServletException, IOException {

        request.setAttribute("error", message);
        request.setAttribute("email", email);
        request.setAttribute("csrfToken", Csrf.token(request));

        request.getRequestDispatcher("/login.jsp")
               .forward(request, response);
    }

    /** Give the browser a long-lived "remember me" cookie. */
    private void issueRememberCookie(HttpServletRequest request,
                                     HttpServletResponse response,
                                     User user) throws SQLException {

        String token = accountDAO.createRememberToken(
                user.getUserId(), request.getHeader("User-Agent"));

        Cookie cookie = new Cookie(
                AppConfig.rememberCookieName(), token);

        cookie.setPath(request.getContextPath().isEmpty()
                ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setMaxAge(AppConfig.rememberMeDays() * 24 * 60 * 60);

        response.addCookie(cookie);
    }

    /** Remove any existing "remember me" cookie. */
    private void clearRememberCookie(HttpServletRequest request,
                                     HttpServletResponse response) {

        Cookie cookie = new Cookie(
                AppConfig.rememberCookieName(), "");

        cookie.setPath(request.getContextPath().isEmpty()
                ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    /**
     * Only allow same-site redirects (prevents open-redirect attacks).
     * Returns null when the target is missing or unsafe.
     */
    private String safeNext(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        if (next.startsWith("/") && !next.startsWith("//")
                && !next.contains("\\") && !next.contains(":")) {
            return next;
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
