package com.shopsphere.dao;

import com.shopsphere.model.CartItem;
import com.shopsphere.model.Product;
import com.shopsphere.service.PricingService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the shopping cart (cart_items table).
 * All methods operate on behalf of one logged-in user.
 */
public class CartDAO {

    /** Full cart with product details joined in. */
    public List<CartItem> getItems(int userId) throws SQLException {

        String sql = """
                SELECT ci.cart_item_id, ci.user_id, ci.product_id, ci.quantity,
                       p.product_id   AS p_id,
                       p.name, p.description, p.category,
                       p.price, p.stock, p.image_url
                FROM cart_items ci
                JOIN products p ON p.product_id = ci.product_id
                WHERE ci.user_id = ?
                ORDER BY ci.cart_item_id DESC
                """;

        List<CartItem> items = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                            rs.getInt("p_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("category"),
                            rs.getBigDecimal("price"),
                            rs.getInt("stock"),
                            rs.getString("image_url")
                    );

                    items.add(new CartItem(
                            rs.getInt("cart_item_id"),
                            rs.getInt("user_id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            product
                    ));
                }
            }
        }

        return items;
    }

    /**
     * Add a product (or increase its quantity), capped by available stock.
     *
     * @return true when the cart was changed, false when the product is
     *         out of stock (or already at full stock).
     */
    public boolean addItem(int userId, int productId, int quantity)
            throws SQLException {

        if (quantity < 1) {
            quantity = 1;
        }

        Product product = new ProductDAO().getProductById(productId);

        if (product == null || product.getStock() <= 0) {
            return false;
        }

        String sql = """
                INSERT INTO cart_items (user_id, product_id, quantity)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    quantity = LEAST(quantity + VALUES(quantity), ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);
            statement.setInt(3, quantity);
            statement.setInt(4, product.getStock());

            statement.executeUpdate();

            // Did the row end up with quantity 0 because stock is 0?
            return getQuantity(userId, productId) > 0;
        }
    }

    /**
     * Change the quantity of one cart row. Quantity 0 removes it.
     *
     * <p>The new quantity is capped by the stock that is actually
     * available, so the cart can never hold more units than the shop
     * has.
     *
     * @return true when the row was updated
     */
    public boolean updateQuantity(int userId, int productId, int quantity)
            throws SQLException {

        if (quantity <= 0) {
            return removeItem(userId, productId);
        }

        Product product = new ProductDAO().getProductById(productId);

        if (product == null) {
            return removeItem(userId, productId);
        }

        // Never let the cart hold more than the stock allows.
        int capped = Math.min(quantity, product.getStock());

        if (capped <= 0) {
            return removeItem(userId, productId);
        }

        String sql = """
                UPDATE cart_items
                SET quantity = ?
                WHERE user_id = ? AND product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, capped);
            statement.setInt(2, userId);
            statement.setInt(3, productId);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean removeItem(int userId, int productId) throws SQLException {

        String sql = """
                DELETE FROM cart_items
                WHERE user_id = ? AND product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);

            return statement.executeUpdate() == 1;
        }
    }

    public void clearCart(int userId) throws SQLException {

        String sql = "DELETE FROM cart_items WHERE user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    /** Number of units in the cart (sum of quantities) — used for the badge. */
    public int countItems(int userId) throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(quantity), 0) AS total
                FROM cart_items
                WHERE user_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    /** Sum of line totals in the cart. */
    public BigDecimal getSubtotal(List<CartItem> items) {
        return PricingService.subtotal(items);
    }

    /**
     * Move everything from a guest's session cart into their account
     * cart, once they sign in.
     *
     * <p>Quantities of the same product are added together and then
     * capped by the available stock.
     *
     * @return the number of lines that were merged
     */
    public int mergeIntoCart(int userId, List<CartItem> guestItems)
            throws SQLException {

        if (guestItems == null || guestItems.isEmpty()) {
            return 0;
        }

        int merged = 0;

        for (CartItem guestItem : guestItems) {

            Product product = new ProductDAO().getProductById(
                    guestItem.getProductId());

            if (product == null || product.getStock() <= 0) {
                continue;      // no longer available — skip it
            }

            int existing = getQuantity(userId, guestItem.getProductId());

            if (existing > 0) {
                // Already in the cart — raise the quantity instead.
                updateQuantity(userId, guestItem.getProductId(),
                        existing + guestItem.getQuantity());
            } else {
                addItem(userId, guestItem.getProductId(),
                        guestItem.getQuantity());
            }

            merged++;
        }

        return merged;
    }

    private int getQuantity(int userId, int productId) throws SQLException {

        String sql = """
                SELECT quantity FROM cart_items
                WHERE user_id = ? AND product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setInt(2, productId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }

        return 0;
    }
}
