package com.shopsphere.servlet;

import com.shopsphere.dao.OrderDAO;
import com.shopsphere.model.User;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.shopsphere.model.Order;

/**
 * Order history for the logged-in customer.
 *
 *   GET  /orders          → list of my orders
 *   POST /orders action=cancel → cancel while it is still cancellable
 */
@WebServlet("/orders")
public class OrdersServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int userId = currentUserId(request);
            List<Order> orders = orderDAO.getOrdersByUser(userId);

            request.setAttribute("orders", orders);
            request.setAttribute("csrfToken", Csrf.token(request));

            request.getRequestDispatcher("/orders.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load your orders.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            response.sendRedirect(request.getContextPath()
                    + "/orders?error=session");
            return;
        }

        if (!"cancel".equals(request.getParameter("action"))) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }

        try {
            int orderId = Integer.parseInt(
                    request.getParameter("orderId"));

            boolean cancelled = orderDAO.cancelOrder(
                    orderId, currentUserId(request));

            response.sendRedirect(request.getContextPath() + "/orders?cancelled="
                    + (cancelled ? "1" : "0"));

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/orders");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to cancel the order.", e);
        }
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user.getUserId();
    }
}
