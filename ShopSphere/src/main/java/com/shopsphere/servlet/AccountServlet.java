package com.shopsphere.servlet;

import com.shopsphere.config.AppConfig;
import com.shopsphere.dao.AccountDAO;
import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.PaymentDAO;
import com.shopsphere.dao.ReviewDAO;
import com.shopsphere.dao.UserDAO;
import com.shopsphere.model.User;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * The customer's account area.
 *
 *   GET  /account                → profile, password, orders, payments
 *   POST /account action=profile → change name and phone
 *   POST /account action=password → change the password
 */
@WebServlet("/account")
public class AccountServlet extends HttpServlet {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^$|^[0-9]{10}$");

    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        int userId = user.getUserId();

        try {
            // Always show the freshest copy of the account.
            User fresh = userDAO.findById(userId);
            if (fresh != null) {
                request.setAttribute("account", fresh);
                request.getSession().setAttribute("user", fresh);
                request.getSession().setAttribute("userName",
                        fresh.getFullName());
            }

            request.setAttribute("orders",
                    orderDAO.getOrdersByUser(userId));
            request.setAttribute("payments",
                    paymentDAO.findByUser(userId));
            request.setAttribute("reviews",
                    reviewDAO.findByUser(userId));
            request.setAttribute("csrfToken", Csrf.token(request));

            request.getRequestDispatcher("/account.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load your account.", e);
        }
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
            request.getRequestDispatcher("/account.jsp")
                   .forward(request, response);
            return;
        }

        String action = request.getParameter("action");
        User user = currentUser(request);

        try {
            if ("profile".equals(action)) {
                updateProfile(request, response, user);
            } else if ("password".equals(action)) {
                changePassword(request, response, user);
            } else {
                response.sendRedirect(request.getContextPath() + "/account");
            }

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update your account.", e);
        }
    }

    // ------------------------------------------------------------

    private void updateProfile(HttpServletRequest request,
                               HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {

        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));

        if (fullName.length() < 2) {
            request.setAttribute("error",
                    "Please enter your full name.");
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/account.jsp")
                   .forward(request, response);
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            request.setAttribute("error",
                    "The phone number must be 10 digits.");
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/account.jsp")
                   .forward(request, response);
            return;
        }

        userDAO.updateProfile(user.getUserId(), fullName, phone);

        // Keep the header in sync straight away.
        request.getSession().setAttribute("userName", fullName);
        request.getSession().setAttribute("user",
                userDAO.findById(user.getUserId()));

        response.sendRedirect(request.getContextPath()
                + "/account?updated=profile");
    }

    private void changePassword(HttpServletRequest request,
                                HttpServletResponse response, User user)
            throws SQLException, IOException, ServletException {

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirm = request.getParameter("confirmPassword");

        String error = null;

        if (oldPassword == null || oldPassword.isEmpty()) {
            error = "Please enter your current password.";
        } else if (newPassword == null || newPassword.length() < 8) {
            error = "The new password must be at least 8 characters.";
        } else if (!newPassword.equals(confirm)) {
            error = "The new passwords do not match.";
        }

        if (error == null
                && !userDAO.changePassword(user.getUserId(), oldPassword,
                        newPassword)) {
            error = "Your current password was not correct.";
        }

        if (error != null) {
            request.setAttribute("error", error);
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/account.jsp")
                   .forward(request, response);
            return;
        }

        // A password change invalidates every remembered device.
        accountDAO.deleteAllRememberTokens(user.getUserId());

        response.sendRedirect(request.getContextPath()
                + "/account?updated=password");
    }

    // ------------------------------------------------------------

    private User currentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute("user");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
