package com.shopsphere.servlet;

import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.PaymentDAO;
import com.shopsphere.model.Order;
import com.shopsphere.model.Payment;
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
 * Order detail page — doubles as the order confirmation screen
 * (?placed=1) and the tracking view for one order.
 */
@WebServlet("/order")
public class OrderServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        int orderId;
        try {
            orderId = Integer.parseInt(request.getParameter("id"));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Order ID is required.");
            return;
        }

        User user = (User) request.getSession().getAttribute("user");
        boolean isAdmin = user.isAdmin();

        try {
            // Customers only see their own orders; admins see any.
            Order order = orderDAO.getOrder(
                    orderId, isAdmin ? null : user.getUserId());

            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Order not found.");
                return;
            }

            List<Payment> payments = paymentDAO.findByOrder(orderId);

            request.setAttribute("order", order);
            request.setAttribute("payments", payments);
            request.setAttribute("placed",
                    "1".equals(request.getParameter("placed")));

            request.getRequestDispatcher("/order.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to load the order.", e);
        }
    }
}
