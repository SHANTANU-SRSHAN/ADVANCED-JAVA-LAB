package com.shopsphere.servlet;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.ProductPage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The catalogue.
 *
 * <p>Supports searching, filtering by category, sorting and paging.
 */
@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    /** How many products fit on one page. */
    private static final int PAGE_SIZE = 9;

    private ProductDAO productDAO;

    @Override
    public void init() {
        productDAO = new ProductDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String search = trim(request.getParameter("search"));
        String category = trim(request.getParameter("category"));
        String sort = trim(request.getParameter("sort"));

        if (sort.isEmpty() || !isAllowedSort(sort)) {
            sort = "newest";
        }

        int page = parseInt(request.getParameter("page"), 1);
        if (page < 1) {
            page = 1;
        }

        try {
            ProductPage result = productDAO.getProductPage(
                    search, category, sort, page, PAGE_SIZE);

            // If the customer asked for a page past the end, go back to
            // the last page that actually has products.
            if (result.getTotalPages() < page
                    && result.getTotalPages() >= 1) {
                page = result.getTotalPages();
                result = productDAO.getProductPage(
                        search, category, sort, page, PAGE_SIZE);
            }

            // Named "productPage" rather than "page" so it cannot be
            // confused with the JSP implicit object of the same name.
            request.setAttribute("productPage", result);
            request.setAttribute("products", result.getProducts());
            request.setAttribute("categories",
                    productDAO.getCategories());

            request.setAttribute("search", search);
            request.setAttribute("selectedCategory", category);
            request.setAttribute("sort", sort);
            request.setAttribute("currentPage", page);

            request.getRequestDispatcher("/products.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load products from the database.", e);
        }
    }

    private boolean isAllowedSort(String sort) {
        return switch (sort) {
            case "priceLow", "priceHigh", "name", "newest" -> true;
            default -> false;
        };
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
