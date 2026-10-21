
package com.shopsphere.dao;

import com.shopsphere.model.Product;
import com.shopsphere.model.ProductPage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    // Get all products, with optional search, category and sorting.
    public List<Product> getProducts(
            String search, String category, String sort)
            throws SQLException {

        List<Product> products = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT product_id, name, description, category,
                       price, stock, image_url
                FROM products
                WHERE 1 = 1
                """);

        if (search != null && !search.isBlank()) {
            sql.append("""
                     AND (name LIKE ?
                          OR description LIKE ?
                          OR category LIKE ?)
                    """);
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ? ");
        }

        // Only allow predefined sorting options.
        switch (sort == null ? "newest" : sort) {
            case "priceLow":
                sql.append(" ORDER BY price ASC, product_id DESC");
                break;

            case "priceHigh":
                sql.append(" ORDER BY price DESC, product_id DESC");
                break;

            case "name":
                sql.append(" ORDER BY name ASC");
                break;

            case "newest":
            default:
                sql.append(" ORDER BY product_id DESC");
                break;
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            int index = 1;

            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim() + "%";

                statement.setString(index++, keyword);
                statement.setString(index++, keyword);
                statement.setString(index++, keyword);
            }

            if (category != null && !category.isBlank()) {
                statement.setString(index++, category);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        }

        return products;
    }

    // Keep compatibility with code that requests every product.
    public List<Product> getAllProducts() throws SQLException {
        return getProducts("", "", "newest");
    }

    /**
     * One page of products, with the search, the category filter and the
     * sorting applied.
     *
     * @param page     the page number (1-based)
     * @param pageSize how many products per page
     */
    public ProductPage getProductPage(String search, String category,
                                      String sort, int page, int pageSize)
            throws SQLException {

        if (pageSize < 1) {
            pageSize = 9;
        }
        if (page < 1) {
            page = 1;
        }

        int total = countMatching(search, category);

        int offset = (page - 1) * pageSize;

        List<Product> products = new ArrayList<>();

        // Nothing to show — do not run the query at all.
        if (total == 0) {
            return new ProductPage(products, 0, page, pageSize);
        }

        StringBuilder sql = new StringBuilder("""
                SELECT product_id, name, description, category,
                       price, stock, image_url
                FROM products
                WHERE is_active = 1
                """);

        List<Object> parameters = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append("""
                     AND (name LIKE ?
                          OR description LIKE ?
                          OR category LIKE ?)
                    """);
            String keyword = "%" + search.trim() + "%";
            parameters.add(keyword);
            parameters.add(keyword);
            parameters.add(keyword);
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ? ");
            parameters.add(category);
        }

        sql.append(orderBy(sort));
        sql.append(" LIMIT ? OFFSET ?");

        parameters.add(pageSize);
        parameters.add(offset);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            int index = 1;
            for (Object parameter : parameters) {
                statement.setObject(index++, parameter);
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        }

        return new ProductPage(products, total, page, pageSize);
    }

    /** How many products match a search and category filter. */
    public int countMatching(String search, String category)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM products WHERE is_active = 1");

        List<Object> parameters = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append("""
                     AND (name LIKE ?
                          OR description LIKE ?
                          OR category LIKE ?)
                    """);
            String keyword = "%" + search.trim() + "%";
            parameters.add(keyword);
            parameters.add(keyword);
            parameters.add(keyword);
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ? ");
            parameters.add(category);
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            int index = 1;
            for (Object parameter : parameters) {
                statement.setObject(index++, parameter);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** The ORDER BY clause for one of the allowed sort options. */
    private String orderBy(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "priceLow" -> " ORDER BY price ASC, product_id DESC";
            case "priceHigh" -> " ORDER BY price DESC, product_id DESC";
            case "name" -> " ORDER BY name ASC";
            case "rating" -> " ORDER BY name ASC";
            case "newest" -> " ORDER BY product_id DESC";
            default -> " ORDER BY product_id DESC";
        };
    }

    // Newest products — used for the "Featured products" home section.
    public List<Product> getFeatured(int limit) throws SQLException {

        List<Product> products = new ArrayList<>();

        String sql = """
                SELECT product_id, name, description, category,
                       price, stock, image_url
                FROM products
                ORDER BY product_id DESC
                LIMIT ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    products.add(mapProduct(rs));
                }
            }
        }

        return products;
    }

    // ---------- Admin: create / update / delete ----------

    public boolean addProduct(String name, String description, String category,
                              java.math.BigDecimal price, int stock,
                              String imageUrl) throws SQLException {

        String sql = """
                INSERT INTO products
                    (name, description, category, price, stock, image_url)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, category);
            statement.setBigDecimal(4, price);
            statement.setInt(5, stock);
            statement.setString(6, imageUrl);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateProduct(int productId, String name,
                                 String description, String category,
                                 java.math.BigDecimal price, int stock,
                                 String imageUrl) throws SQLException {

        String sql = """
                UPDATE products
                SET name = ?, description = ?, category = ?,
                    price = ?, stock = ?, image_url = ?
                WHERE product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, category);
            statement.setBigDecimal(4, price);
            statement.setInt(5, stock);
            statement.setString(6, imageUrl);
            statement.setInt(7, productId);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteProduct(int productId) throws SQLException {

        // Order items keep the product row (FK), so block deletion
        // instead of losing order history.
        String sql = """
                DELETE FROM products
                WHERE product_id = ?
                  AND product_id NOT IN (
                      SELECT DISTINCT product_id FROM order_items
                  )
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            return statement.executeUpdate() == 1;
        }
    }

    public int countProducts() throws SQLException {

        String sql = "SELECT COUNT(*) FROM products";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // Find one product by its ID for the product details page.
    public Product getProductById(int productId)
            throws SQLException {

        String sql = """
                SELECT product_id, name, description, category,
                       price, stock, image_url
                FROM products
                WHERE product_id = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        }

        return null;
    }

    // Load category names dynamically from the database.
    public List<String> getCategories() throws SQLException {

        List<String> categories = new ArrayList<>();

        String sql = """
                SELECT DISTINCT category
                FROM products
                WHERE category IS NOT NULL
                  AND TRIM(category) <> ''
                ORDER BY category
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }

        return categories;
    }

    // Convert a database row into a Product object.
    private Product mapProduct(ResultSet rs) throws SQLException {

        return new Product(
                rs.getInt("product_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getBigDecimal("price"),
                rs.getInt("stock"),
                rs.getString("image_url")
        );
    }
}
