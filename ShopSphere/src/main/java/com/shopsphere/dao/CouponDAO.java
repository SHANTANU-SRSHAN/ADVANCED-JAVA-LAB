package com.shopsphere.dao;

import com.shopsphere.model.Coupon;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Discount coupons.
 */
public class CouponDAO {

    /** Why a coupon could not be used — null when it is fine. */
    public record Validation(Coupon coupon, String error) {

        public boolean isValid() {
            return error == null && coupon != null;
        }
    }

    /** Find a coupon by its code (case-insensitive). */
    public Coupon findByCode(String code) throws SQLException {

        if (code == null || code.isBlank()) {
            return null;
        }

        String sql = """
                SELECT coupon_id, code, description, discount_type,
                       discount_value, min_order_amount, max_discount,
                       usage_limit, used_count, per_user_limit,
                       valid_from, valid_to, is_active
                FROM coupons
                WHERE UPPER(code) = UPPER(?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, code.trim());

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapCoupon(rs) : null;
            }
        }
    }

    /**
     * Check whether a customer may use a coupon on an order of the
     * given value.
     */
    public Validation validate(String code, int userId,
                               BigDecimal orderAmount) throws SQLException {

        Coupon coupon = findByCode(code);

        if (coupon == null) {
            return new Validation(null,
                    "That coupon code was not found.");
        }

        if (!coupon.isActive()) {
            return new Validation(coupon,
                    "That coupon is no longer active.");
        }

        if (!coupon.isCurrentlyValid()) {
            return new Validation(coupon,
                    "That coupon has expired or has been used up.");
        }

        if (orderAmount == null
                || orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return new Validation(coupon,
                    "This coupon needs a minimum order of "
                            + coupon.getMinOrderAmount() + ".");
        }

        if (coupon.getUsageLimit() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return new Validation(coupon,
                    "This coupon has reached its usage limit.");
        }

        // --- per-customer limit ---
        int timesUsed = countRedemptions(coupon.getCouponId(), userId);

        if (timesUsed >= coupon.getPerUserLimit()) {
            return new Validation(coupon,
                    "You have already used this coupon "
                            + timesUsed + " time(s).");
        }

        if (coupon.discountFor(orderAmount).signum() <= 0) {
            return new Validation(coupon,
                    "This coupon gives no discount on this order.");
        }

        return new Validation(coupon, null);
    }

    /** Record that a customer used a coupon on an order. */
    public void recordRedemption(int couponId, int userId, int orderId,
                                 BigDecimal amount) throws SQLException {

        String insert = """
                INSERT INTO coupon_redemptions
                    (coupon_id, user_id, order_id, amount)
                VALUES (?, ?, ?, ?)
                """;

        String update = """
                UPDATE coupons SET used_count = used_count + 1
                WHERE coupon_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement insertStatement =
                     connection.prepareStatement(insert);
             PreparedStatement updateStatement =
                     connection.prepareStatement(update)) {

            connection.setAutoCommit(false);

            try {
                insertStatement.setInt(1, couponId);
                insertStatement.setInt(2, userId);
                if (orderId > 0) {
                    insertStatement.setInt(3, orderId);
                } else {
                    insertStatement.setNull(3, java.sql.Types.INTEGER);
                }
                insertStatement.setBigDecimal(4, amount);
                insertStatement.executeUpdate();

                updateStatement.setInt(1, couponId);
                updateStatement.executeUpdate();

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** How many times a customer has already used a coupon. */
    public int countRedemptions(int couponId, int userId) throws SQLException {

        String sql = """
                SELECT COUNT(*) FROM coupon_redemptions
                WHERE coupon_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, couponId);
            statement.setInt(2, userId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Every coupon (admin list). */
    public List<Coupon> findAll() throws SQLException {

        String sql = """
                SELECT coupon_id, code, description, discount_type,
                       discount_value, min_order_amount, max_discount,
                       usage_limit, used_count, per_user_limit,
                       valid_from, valid_to, is_active
                FROM coupons
                ORDER BY is_active DESC, coupon_id DESC
                """;

        List<Coupon> coupons = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                coupons.add(mapCoupon(rs));
            }
        }

        return coupons;
    }

    /** Coupons that a customer could use right now (for hints). */
    public List<Coupon> findAvailable(int userId, BigDecimal orderAmount)
            throws SQLException {

        List<Coupon> available = new ArrayList<>();

        for (Coupon coupon : findAll()) {
            if (!coupon.isCurrentlyValid()) {
                continue;
            }
            if (countRedemptions(coupon.getCouponId(), userId)
                    >= coupon.getPerUserLimit()) {
                continue;
            }
            if (orderAmount != null
                    && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                continue;
            }
            available.add(coupon);
        }

        return available;
    }

    /** Create a coupon (admin). */
    public boolean insert(Coupon coupon) throws SQLException {

        String sql = """
                INSERT INTO coupons
                    (code, description, discount_type, discount_value,
                     min_order_amount, max_discount, usage_limit,
                     per_user_limit, valid_to, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, coupon.getCode().toUpperCase());
            statement.setString(2, coupon.getDescription());
            statement.setString(3, coupon.getDiscountType());
            statement.setBigDecimal(4, coupon.getDiscountValue());
            statement.setBigDecimal(5, coupon.getMinOrderAmount());

            if (coupon.getMaxDiscount() != null) {
                statement.setBigDecimal(6, coupon.getMaxDiscount());
            } else {
                statement.setNull(6, java.sql.Types.DECIMAL);
            }

            if (coupon.getUsageLimit() != null) {
                statement.setInt(7, coupon.getUsageLimit());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }

            statement.setInt(8, coupon.getPerUserLimit());

            if (coupon.getValidTo() != null) {
                statement.setTimestamp(9,
                        Timestamp.valueOf(coupon.getValidTo()));
            } else {
                statement.setNull(9, java.sql.Types.TIMESTAMP);
            }

            statement.setInt(10, coupon.isActive() ? 1 : 0);

            return statement.executeUpdate() == 1;
        }
    }

    /** Switch a coupon on or off. */
    public boolean setActive(int couponId, boolean active)
            throws SQLException {

        String sql = "UPDATE coupons SET is_active = ? WHERE coupon_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, active ? 1 : 0);
            statement.setInt(2, couponId);

            return statement.executeUpdate() == 1;
        }
    }

