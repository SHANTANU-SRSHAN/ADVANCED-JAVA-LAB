package com.shopsphere.servlet;

import com.shopsphere.dao.UserDAO;
import com.shopsphere.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Admin user management.
 *
 *   GET  /admin/users                 → every account
 *   POST /admin/users action=role     → make somebody an admin (or back)
 *   POST /admin/users action=unlock   → release a locked account
 */
@WebServlet("/admin/users")
public class AdminUsersServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<User> users = userDAO.findAll();

            request.setAttribute("users", users);
            request.setAttribute("customerCount",
                    userDAO.countCustomers());

            request.getRequestDispatcher("/admin/users.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load the customer list.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int userId = parseInt(request.getParameter("userId"), -1);

        try {
            if ("role".equals(action) && userId > 0) {

                String role = request.getParameter("role");

                // Do not let the last admin demote themselves.
                if ("CUSTOMER".equals(role)
                        && isLastAdmin(userId)) {
                    response.sendRedirect(request.getContextPath()
                            + "/admin/users?error=You+cannot+remove+"
                            + "the+last+administrator.");
                    return;
                }

                boolean updated = userDAO.updateRole(userId, role);

                response.sendRedirect(request.getContextPath()
                        + "/admin/users?" + (updated ? "updated=1"
                        : "error=Could+not+update+the+account."));

                return;
            }

            if ("unlock".equals(action) && userId > 0) {
                userDAO.unlock(userId);
                response.sendRedirect(request.getContextPath()
                        + "/admin/users?updated=1");
                return;
            }

            response.sendRedirect(request.getContextPath() + "/admin/users");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update the account.", e);
        }
    }

    /** True when this is the only administrator left. */
    private boolean isLastAdmin(int userId) throws SQLException {

        List<User> users = userDAO.findAll();

        int admins = 0;
        boolean thisOneIsAdmin = false;

        for (User user : users) {
            if (user.isAdmin()) {
                admins++;
                if (user.getUserId() == userId) {
                    thisOneIsAdmin = true;
                }
            }
        }

        return thisOneIsAdmin && admins <= 1;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
