package com.shopsphere.filter;

import com.shopsphere.config.AppConfig;
import com.shopsphere.dao.AccountDAO;
import com.shopsphere.dao.UserDAO;
import com.shopsphere.dao.WishlistDAO;
import com.shopsphere.model.User;
import com.shopsphere.service.CartService;
import com.shopsphere.util.Csrf;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;

/**
 * One filter for the whole application. On every request it:
 *
 *  1. Restores a session from the "remember me" cookie when the
 *     visitor comes back after the session expired.
 *  2. Keeps the session "in sync" — user name / role / cart badge /
 *     wishlist badge are refreshed so the header looks the same on
 *     every page.
 *  3. Sends anonymous visitors to the login page for the private pages
 *     (checkout, payment, orders, wishlist).
 *  4. Keeps non-admin users away from /admin.
 *  5. Publishes the CSRF token so every form can carry it.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    /**
     * Pages that require a logged-in customer.
     *
     * <p>Note that "/login", "/register", "/forgot-password" and
     * "/reset-password" are deliberately NOT in this list — they are
     * the pages a visitor needs when they are not signed in.
     */
    private static final Set<String> PROTECTED_PAGES = Set.of(
            "/checkout", "/payment", "/orders", "/order", "/wishlist",
            "/account", "/addresses");

    private CartService cartService;
    private UserDAO userDAO;
    private AccountDAO accountDAO;
    private WishlistDAO wishlistDAO;

    @Override
    public void init(FilterConfig filterConfig) {
        cartService = new CartService();
        userDAO = new UserDAO();
        accountDAO = new AccountDAO();
        wishlistDAO = new WishlistDAO();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI()
                .substring(req.getContextPath().length());

        // ---- 1. restore a session from the remember-me cookie ----
        HttpSession session = req.getSession(false);
        User user = session == null
                ? null : (User) session.getAttribute("user");

        if (user == null) {
            user = restoreFromRememberCookie(req);
            if (user != null) {
                session = req.getSession(true);
                session.setAttribute("user", user);
            }
        }

        if (session == null) {
            session = req.getSession(true);
        }

        // ---- 2. keep the session attributes in sync ----
        if (user != null) {
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("userName", user.getFullName());
            session.setAttribute("userRole", user.getRole());
            session.setAttribute("emailVerified", user.isEmailVerified());

            try {
                session.setAttribute("cartCount",
                        cartService.count(req));

                session.setAttribute("wishlistCount",
                        wishlistDAO.count(user.getUserId()));

            } catch (SQLException e) {
                // Never block the page because a badge failed.
                session.setAttribute("cartCount", 0);
                session.setAttribute("wishlistCount", 0);
            }

        } else {
            session.removeAttribute("cartCount");
            session.removeAttribute("wishlistCount");
        }

        // ---- 3. publish the CSRF token ----
        Csrf.writeCookie(req, res);

        // ---- 4. login required? ----
        if (user == null && requiresLogin(path)) {
            String next = path;
            if (req.getQueryString() != null) {
                next += "?" + req.getQueryString();
            }

            res.sendRedirect(req.getContextPath()
                    + "/login?next="
                    + URLEncoder.encode(next, StandardCharsets.UTF_8));
            return;
        }

        // ---- 5. admin area ----
        if (path.startsWith("/admin")
                && (user == null || !user.isAdmin())) {
            res.sendRedirect(req.getContextPath() + "/home");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Look for a valid "remember me" cookie and load the account it
     * belongs to.
     */
    private User restoreFromRememberCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String token = null;
        for (Cookie cookie : cookies) {
            if (AppConfig.rememberCookieName().equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            int userId = accountDAO.findUserByRememberToken(token);

            if (userId <= 0) {
                return null;
            }

            return userDAO.findById(userId);

        } catch (SQLException e) {
            return null;
        }
    }

    private boolean requiresLogin(String path) {
        for (String page : PROTECTED_PAGES) {
            if (path.equals(page) || path.startsWith(page + "/")) {
                return true;
            }
        }
        return false;
    }
}