    /** Delete a coupon that was never used. */
    public boolean delete(int couponId) throws SQLException {

        String sql = """
                DELETE FROM coupons
                WHERE coupon_id = ?
                  AND coupon_id NOT IN (
                      SELECT DISTINCT coupon_id FROM coupon_redemptions
                  )
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, couponId);
            return statement.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------

    private Coupon mapCoupon(ResultSet rs) throws SQLException {

        Coupon coupon = new Coupon();

        coupon.setCouponId(rs.getInt("coupon_id"));
        coupon.setCode(rs.getString("code"));
        coupon.setDescription(rs.getString("description"));
        coupon.setDiscountType(rs.getString("discount_type"));
        coupon.setDiscountValue(rs.getBigDecimal("discount_value"));
        coupon.setMinOrderAmount(rs.getBigDecimal("min_order_amount"));

        BigDecimal maxDiscount = rs.getBigDecimal("max_discount");
        if (maxDiscount != null) {
            coupon.setMaxDiscount(maxDiscount);
        }

        int usageLimit = rs.getInt("usage_limit");
        if (!rs.wasNull()) {
            coupon.setUsageLimit(usageLimit);
        }

        coupon.setUsedCount(rs.getInt("used_count"));
        coupon.setPerUserLimit(rs.getInt("per_user_limit"));

        Timestamp validFrom = rs.getTimestamp("valid_from");
        if (validFrom != null) {
            coupon.setValidFrom(validFrom.toLocalDateTime());
        }

        Timestamp validTo = rs.getTimestamp("valid_to");
        if (validTo != null) {
            coupon.setValidTo(validTo.toLocalDateTime());
        }

        coupon.setActive(rs.getBoolean("is_active"));

        return coupon;
    }
}
