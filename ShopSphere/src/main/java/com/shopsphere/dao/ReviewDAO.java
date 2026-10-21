package com.shopsphere.dao;

import com.shopsphere.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer reviews and ratings.
 */
public class ReviewDAO {

    /** Every review of one product, newest first. */
    public List<Review> findByProduct(int productId) throws SQLException {

        String sql = """
                SELECT r.review_id, r.product_id, r.user_id, r.rating,
                       r.title, r.comment, r.is_verified_buy, r.created_at,
                       u.full_name AS customer_name, p.name AS product_name
                FROM reviews r
                JOIN users u    ON u.user_id = r.user_id
                JOIN products p ON p.product_id = r.product_id
                WHERE r.product_id = ?
                ORDER BY r.created_at DESC
                """;

        List<Review> reviews = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReview(rs));
                }
            }
        }

        return reviews;
    }

    /** Every review written by one customer. */
    public List<Review> findByUser(int userId) throws SQLException {

        String sql = """
                SELECT r.review_id, r.product_id, r.user_id, r.rating,
                       r.title, r.comment, r.is_verified_buy, r.created_at,
                       u.full_name AS customer_name, p.name AS product_name
                FROM reviews r
                JOIN users u    ON u.user_id = r.user_id
                JOIN products p ON p.product_id = r.product_id
                WHERE r.user_id = ?
                ORDER BY r.created_at DESC
                """;

        List<Review> reviews = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReview(rs));
                }
            }
        }

        return reviews;
    }

    /** The review a customer already left for a product, if any. */
    public Review findByUserAndProduct(int userId, int productId)
            throws SQLException {

        String sql = """
                SELECT r.review_id, r.product_id, r.user_id, r.rating,
                       r.title, r.comment, r.is_verified_buy, r.created_at,
                       u.full_name AS customer_name, p.name AS product_name
                FROM reviews r
                JOIN users u    ON u.user_id = r.user_id
                JOIN products p ON p.product_id = r.product_id
                WHERE r.user_id = ? AND r.product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapReview(rs) : null;
            }
        }
    }

    /**
     * Save (or replace) a review.
     *
     * @return true when it was stored
     */
    public boolean save(Review review) throws SQLException {

        String sql = """
                INSERT INTO reviews
                    (product_id, user_id, rating, title, comment,
                     is_verified_buy)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    rating = VALUES(rating),
                    title = VALUES(title),
                    comment = VALUES(comment),
                    is_verified_buy = VALUES(is_verified_buy),
                    created_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, review.getProductId());
            statement.setInt(2, review.getUserId());
            statement.setInt(3, review.getRating());
            statement.setString(4, review.getTitle());
            statement.setString(5, review.getComment());
            statement.setInt(6, review.isVerifiedPurchase() ? 1 : 0);

            return statement.executeUpdate() > 0;
        }
    }

    /** Delete one of a customer's own reviews. */
    public boolean delete(int reviewId, int userId) throws SQLException {

        String sql = """
                DELETE FROM reviews WHERE review_id = ? AND user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, reviewId);
            statement.setInt(2, userId);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * The average rating and the number of reviews for a product.
     *
     * @return an array of { averageRating, reviewCount }
     */
    public double[] averageRating(int productId) throws SQLException {

        String sql = """
                SELECT COALESCE(AVG(rating), 0) AS avg_rating,
                       COUNT(*) AS review_count
                FROM reviews
                WHERE product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new double[]{
                            rs.getDouble("avg_rating"),
                            rs.getDouble("review_count")};
                }
            }
        }

        return new double[]{0, 0};
    }

    /**
     * The number of stars given for each rating (1..5).
     *
     * @return an array of 5 counts, index 0 = one star
     */
    public int[] ratingBreakdown(int productId) throws SQLException {

        int[] counts = new int[5];

        String sql = """
                SELECT rating, COUNT(*) AS n
                FROM reviews
                WHERE product_id = ?
                GROUP BY rating
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int rating = rs.getInt("rating");
                    if (rating >= 1 && rating <= 5) {
                        counts[rating - 1] = rs.getInt("n");
                    }
                }
            }
        }

        return counts;
    }

    /**
     * True when the customer actually bought this product.
     */
    public boolean hasPurchased(int userId, int productId)
            throws SQLException {

        String sql = """
                SELECT 1
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.order_id
                WHERE o.user_id = ? AND oi.product_id = ?
                  AND o.order_status <> 'CANCELLED'
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ------------------------------------------------------------

    private Review mapReview(ResultSet rs) throws SQLException {

        Review review = new Review();

        review.setReviewId(rs.getInt("review_id"));
        review.setProductId(rs.getInt("product_id"));
        review.setUserId(rs.getInt("user_id"));
        review.setRating(rs.getInt("rating"));
        review.setTitle(rs.getString("title"));
        review.setComment(rs.getString("comment"));
        review.setVerifiedPurchase(rs.getBoolean("is_verified_buy"));
        review.setCustomerName(rs.getString("customer_name"));

        String productName = rs.getString("product_name");
        if (productName != null) {
            review.setProductName(productName);
        }

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            review.setCreatedAt(created.toLocalDateTime());
        }

        return review;
    }
}
