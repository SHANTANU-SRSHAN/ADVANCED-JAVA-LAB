package com.shopsphere.dao;

import com.shopsphere.model.ShippingMethod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The delivery options offered at checkout.
 */
public class ShippingMethodDAO {

    /** Every active delivery option, cheapest first. */
    public List<ShippingMethod> findActive() throws SQLException {

        String sql = """
                SELECT method_id, code, name, description, price,
                       min_order_amount, max_days, is_active, sort_order
                FROM shipping_methods
                WHERE is_active = 1
                ORDER BY sort_order, price
                """;

        List<ShippingMethod> methods = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                methods.add(mapMethod(rs));
            }
        }

        return methods;
    }

    /** Every delivery option, including the inactive ones (admin). */
    public List<ShippingMethod> findAll() throws SQLException {

        String sql = """
                SELECT method_id, code, name, description, price,
                       min_order_amount, max_days, is_active, sort_order
                FROM shipping_methods
                ORDER BY sort_order, price
                """;

        List<ShippingMethod> methods = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                methods.add(mapMethod(rs));
            }
        }

        return methods;
    }

    /** One delivery option by its code. */
    public ShippingMethod findByCode(String code) throws SQLException {

        if (code == null || code.isBlank()) {
            return null;
        }

        String sql = """
                SELECT method_id, code, name, description, price,
                       min_order_amount, max_days, is_active, sort_order
                FROM shipping_methods
                WHERE code = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, code.trim().toUpperCase());

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapMethod(rs) : null;
            }
        }
    }

    /** Switch a delivery option on or off. */
    public boolean setActive(int methodId, boolean active)
            throws SQLException {

        String sql = "UPDATE shipping_methods SET is_active = ? "
                + "WHERE method_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, methodId);

            return statement.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------

    private ShippingMethod mapMethod(ResultSet rs) throws SQLException {

        ShippingMethod method = new ShippingMethod();

        method.setMethodId(rs.getInt("method_id"));
        method.setCode(rs.getString("code"));
        method.setName(rs.getString("name"));
        method.setDescription(rs.getString("description"));
        method.setPrice(rs.getBigDecimal("price"));
        method.setMinOrderAmount(rs.getBigDecimal("min_order_amount"));
        method.setMaxDays(rs.getInt("max_days"));
        method.setActive(rs.getBoolean("is_active"));
        method.setSortOrder(rs.getInt("sort_order"));

        return method;
    }
}
