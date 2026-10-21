package com.shopsphere.servlet;

import com.shopsphere.dao.AccountDAO;
import com.shopsphere.dao.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Confirms an e-mail address from the link sent at registration.
 */
@WebServlet("/verify-email")
public class VerifyEmailServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");

        if (token == null || token.isBlank()) {
            response.sendRedirect(request.getContextPath()
                    + "/login?verified=invalid");
            return;
        }

        try {
            int userId = accountDAO.findUserByVerificationToken(token);

            if (userId <= 0) {
                response.sendRedirect(request.getContextPath()
                        + "/login?verified=expired");
                return;
            }

            userDAO.markEmailVerified(userId);
            accountDAO.useVerificationToken(token);

            // Keep the session in step if the customer is signed in.
            Object user = request.getSession(false) == null
                    ? null : request.getSession().getAttribute("user");

            if (user instanceof com.shopsphere.model.User signedIn
                    && signedIn.getUserId() == userId) {
                signedIn.setEmailVerified(true);
                request.getSession().setAttribute("emailVerified", true);
            }

            response.sendRedirect(request.getContextPath()
                    + "/login?verified=1");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to verify your e-mail address.", e);
        }
    }
}
