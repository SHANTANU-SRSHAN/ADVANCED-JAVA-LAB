package com.shopsphere.dao;

import com.shopsphere.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the wishlist table.
 */
public class WishlistDAO {

    /** Products saved by a user (products joined in). */
    public List<Product> getWishlist(int userId) throws SQLException {

        String sql = """
                SELECT p.product_id, p.name, p.description, p.category,
                       p.price, p.stock, p.image_url
                FROM wishlist w
                JOIN products p ON p.product_id = w.product_id
                WHERE w.user_id = ?
                ORDER BY w.wishlist_id DESC
                """;

        List<Product> products = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    products.add(new Product(
                            rs.getInt("product_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("category"),
                            rs.getBigDecimal("price"),
                            rs.getInt("stock"),
                            rs.getString("image_url")
                    ));
                }
            }
        }

        return products;
    }

    /** How many products a customer has saved (badge in the header). */
    public int count(int userId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM wishlist WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean contains(int userId, int productId) throws SQLException {
        String sql = """
                SELECT 1 FROM wishlist
                WHERE user_id = ? AND product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void add(int userId, int productId) throws SQLException {

        String sql = """
                INSERT IGNORE INTO wishlist (user_id, product_id)
                VALUES (?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);
            statement.executeUpdate();
        }
    }

    public void remove(int userId, int productId) throws SQLException {

        String sql = """
                DELETE FROM wishlist
                WHERE user_id = ? AND product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);
            statement.executeUpdate();
        }
    }

    /**
     * Add when missing, remove when present.
     *
     * @return true when the product is now saved (in the wishlist)
     */
    public boolean toggle(int userId, int productId) throws SQLException {

        if (contains(userId, productId)) {
            remove(userId, productId);
            return false;
        }

        add(userId, productId);
        return true;
    }
}
