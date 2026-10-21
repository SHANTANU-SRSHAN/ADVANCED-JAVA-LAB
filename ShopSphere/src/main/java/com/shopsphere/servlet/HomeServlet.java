package com.shopsphere.servlet;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Product;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Home page — loads the featured products and category list from the
 * database so the landing page is in sync with the catalog.
 */
@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<Product> featured = productDAO.getFeatured(4);
            List<String> categories = productDAO.getCategories();

            request.setAttribute("featuredProducts", featured);
            request.setAttribute("categories", categories);

            request.getRequestDispatcher("/index.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load the home page.", e);
        }
    }
}
