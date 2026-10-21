package com.shopsphere.servlet;

import com.shopsphere.dao.OrderDAO;
import com.shopsphere.model.Order;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Admin order management: browse all orders and move them through
 * PLACED → CONFIRMED → PACKED → SHIPPED → DELIVERED (or CANCELLED).
 *
 *   GET  /admin/orders?status=SHIPPED → filtered list
 *   POST /admin/orders action=status   → update one order
 *   POST /admin/orders action=tracking → add a tracking number
 */
@WebServlet("/admin/orders")
public class AdminOrdersServlet extends HttpServlet {

    private static final Set<String> STATUSES = Set.of(
            "ALL", "PLACED", "CONFIRMED", "PACKED", "SHIPPED", "DELIVERED",
            "CANCELLED", "REFUNDED");

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String status = request.getParameter("status");
        if (status == null || !STATUSES.contains(status)) {
            status = "ALL";
        }

        try {
            List<Order> orders = orderDAO.getAllOrders(status);

            request.setAttribute("orders", orders);
            request.setAttribute("statusFilter", status);

            request.getRequestDispatcher("/admin/orders.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load orders for the admin area.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int orderId = parseInt(request.getParameter("orderId"), -1);

        try {
            if ("tracking".equals(action) && orderId > 0) {

                String trackingNumber =
                        trim(request.getParameter("trackingNumber"));

                boolean updated = orderDAO.setTrackingNumber(
                        orderId, trackingNumber);

                response.sendRedirect(request.getContextPath()
                        + "/admin/orders?"
                        + (updated ? "updated=1" : "error=Update+failed."));

                return;
            }

            if ("status".equals(action) && orderId > 0) {

                String status = request.getParameter("status");
                String note = trim(request.getParameter("note"));
                String changedBy = adminName(request);

                boolean updated = orderDAO.updateStatus(
                        orderId, status, note, changedBy);

                response.sendRedirect(request.getContextPath()
                        + "/admin/orders?"
                        + (updated ? "updated=1" : "error=Update+failed."));

                return;
            }

            response.sendRedirect(request.getContextPath()
                    + "/admin/orders");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update the order status.", e);
        }
    }

    /** The e-mail of the admin making the change. */
    private String adminName(HttpServletRequest request) {

        Object user = request.getSession().getAttribute("user");

        if (user instanceof com.shopsphere.model.User admin) {
            return admin.getEmail();
        }

        return "admin";
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
