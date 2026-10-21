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
 * "Reset password" — the second half of the reset flow.
 *
 * <p>The token in the link identifies the account. Once the password is
 * changed the token is burned, every remembered device is signed out,
 * and the customer is sent to the login page.
 */
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String token = trim(request.getParameter("token"));

        request.setAttribute("csrfToken", Csrf.token(request));
        request.setAttribute("token", token);

        if (token.isEmpty()) {
            request.setAttribute("error",
                    "That reset link is not valid. Please request a new one.");
            request.getRequestDispatcher("/reset-password.jsp")
                   .forward(request, response);
            return;
        }

        try {
            if (accountDAO.findUserByResetToken(token) <= 0) {
                request.setAttribute("error",
                        "That reset link has expired or has already been "
                                + "used. Please request a new one.");
                request.getRequestDispatcher("/reset-password.jsp")
                       .forward(request, response);
                return;
            }

            request.getRequestDispatcher("/reset-password.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to open the reset form.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String token = trim(request.getParameter("token"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");

        request.setAttribute("token", token);
        request.setAttribute("csrfToken", Csrf.token(request));

        if (!Csrf.isValid(request)) {
            request.setAttribute("error",
                    "Your session expired. Please try again.");
            request.getRequestDispatcher("/reset-password.jsp")
                   .forward(request, response);
            return;
        }

        if (password == null || password.length() < 8) {
            request.setAttribute("error",
                    "The new password must be at least 8 characters.");
            request.getRequestDispatcher("/reset-password.jsp")
                   .forward(request, response);
            return;
        }

        if (!password.equals(confirm)) {
            request.setAttribute("error",
                    "The new passwords do not match.");
            request.getRequestDispatcher("/reset-password.jsp")
                   .forward(request, response);
            return;
        }

        try {
            int userId = accountDAO.findUserByResetToken(token);

            if (userId <= 0) {
                request.setAttribute("error",
                        "That reset link has expired or has already been "
                                + "used. Please request a new one.");
                request.getRequestDispatcher("/reset-password.jsp")
                       .forward(request, response);
                return;
            }

            userDAO.setPassword(userId, password);

            // Burn the token and sign out every remembered device.
            accountDAO.invalidatePasswordResetTokens(userId);
            accountDAO.deleteAllRememberTokens(userId);

            response.sendRedirect(request.getContextPath()
                    + "/login?reset=1");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to reset your password.", e);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
