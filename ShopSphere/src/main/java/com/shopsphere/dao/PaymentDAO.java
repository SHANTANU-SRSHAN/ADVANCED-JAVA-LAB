package com.shopsphere.dao;

import com.shopsphere.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment records — one row per payment attempt.
 */
public class PaymentDAO {

    /**
     * Record a new payment attempt.
     *
     * @return the new payment id
     */
    public int insert(Payment payment) throws SQLException {

        String sql = """
                INSERT INTO payments
                    (order_id, user_id, gateway, method, transaction_id,
                     reference_id, amount, currency, status, failure_reason,
                     card_brand, card_last4, upi_id, bank_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql,
                             java.sql.Statement.RETURN_GENERATED_KEYS)) {

            if (payment.getOrderId() > 0) {
                statement.setInt(1, payment.getOrderId());
            } else {
                statement.setNull(1, java.sql.Types.INTEGER);
            }

            statement.setInt(2, payment.getUserId());
            statement.setString(3, payment.getGateway());
            statement.setString(4, payment.getMethod());
            statement.setString(5, payment.getTransactionId());
            statement.setString(6, payment.getReferenceId());
            statement.setBigDecimal(7, payment.getAmount());
            statement.setString(8, payment.getCurrency());
            statement.setString(9, payment.getStatus());
            statement.setString(10, payment.getFailureReason());
            statement.setString(11, payment.getCardBrand());
            statement.setString(12, payment.getCardLast4());
            statement.setString(13, payment.getUpiId());
            statement.setString(14, payment.getBankName());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        return 0;
    }

    /** Update the outcome of a payment. */
    public boolean updateStatus(int paymentId, String status,
                                String failureReason) throws SQLException {

        String sql = """
                UPDATE payments
                SET status = ?, failure_reason = ?
                WHERE payment_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setString(2, failureReason);
            statement.setInt(3, paymentId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Every payment attempt for one order, newest first. */
    public List<Payment> findByOrder(int orderId) throws SQLException {

        String sql = """
                SELECT payment_id, order_id, user_id, gateway, method,
                       transaction_id, reference_id, amount, currency,
                       status, failure_reason, card_brand, card_last4,
                       upi_id, bank_name, created_at, updated_at
                FROM payments
                WHERE order_id = ?
                ORDER BY payment_id DESC
                """;

        List<Payment> payments = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapPayment(rs));
                }
            }
        }

        return payments;
    }

    /** Every payment made by one customer, newest first. */
    public List<Payment> findByUser(int userId) throws SQLException {

        String sql = """
                SELECT payment_id, order_id, user_id, gateway, method,
                       transaction_id, reference_id, amount, currency,
                       status, failure_reason, card_brand, card_last4,
                       upi_id, bank_name, created_at, updated_at
                FROM payments
                WHERE user_id = ?
                ORDER BY payment_id DESC
                """;

        List<Payment> payments = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapPayment(rs));
                }
            }
        }

        return payments;
    }

    /** The successful payment for an order, if there is one. */
    public Payment findSuccessfulForOrder(int orderId) throws SQLException {

        String sql = """
                SELECT payment_id, order_id, user_id, gateway, method,
                       transaction_id, reference_id, amount, currency,
                       status, failure_reason, card_brand, card_last4,
                       upi_id, bank_name, created_at, updated_at
                FROM payments
                WHERE order_id = ? AND status IN ('SUCCESS', 'REFUNDED')
                ORDER BY payment_id DESC
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapPayment(rs) : null;
            }
        }
    }

    /** One payment by its id. */
    public Payment findById(int paymentId) throws SQLException {

        String sql = """
                SELECT payment_id, order_id, user_id, gateway, method,
                       transaction_id, reference_id, amount, currency,
                       status, failure_reason, card_brand, card_last4,
                       upi_id, bank_name, created_at, updated_at
                FROM payments
                WHERE payment_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, paymentId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapPayment(rs) : null;
            }
        }
    }

    /** Total amount collected successfully. */
    public BigDecimal totalCollected() throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0) FROM payments
                WHERE status = 'SUCCESS'
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    // ------------------------------------------------------------

    private Payment mapPayment(ResultSet rs) throws SQLException {

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

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            payment.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            payment.setUpdatedAt(updated.toLocalDateTime());
        }

        return payment;
    }
}
