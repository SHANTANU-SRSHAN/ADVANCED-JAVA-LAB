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
import java.util.regex.Pattern;

/**
 * Account registration.
 *
 * <p>Validates the form, stores the user with a BCrypt hash, creates an
 * e-mail verification token and then sends the visitor to the login page.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Passwords must have a letter and a number. */
    private static final Pattern STRONG_PASSWORD =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Already logged in? Nothing to do here.
        if (request.getSession(false) != null
                && request.getSession().getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/products");
            return;
        }

        request.setAttribute("csrfToken", Csrf.token(request));

        request.getRequestDispatcher("/register.jsp")
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
            request.getRequestDispatcher("/register.jsp")
                   .forward(request, response);
            return;
        }

        String fullName = trim(request.getParameter("fullName"));
        String email = trim(request.getParameter("email")).toLowerCase();
        String phone = trim(request.getParameter("phone"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirmPassword");

        // Keep what the visitor typed so the form does not go blank.
        request.setAttribute("fullName", fullName);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);

        String error = validate(fullName, email, phone, password, confirm);

        if (error == null) {
            try {
                if (userDAO.emailExists(email)) {
                    error = "An account with this email already exists.";

                } else if (!userDAO.registerUser(fullName, email, password)) {
                    error = "Registration failed. Please try again.";

                } else {
                    // Send a verification link (printed in dev mode).
                    com.shopsphere.model.User created =
                            userDAO.findByEmail(email);
                    if (created != null) {
                        String token = accountDAO
                                .createEmailVerificationToken(created.getUserId());
                        logVerificationLink(request, created.getEmail(), token);
                    }
                }

            } catch (SQLException e) {
                throw new ServletException(
                        "Unable to register because of a database error.", e);
            }
        }

        if (error != null) {
            request.setAttribute("error", error);
            request.setAttribute("csrfToken", Csrf.token(request));
            request.getRequestDispatcher("/register.jsp")
                   .forward(request, response);
            return;
        }

        response.sendRedirect(
                request.getContextPath() + "/login?registered=1");
    }

    // ------------------------------------------------------------

    private String validate(String fullName, String email, String phone,
                            String password, String confirm) {

        if (fullName.length() < 2) {
            return "Please enter your full name.";
        }
        if (fullName.length() > 100) {
            return "Full name must be 100 characters or fewer.";
        }
        if (email.isEmpty()
                || !EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9]{10}$")) {
            return "The phone number must be 10 digits.";
        }
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (password.length() > 64) {
            return "Password must be 64 characters or fewer.";
        }
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            return "Password must contain at least one letter "
                    + "and one number.";
        }
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }

        return null;
    }

    /**
     * There is no mail server in this project, so the verification link
     * is written to the console. In a real deployment this is where an
     * e-mail would be sent.
     */
    private void logVerificationLink(HttpServletRequest request,
                                     String email, String token) {

        String link = request.getContextPath()
                + "/verify-email?token=" + token;

        System.out.println("[ShopSphere] Verification link for " + email
                + " → " + request.getScheme() + "://"
                + request.getServerName() + ":"
                + request.getServerPort() + link);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
