package com.shopsphere.servlet;

import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.PaymentDAO;
import com.shopsphere.model.Order;
import com.shopsphere.model.Payment;
import com.shopsphere.model.Refund;
import com.shopsphere.payment.GatewayResult;
import com.shopsphere.payment.PaymentGateway;
import com.shopsphere.payment.PaymentGateways;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin refunds.
 *
 *   GET  /admin/refunds                → every refund
 *   POST /admin/refunds action=refund  → refund a paid order
 */
@WebServlet("/admin/refunds")
public class AdminRefundsServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            request.setAttribute("refunds", findRefunds());
            request.setAttribute("payments",
                    findRefundablePayments());

            request.getRequestDispatcher("/admin/refunds.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load the refunds.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!"refund".equals(request.getParameter("action"))) {
            response.sendRedirect(request.getContextPath()
                    + "/admin/refunds");
            return;
        }

        int paymentId = parseInt(request.getParameter("paymentId"), -1);
        String reason = trim(request.getParameter("reason"));

        if (reason.isEmpty()) {
            response.sendRedirect(request.getContextPath()
                    + "/admin/refunds?error=Please+give+a+reason+"
                    + "for+the+refund.");
            return;
        }

        try {
            Payment payment = paymentDAO.findById(paymentId);

            if (payment == null || !payment.isSuccessful()) {
                response.sendRedirect(request.getContextPath()
                        + "/admin/refunds?error=That+payment+cannot+"
                        + "be+refunded.");
                return;
            }

            // ---- ask the gateway to reverse the charge ----
            PaymentGateway gateway = PaymentGateways.create();

            GatewayResult result = gateway.refund(
                    payment.getTransactionId(),
                    payment.getAmount(),
                    reason);

            if (!result.success()) {
                response.sendRedirect(request.getContextPath()
                        + "/admin/refunds?error=The+refund+was+declined.");
                return;
            }

            // ---- record it ----
            insertRefund(payment, reason);

            paymentDAO.updateStatus(payment.getPaymentId(), "REFUNDED",
                    null);

            orderDAO.markRefunded(payment.getOrderId());

            response.sendRedirect(request.getContextPath()
                    + "/admin/refunds?refunded=1");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to process the refund.", e);
        }
    }

    // ------------------------------------------------------------

    private void insertRefund(Payment payment, String reason)
            throws SQLException {

        String sql = """
                INSERT INTO refunds
                    (payment_id, order_id, user_id, amount, reason,
                     status, processed_by)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', ?)
                """;

        try (Connection connection =
                     com.shopsphere.dao.DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, payment.getPaymentId());
            statement.setInt(2, payment.getOrderId());
            statement.setInt(3, payment.getUserId());
            statement.setBigDecimal(4, payment.getAmount());
            statement.setString(5, reason);
            statement.setString(6, "admin");

            statement.executeUpdate();
        }
    }

    /** Every refund, newest first. */
    private List<Refund> findRefunds() throws SQLException {

        String sql = """
                SELECT r.refund_id, r.payment_id, r.order_id, r.user_id,
                       r.amount, r.reason, r.status, r.processed_by,
                       r.created_at,
                       u.full_name AS customer_name, o.payment_method
                FROM refunds r
                JOIN orders o ON o.order_id = r.order_id
                JOIN users  u ON u.user_id  = r.user_id
                ORDER BY r.refund_id DESC
                """;

        List<Refund> refunds = new ArrayList<>();

        try (Connection connection =
                     com.shopsphere.dao.DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             java.sql.ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Refund refund = new Refund();

                refund.setRefundId(rs.getInt("refund_id"));
                refund.setPaymentId(rs.getInt("payment_id"));
                refund.setOrderId(rs.getInt("order_id"));
                refund.setUserId(rs.getInt("user_id"));
                refund.setAmount(rs.getBigDecimal("amount"));
                refund.setReason(rs.getString("reason"));
                refund.setStatus(rs.getString("status"));
                refund.setProcessedBy(rs.getString("processed_by"));
                refund.setCustomerName(rs.getString("customer_name"));
                refund.setPaymentMethod(rs.getString("payment_method"));

                java.sql.Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    refund.setCreatedAt(created.toLocalDateTime());
                }

                refunds.add(refund);
            }
        }

        return refunds;
    }

    /** Payments that can still be refunded. */
    private List<Payment> findRefundablePayments() throws SQLException {

        String sql = """
                SELECT p.payment_id, p.order_id, p.user_id, p.gateway,
                       p.method, p.transaction_id, p.reference_id,
                       p.amount, p.currency, p.status, p.failure_reason,
                       p.card_brand, p.card_last4, p.upi_id, p.bank_name,
                       p.created_at, p.updated_at
                FROM payments p
                JOIN orders o ON o.order_id = p.order_id
                WHERE p.status = 'SUCCESS'
                  AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
                ORDER BY p.payment_id DESC
                """;

        List<Payment> payments = new java.util.ArrayList<>();

        try (Connection connection =
                     com.shopsphere.dao.DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             java.sql.ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Payment payment = new Payment();

                payment.setPaymentId(rs.getInt("payment_id"));
                payment.setOrderId(rs.getInt("order_id"));
                payment.setUserId(rs.getInt("user_id"));
                payment.setGateway(rs.getString("gateway"));
                payment.setMethod(rs.getString("method"));
                payment.setTransactionId(rs.getString("transaction_id"));
                payment.setReferenceId(rs.getString("reference_id"));
                payment.setAmount(rs.getBigDecimal("amount"));
                payment.setCurrency(rs.getString("currency"));
                payment.setStatus(rs.getString("status"));
                payment.setFailureReason(rs.getString("failure_reason"));
                payment.setCardBrand(rs.getString("card_brand"));
                payment.setCardLast4(rs.getString("card_last4"));
                payment.setUpiId(rs.getString("upi_id"));
                payment.setBankName(rs.getString("bank_name"));

                payments.add(payment);
            }
        }

        return payments;
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
