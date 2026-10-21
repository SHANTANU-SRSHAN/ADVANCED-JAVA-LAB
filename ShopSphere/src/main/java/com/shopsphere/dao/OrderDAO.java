package com.shopsphere.dao;

import com.shopsphere.model.CartItem;
import com.shopsphere.model.Coupon;
import com.shopsphere.model.Order;
import com.shopsphere.model.OrderDraft;
import com.shopsphere.model.OrderEvent;
import com.shopsphere.model.OrderItem;
import com.shopsphere.model.OrderTotals;
import com.shopsphere.model.ShippingMethod;
import com.shopsphere.service.PricingService;
import com.shopsphere.util.TokenUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for orders, order items and the tracking timeline.
 *
 * <p>{@link #placeOrder(OrderDraft)} runs in a single transaction:
 * insert order → insert items → reduce stock → clear cart. If any
 * product does not have enough stock the whole order is rolled back.
 */
public class OrderDAO {

    /** Returned by placeOrder() when a product is out of stock. */
    public static final int STOCK_FAILURE = -1;

    /** Returned by placeOrder() when the draft was not usable. */
    public static final int INVALID_ORDER = -2;

    private static final List<String> ALLOWED_STATUSES = List.of(
            "PLACED", "CONFIRMED", "PACKED", "SHIPPED", "DELIVERED",
            "CANCELLED", "REFUNDED");

    // ------------------------------------------------------------
    // Creating an order
    // ------------------------------------------------------------

    /**
     * Create an order from the customer's cart.
     *
     * @param draft everything about the order
     * @return the new order id, {@link #STOCK_FAILURE} when stock ran
     *         out (rolled back) or {@link #INVALID_ORDER} when the
     *         draft was not usable
     */
    public int placeOrder(OrderDraft draft) throws SQLException {

        if (draft == null || draft.getItems() == null
                || draft.getItems().isEmpty()) {
            return INVALID_ORDER;
        }

        OrderTotals totals = PricingService.totals(
                draft.getItems(),
                draft.getShippingMethod(),
                draft.getCoupon());

        ShippingMethod shipping = draft.getShippingMethod();
        Coupon coupon = draft.getCoupon();

        String orderStatus = draft.isPaid() ? "CONFIRMED" : "PLACED";
        String paymentStatus = draft.isPaid() ? "PAID" : "PENDING";

        Connection connection = DBConnection.getConnection();

        try {
            connection.setAutoCommit(false);

            // ---- 1. the order header ----
            int orderId;
            String insertOrder = """
                    INSERT INTO orders
                        (order_number, user_id,
                         subtotal_amount, discount_amount, shipping_amount,
                         tax_amount, total_amount, order_status,
                         coupon_code, shipping_method_code, shipping_method_name,
                         full_name, phone, address_line1, address_line2,
                         city, state, pincode, shipping_address, notes,
                         estimated_delivery,
                         payment_method, payment_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(insertOrder,
                                 Statement.RETURN_GENERATED_KEYS)) {

                int index = 1;

                statement.setString(index++, TokenUtil.newOrderNumber());
                statement.setInt(index++, draft.getUserId());

                statement.setBigDecimal(index++, totals.getSubtotal());
                statement.setBigDecimal(index++, totals.getDiscount());
                statement.setBigDecimal(index++, totals.getShipping());
                statement.setBigDecimal(index++, totals.getTax());
                statement.setBigDecimal(index++, totals.getTotal());

                statement.setString(index++, orderStatus);

                if (coupon != null) {
                    statement.setString(index++, coupon.getCode());
                } else {
                    statement.setNull(index++, java.sql.Types.VARCHAR);
                }

                if (shipping != null) {
                    statement.setString(index++, shipping.getCode());
                    statement.setString(index++, shipping.getName());
                } else {
                    statement.setNull(index++, java.sql.Types.VARCHAR);
                    statement.setNull(index++, java.sql.Types.VARCHAR);
                }

                statement.setString(index++, draft.getFullName());
                statement.setString(index++, draft.getPhone());
                statement.setString(index++, draft.getAddressLine1());
                statement.setString(index++, draft.getAddressLine2());
                statement.setString(index++, draft.getCity());
                statement.setString(index++, draft.getState());
                statement.setString(index++, draft.getPincode());
                statement.setString(index++, draft.toSingleLine());

                if (draft.getNotes() == null || draft.getNotes().isBlank()) {
                    statement.setNull(index++, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(index++, draft.getNotes());
                }

                LocalDate estimate = null;
                if (shipping != null) {
                    estimate = LocalDate.now().plusDays(shipping.getMaxDays());
                }
                if (estimate == null) {
                    statement.setNull(index++, java.sql.Types.DATE);
                } else {
                    statement.setDate(index++, java.sql.Date.valueOf(estimate));
                }

                statement.setString(index++, draft.getPaymentMethod());
                statement.setString(index++, paymentStatus);

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Order was not created.");
                    }
                    orderId = keys.getInt(1);
                }
            }

            // ---- 2. the line items + stock ----
            String insertItem = """
                    INSERT INTO order_items
                        (order_id, product_id, product_name, image_url,
                         quantity, unit_price, line_total)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            String updateStock = """
                    UPDATE products
                    SET stock = stock - ?
                    WHERE product_id = ? AND stock >= ?
                    """;

            try (PreparedStatement itemStatement =
                         connection.prepareStatement(insertItem);
                 PreparedStatement stockStatement =
                         connection.prepareStatement(updateStock)) {

                for (CartItem item : draft.getItems()) {

                    BigDecimal lineTotal = item.getLineTotal();

                    itemStatement.setInt(1, orderId);
                    itemStatement.setInt(2, item.getProductId());
                    itemStatement.setString(3,
                            item.getProduct().getName());
                    itemStatement.setString(4,
                            item.getProduct().getImageUrl());
                    itemStatement.setInt(5, item.getQuantity());
                    itemStatement.setBigDecimal(6,
                            item.getProduct().getPrice());
                    itemStatement.setBigDecimal(7, lineTotal);
                    itemStatement.addBatch();

                    stockStatement.setInt(1, item.getQuantity());
                    stockStatement.setInt(2, item.getProductId());
                    stockStatement.setInt(3, item.getQuantity());

                    if (stockStatement.executeUpdate() == 0) {
                        connection.rollback();      // not enough stock
                        return STOCK_FAILURE;
                    }
                }

                itemStatement.executeBatch();
            }

            // ---- 3. empty the cart ----
            try (PreparedStatement clear =
                         connection.prepareStatement(
                                 "DELETE FROM cart_items WHERE user_id = ?")) {

                clear.setInt(1, draft.getUserId());
                clear.executeUpdate();
            }

            // ---- 4. the first timeline entry ----
            insertHistory(connection, orderId, orderStatus,
                    draft.isPaid()
                            ? "Payment received. The order is confirmed."
                            : "Order placed. We will confirm it shortly.",
                    "customer");

            connection.commit();
            return orderId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;

        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    /**
     * Keep the old signature working for code that has not been
     * migrated yet.
     */
    public int placeOrder(int userId, List<CartItem> items,
                          String shippingAddress, String phone,
                          String paymentMethod, boolean paid)
            throws SQLException {

        OrderDraft draft = new OrderDraft();
        draft.setUserId(userId);
        draft.setItems(items);
        draft.setFullName("");
        draft.setPhone(phone);
        draft.setAddressLine1(shippingAddress);
        draft.setCity("");
        draft.setPincode("");
        draft.setPaymentMethod(paymentMethod);
        draft.setPaid(paid);

        return placeOrder(draft);
    }

    // ------------------------------------------------------------
    // Reading orders
    // ------------------------------------------------------------

    /** Orders placed by one customer, newest first. */
    public List<Order> getOrdersByUser(int userId) throws SQLException {

        String sql = """
                SELECT o.order_id, o.order_number, o.user_id,
                       o.subtotal_amount, o.discount_amount, o.shipping_amount,
                       o.tax_amount, o.total_amount, o.order_status,
                       o.coupon_code, o.shipping_method_code,
                       o.shipping_method_name,
                       o.full_name, o.phone, o.address_line1, o.address_line2,
                       o.city, o.state, o.pincode, o.shipping_address, o.notes,
                       o.tracking_number, o.estimated_delivery,
                       o.payment_method, o.payment_status,
                       o.created_at, o.updated_at,
                       u.full_name AS customer_name, u.email AS customer_email
                FROM orders o
                JOIN users u ON u.user_id = o.user_id
                WHERE o.user_id = ?
                ORDER BY o.order_id DESC
                """;

        List<Order> orders = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        }

        // Load the line items so the list can show "3 items".
        for (Order order : orders) {
            order.setItems(getItems(order.getOrderId()));
        }

        return orders;
    }

    /** Every order, newest first, optionally filtered by status (admin). */
    public List<Order> getAllOrders(String status) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT o.order_id, o.order_number, o.user_id,
                       o.subtotal_amount, o.discount_amount, o.shipping_amount,
                       o.tax_amount, o.total_amount, o.order_status,
                       o.coupon_code, o.shipping_method_code,
                       o.shipping_method_name,
                       o.full_name, o.phone, o.address_line1, o.address_line2,
                       o.city, o.state, o.pincode, o.shipping_address, o.notes,
                       o.tracking_number, o.estimated_delivery,
                       o.payment_method, o.payment_status,
                       o.created_at, o.updated_at,
                       u.full_name AS customer_name, u.email AS customer_email
                FROM orders o
                JOIN users u ON u.user_id = o.user_id
                WHERE 1 = 1
                """);

        boolean filter = status != null && !status.isBlank()
                && !"ALL".equals(status);

        if (filter) {
            sql.append(" AND o.order_status = ? ");
        }

        sql.append(" ORDER BY o.order_id DESC");

        List<Order> orders = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            if (filter) {
                statement.setString(1, status);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        }

        return orders;
    }

    /**
     * Load one order with its items, timeline and payments.
     *
     * @param userId ownership check — pass the customer id, or null when
     *               an admin is viewing any order.
     * @return the order, or null when not found / not owned by the user
     */
    public Order getOrder(int orderId, Integer userId) throws SQLException {

        String sql = """
                SELECT o.order_id, o.order_number, o.user_id,
                       o.subtotal_amount, o.discount_amount, o.shipping_amount,
                       o.tax_amount, o.total_amount, o.order_status,
                       o.coupon_code, o.shipping_method_code,
                       o.shipping_method_name,
                       o.full_name, o.phone, o.address_line1, o.address_line2,
                       o.city, o.state, o.pincode, o.shipping_address, o.notes,
                       o.tracking_number, o.estimated_delivery,
                       o.payment_method, o.payment_status,
                       o.created_at, o.updated_at,
                       u.full_name AS customer_name, u.email AS customer_email
                FROM orders o
                JOIN users u ON u.user_id = o.user_id
                WHERE o.order_id = ?
                """;

        Order order = null;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    order = mapOrder(rs);
                }
            }
        }

        if (order == null) {
            return null;
        }

        if (userId != null && order.getUserId() != userId) {
            return null;      // somebody else's order
        }

        order.setItems(getItems(orderId));
        order.setEvents(getHistory(orderId));

        return order;
    }

    /** Line items of one order. */
    public List<OrderItem> getItems(int orderId) throws SQLException {

        String sql = """
                SELECT oi.order_item_id, oi.order_id, oi.product_id,
                       oi.product_name, oi.image_url,
                       oi.quantity, oi.unit_price, oi.line_total
                FROM order_items oi
                WHERE oi.order_id = ?
                ORDER BY oi.order_item_id
                """;

        List<OrderItem> items = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();

                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setImageUrl(rs.getString("image_url"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));

                    items.add(item);
                }
            }
        }

        return items;
    }

    /** The tracking timeline of one order. */
    public List<OrderEvent> getHistory(int orderId) throws SQLException {

        String sql = """
                SELECT history_id, order_id, status, note, changed_by,
                       created_at
                FROM order_status_history
                WHERE order_id = ?
                ORDER BY history_id
                """;

        List<OrderEvent> events = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OrderEvent event = new OrderEvent();

                    event.setHistoryId(rs.getInt("history_id"));
                    event.setOrderId(rs.getInt("order_id"));
                    event.setStatus(rs.getString("status"));
                    event.setNote(rs.getString("note"));
                    event.setChangedBy(rs.getString("changed_by"));

                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) {
                        event.setCreatedAt(created.toLocalDateTime());
                    }

                    events.add(event);
                }
            }
        }

        return events;
    }

    // ------------------------------------------------------------
    // Updating an order
    // ------------------------------------------------------------

    /**
     * Update the fulfilment status (admin).
     *
     * <p>The change is written to the tracking timeline as well.
     */
    public boolean updateStatus(int orderId, String status, String note,
                                String changedBy) throws SQLException {

        if (!ALLOWED_STATUSES.contains(status)) {
            return false;
        }

        String sql = "UPDATE orders SET order_status = ? WHERE order_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            try {
                statement.setString(1, status);
                statement.setInt(2, orderId);

                boolean updated = statement.executeUpdate() == 1;

                if (updated) {
                    insertHistory(connection, orderId, status, note, changedBy);
                }

                connection.commit();
                return updated;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** Keep the old signature working. */
    public boolean updateStatus(int orderId, String status)
            throws SQLException {
        return updateStatus(orderId, status, null, "admin");
    }

    /**
     * Cancel an order that the customer can still cancel.
     *
     * <p>The stock that was taken when the order was placed is put back
     * into the catalogue, and the coupon (if any) is released.
     */
    public boolean cancelOrder(int orderId, int userId) throws SQLException {

        Connection connection = DBConnection.getConnection();

        try {
            connection.setAutoCommit(false);

            // Only orders that are still PLACED or CONFIRMED may be
            // cancelled, and only by the customer who placed them.
            String sql = """
                    UPDATE orders
                    SET order_status = 'CANCELLED',
                        payment_status = CASE
                            WHEN payment_status = 'PAID' THEN 'REFUNDED'
                            ELSE payment_status END
                    WHERE order_id = ? AND user_id = ?
                      AND order_status IN ('PLACED', 'CONFIRMED')
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, orderId);
                statement.setInt(2, userId);

                if (statement.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
            }

            // Give the stock back.
            try (PreparedStatement restore = connection.prepareStatement("""
                    UPDATE products p
                    JOIN order_items oi ON oi.product_id = p.product_id
                    SET p.stock = p.stock + oi.quantity
                    WHERE oi.order_id = ?
                    """)) {

                restore.setInt(1, orderId);
                restore.executeUpdate();
            }

            // Release the coupon.
            try (PreparedStatement release = connection.prepareStatement("""
                    UPDATE coupons c
                    JOIN coupon_redemptions cr ON cr.coupon_id = c.coupon_id
                    SET c.used_count = GREATEST(c.used_count - 1, 0)
                    WHERE cr.order_id = ?
                    """)) {

                release.setInt(1, orderId);
                release.executeUpdate();
            }

            insertHistory(connection, orderId, "CANCELLED",
                    "The order was cancelled by the customer.", "customer");

            connection.commit();
            return true;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    /** Attach a tracking number to an order. */
    public boolean setTrackingNumber(int orderId, String trackingNumber)
            throws SQLException {

        String sql = "UPDATE orders SET tracking_number = ? WHERE order_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, trackingNumber);
            statement.setInt(2, orderId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Mark the order as refunded (after a successful refund). */
    public boolean markRefunded(int orderId) throws SQLException {

        String sql = """
                UPDATE orders
                SET payment_status = 'REFUNDED',
                    order_status = 'REFUNDED'
                WHERE order_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);

            boolean updated = statement.executeUpdate() == 1;

            if (updated) {
                insertHistory(connection, orderId, "REFUNDED",
                        "The payment was refunded.", "admin");
            }

            return updated;
        }
    }

    // ------------------------------------------------------------
    // Timeline helper
    // ------------------------------------------------------------

    private void insertHistory(Connection connection, int orderId,
                               String status, String note, String changedBy)
            throws SQLException {

        String sql = """
                INSERT INTO order_status_history
                    (order_id, status, note, changed_by)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);
            statement.setString(2, status);
            statement.setString(3, note);
            statement.setString(4, changedBy);

            statement.executeUpdate();
        }
    }

    // ------------------------------------------------------------

    private Order mapOrder(ResultSet rs) throws SQLException {

        Order order = new Order();

        order.setOrderId(rs.getInt("order_id"));
        order.setOrderNumber(rs.getString("order_number"));
        order.setUserId(rs.getInt("user_id"));

        order.setSubtotalAmount(rs.getBigDecimal("subtotal_amount"));
        order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        order.setShippingAmount(rs.getBigDecimal("shipping_amount"));
        order.setTaxAmount(rs.getBigDecimal("tax_amount"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));

        order.setOrderStatus(rs.getString("order_status"));
        order.setCouponCode(rs.getString("coupon_code"));
        order.setShippingMethodCode(rs.getString("shipping_method_code"));
        order.setShippingMethodName(rs.getString("shipping_method_name"));

        order.setFullName(rs.getString("full_name"));
        order.setPhone(rs.getString("phone"));
        order.setAddressLine1(rs.getString("address_line1"));
        order.setAddressLine2(rs.getString("address_line2"));
        order.setCity(rs.getString("city"));
        order.setState(rs.getString("state"));
        order.setPincode(rs.getString("pincode"));
        order.setShippingAddress(rs.getString("shipping_address"));
        order.setNotes(rs.getString("notes"));
        order.setTrackingNumber(rs.getString("tracking_number"));

        java.sql.Date estimate = rs.getDate("estimated_delivery");
        if (estimate != null) {
            order.setEstimatedDelivery(estimate.toLocalDate());
        }

        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaymentStatus(rs.getString("payment_status"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            order.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            order.setUpdatedAt(updated.toLocalDateTime());
        }

        order.setCustomerName(rs.getString("customer_name"));
        order.setCustomerEmail(rs.getString("customer_email"));

        return order;
    }
}
