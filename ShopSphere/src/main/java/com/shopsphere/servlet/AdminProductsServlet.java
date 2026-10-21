package com.shopsphere.servlet;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Admin product management: list, add, edit and delete products.
 *
 *   GET  /admin/products          → list (+ ?edit=ID opens the form)
 *   POST /admin/products action=add|update|delete
 */
@WebServlet("/admin/products")
public class AdminProductsServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            String search = request.getParameter("search");
            String category = request.getParameter("category");
            String sort = request.getParameter("sort");

            request.setAttribute("products",
                    productDAO.getProducts(
                            search == null ? "" : search,
                            category == null ? "" : category,
                            sort == null || sort.isBlank() ? "newest" : sort));
            request.setAttribute("categories", productDAO.getCategories());

            // Edit form target (?edit=5)
            String editId = request.getParameter("edit");
            if (editId != null && !editId.isBlank()) {
                try {
                    request.setAttribute("editProduct",
                            productDAO.getProductById(
                                    Integer.parseInt(editId)));
                } catch (NumberFormatException ignored) {
                    // bad id → just show the list
                }
            }

            request.getRequestDispatcher("/admin/products.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load products for the admin area.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!com.shopsphere.util.Csrf.isValid(request)) {
            response.sendRedirect(request.getContextPath()
                    + "/admin/products?error=Your+session+expired."
                    + "+Please+try+again.");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("add".equals(action)) {
                handleAdd(request, response);
            } else if ("update".equals(action)) {
                handleUpdate(request, response);
            } else if ("delete".equals(action)) {
                handleDelete(request, response);
            } else {
                response.sendRedirect(
                        request.getContextPath() + "/admin/products");
            }

        } catch (SQLException e) {
            throw new ServletException(
                    "Database error while saving the product.", e);
        }
    }

    private void handleAdd(HttpServletRequest request,
                           HttpServletResponse response)
            throws SQLException, IOException {

        ProductForm form = readForm(request);

        if (form.error == null) {
            productDAO.addProduct(form.name, form.description,
                    form.category, form.price, form.stock, form.imageUrl);
            response.sendRedirect(request.getContextPath()
                    + "/admin/products?saved=1");
        } else {
            response.sendRedirect(request.getContextPath()
                    + "/admin/products?error="
                    + java.net.URLEncoder.encode(form.error, "UTF-8"));
        }
    }

    private void handleUpdate(HttpServletRequest request,
                              HttpServletResponse response)
            throws SQLException, IOException {

        int productId;
        try {
            productId = Integer.parseInt(
                    request.getParameter("productId"));
        } catch (Exception e) {
            productId = -1;
        }

        ProductForm form = readForm(request);

        if (productId > 0 && form.error == null) {
            productDAO.updateProduct(productId, form.name, form.description,
                    form.category, form.price, form.stock, form.imageUrl);
            response.sendRedirect(request.getContextPath()
                    + "/admin/products?saved=1");
        } else {
            String message = form.error != null ? form.error
                    : "Invalid product id.";
            response.sendRedirect(request.getContextPath()
                    + "/admin/products?error="
                    + java.net.URLEncoder.encode(message, "UTF-8"));
        }
    }

    private void handleDelete(HttpServletRequest request,
                              HttpServletResponse response)
            throws SQLException, IOException {

        int productId;
        try {
            productId = Integer.parseInt(
                    request.getParameter("productId"));
        } catch (Exception e) {
            productId = -1;
        }

        boolean deleted = productId > 0
                && productDAO.deleteProduct(productId);

        response.sendRedirect(request.getContextPath()
                + "/admin/products?"
                + (deleted ? "deleted=1"
                           : "error=Product+is+part+of+an+order+and+cannot+be+deleted."));
    }

    private static class ProductForm {
        String name, description, category, imageUrl, error;
        BigDecimal price;
        int stock;
    }

    private ProductForm readForm(HttpServletRequest request) {
        ProductForm form = new ProductForm();

        form.name = trim(request.getParameter("name"));
        form.description = trim(request.getParameter("description"));
        form.category = trim(request.getParameter("category"));
        form.imageUrl = trim(request.getParameter("imageUrl"));

        if (form.name.length() < 2) {
            form.error = "Product name is required.";
            return form;
        }

        try {
            form.price = new BigDecimal(trim(request.getParameter("price")));
            if (form.price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            form.error = "Price must be a number greater than 0.";
            return form;
        }

        try {
            form.stock = Integer.parseInt(trim(request.getParameter("stock")));
            if (form.stock < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            form.error = "Stock must be 0 or more.";
            return form;
        }

        if (form.category.isEmpty()) {
            form.category = "Other";
        }

        return form;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
