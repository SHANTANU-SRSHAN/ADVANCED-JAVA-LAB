package com.shopsphere.servlet;

import com.shopsphere.dao.AdminDAO;
import com.shopsphere.dao.OrderDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Admin home — store statistics and the newest orders.
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    /** A product with this many units or fewer is "low stock". */
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final AdminDAO adminDAO = new AdminDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            Map<String, Object> stats = adminDAO.getDashboardStats();

            request.setAttribute("stats", stats);
            request.setAttribute("statusCounts",
                    adminDAO.getOrdersByStatus());
            request.setAttribute("lowStock",
                    adminDAO.getLowStockProducts(LOW_STOCK_THRESHOLD));
            request.setAttribute("dailyRevenue",
                    adminDAO.getDailyRevenue(7));
            request.setAttribute("inventoryValue",
                    adminDAO.getInventoryValue());
            request.setAttribute("topProducts",
                    adminDAO.getTopProducts(5));

            // Newest five orders for the "recent activity" table.
            request.setAttribute("recentOrders",
                    orderDAO.getAllOrders("ALL").stream()
                            .limit(5)
                            .toList());

            request.getRequestDispatcher("/admin/dashboard.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load the dashboard.", e);
        }
    }
}
