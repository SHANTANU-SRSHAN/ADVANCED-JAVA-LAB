package com.shopsphere.servlet;

import com.shopsphere.config.AppConfig;
import com.shopsphere.dao.AccountDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Logs the customer out.
 *
 * <p>The session is destroyed and the "remember me" cookie is removed,
 * so the header, cart badge and private pages all return to the
 * "guest" state on the next page.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {
        logout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        logout(request, response);
    }

    private void logout(HttpServletRequest request,
                        HttpServletResponse response)
            throws IOException {

        // ---- drop the remember-me token as well ----
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AppConfig.rememberCookieName()
                        .equals(cookie.getName())) {
                    try {
                        accountDAO.deleteRememberToken(cookie.getValue());
                    } catch (SQLException e) {
                        // Not being able to clean the token up must not
                        // stop the customer from signing out.
                    }
                }
            }
        }

        Cookie expired = new Cookie(
                AppConfig.rememberCookieName(), "");

        expired.setPath(request.getContextPath().isEmpty()
                ? "/" : request.getContextPath());
        expired.setHttpOnly(true);
        expired.setMaxAge(0);

        response.addCookie(expired);

        // ---- end the session ----
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        // Prevent the browser from showing a cached "logged in" page.
        response.setHeader("Cache-Control",
                "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        response.sendRedirect(request.getContextPath() + "/home");
    }
}
