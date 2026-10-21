package com.shopsphere.servlet;

import com.shopsphere.dao.AccountDAO;
import com.shopsphere.dao.UserDAO;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * "Forgot password" — the first half of the reset flow.
 *
 * <p>A one-time token is created and a reset link is produced. The
 * message shown to the visitor is the same whether or not the address
 * exists, so the form cannot be used to discover which e-mails are
 * registered.
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    private static final String CONFIRMATION_MESSAGE =
            "If an account exists for that address, a reset link "
                    + "has been created. Please check your e-mail "
                    + "(and the application console in development).";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Show the confirmation once after the POST redirects back here,
        // so refreshing the page does not resubmit the form.
        Object flash = request.getSession()
                .getAttribute("forgotPasswordMessage");
        if (flash != null) {
            request.setAttribute("message", flash);
            request.getSession().removeAttribute("forgotPasswordMessage");
        }

        request.setAttribute("csrfToken", Csrf.token(request));

        request.getRequestDispatcher("/forgot-password.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            request.setAttribute("error",
                    "Your session expired. Please try again.");
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/forgot-password.jsp")
                   .forward(request, response);
            return;
        }

        String email = trim(request.getParameter("email")).toLowerCase();

        if (email.isEmpty()) {
            request.setAttribute("error",
                    "Please enter the e-mail address of your account.");
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/forgot-password.jsp")
                   .forward(request, response);
            return;
        }

        try {
            com.shopsphere.model.User user = userDAO.findByEmail(email);

            if (user != null) {
                String token =
                        accountDAO.createPasswordResetToken(user.getUserId());
                logResetLink(request, user.getEmail(), token);
            }

            // The same message either way — the form must not reveal
            // which e-mail addresses are registered.  Stored as a
            // one-shot flash message for the redirect target.
            request.getSession().setAttribute(
                    "forgotPasswordMessage", CONFIRMATION_MESSAGE);
            response.sendRedirect(request.getContextPath()
                    + "/forgot-password");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to start the password reset.", e);
        }
    }

    private void logResetLink(HttpServletRequest request, String email,
                              String token) {

        String link = request.getContextPath()
                + "/reset-password?token=" + token;

        System.out.println("[ShopSphere] Password reset link for " + email
                + " → " + request.getScheme() + "://"
                + request.getServerName() + ":"
                + request.getServerPort() + link);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
