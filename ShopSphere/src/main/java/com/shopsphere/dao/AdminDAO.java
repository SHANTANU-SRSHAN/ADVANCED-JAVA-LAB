package com.shopsphere.dao;

import com.shopsphere.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated numbers for the admin dashboard.
 */
public class AdminDAO {

    /** Label → value pairs, ready to display on the dashboard. */
    public Map<String, Object> getDashboardStats() throws SQLException {

        Map<String, Object> stats = new LinkedHashMap<>();

        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM users
                     WHERE role = 'CUSTOMER')              AS customers,
                  (SELECT COUNT(*) FROM products)          AS products,
                  (SELECT COUNT(*) FROM orders)            AS orders,
                  (SELECT COUNT(*) FROM orders
                     WHERE order_status = 'DELIVERED')     AS delivered,
                  (SELECT COUNT(*) FROM orders
                     WHERE order_status = 'CANCELLED')     AS cancelled,
                  (SELECT COALESCE(SUM(total_amount), 0) FROM orders
                     WHERE payment_status = 'PAID')        AS revenue,
                  (SELECT COALESCE(SUM(stock), 0)
                     FROM products)                        AS stock,
                  (SELECT COUNT(*) FROM orders
                     WHERE DATE(created_at) = CURDATE())   AS today_orders,
                  (SELECT COALESCE(SUM(total_amount), 0) FROM orders
                     WHERE DATE(created_at) = CURDATE()
                       AND payment_status = 'PAID')        AS today_revenue
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                stats.put("customers", rs.getInt("customers"));
                stats.put("products", rs.getInt("products"));
                stats.put("orders", rs.getInt("orders"));
                stats.put("delivered", rs.getInt("delivered"));
                stats.put("cancelled", rs.getInt("cancelled"));
                stats.put("revenue", rs.getBigDecimal("revenue"));
                stats.put("stock", rs.getInt("stock"));
                stats.put("todayOrders", rs.getInt("today_orders"));
                stats.put("todayRevenue", rs.getBigDecimal("today_revenue"));
            }
        }

        return stats;
    }

    /** Order count + revenue grouped by status (small chart data). */
    public Map<String, Integer> getOrdersByStatus() throws SQLException {

        String sql = """
                SELECT order_status, COUNT(*) AS n
                FROM orders
                GROUP BY order_status
                """;

        Map<String, Integer> counts = new LinkedHashMap<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                counts.put(rs.getString("order_status"), rs.getInt("n"));
            }
        }

        return counts;
    }

    /** Products that are running out of stock. */
    public List<Product> getLowStockProducts(int threshold)
            throws SQLException {

        String sql = """
                SELECT product_id, name, description, category,
                       price, stock, image_url
                FROM products
                WHERE stock <= ?
                ORDER BY stock ASC, name ASC
                """;

        List<Product> products = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, threshold);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(new Product(
                            resultSet.getInt("product_id"),
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getString("category"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getInt("stock"),
                            resultSet.getString("image_url")
                    ));
                }
            }
        }

        return products;
    }

    /**
     * Revenue for each of the last {@code days} days, oldest first.
     *
     * <p>Used for the small sales chart on the dashboard.
     */
    public Map<String, BigDecimal> getDailyRevenue(int days)
            throws SQLException {

        String sql = """
                SELECT DATE(created_at) AS day,
                       COALESCE(SUM(total_amount), 0) AS revenue
                FROM orders
                WHERE payment_status = 'PAID'
                  AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                GROUP BY DATE(created_at)
                ORDER BY day
                """;

        Map<String, BigDecimal> revenue = new LinkedHashMap<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, days);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    revenue.put(rs.getString("day"),
                            rs.getBigDecimal("revenue"));
                }
            }
        }

        return revenue;
    }

    /** How much is in the catalogue at today's prices. */
    public BigDecimal getInventoryValue() throws SQLException {

        String sql = "SELECT COALESCE(SUM(price * stock), 0) FROM products";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    /** The five best selling products. */
    public List<String[]> getTopProducts(int limit) throws SQLException {

        String sql = """
                SELECT p.name, SUM(oi.quantity) AS units,
                       SUM(oi.line_total) AS revenue
                FROM order_items oi
                JOIN products p ON p.product_id = oi.product_id
                JOIN orders   o ON o.order_id   = oi.order_id
                WHERE o.order_status <> 'CANCELLED'
                GROUP BY p.product_id, p.name
                ORDER BY units DESC
                LIMIT ?
                """;

        List<String[]> rows = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                            rs.getString("name"),
                            String.valueOf(rs.getInt("units")),
                            String.valueOf(rs.getBigDecimal("revenue"))});
                }
            }
        }

        return rows;
    }
}
