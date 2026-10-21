package com.shopsphere.servlet;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.dao.ReviewDAO;
import com.shopsphere.dao.WishlistDAO;
import com.shopsphere.model.Product;
import com.shopsphere.model.Review;
import com.shopsphere.model.User;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * The product detail page: photos, price, stock, reviews and ratings.
 */
@WebServlet("/product")
public class ProductDetailsServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String idParameter = request.getParameter("id");

        if (idParameter == null || idParameter.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Product ID is required."
            );
            return;
        }

        int productId;

        try {
            productId = Integer.parseInt(idParameter);

            if (productId <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID."
            );
            return;
        }

        try {
            Product product = productDAO.getProductById(productId);

            if (product == null) {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "Product not found."
                );
                return;
            }

            request.setAttribute("product", product);

            // ---- reviews and rating ----
            List<Review> reviews = reviewDAO.findByProduct(productId);
            double[] rating = reviewDAO.averageRating(productId);

            request.setAttribute("reviews", reviews);
            request.setAttribute("ratingBreakdown",
                    reviewDAO.ratingBreakdown(productId));
            request.setAttribute("averageRating", rating[0]);
            request.setAttribute("reviewCount", (int) rating[1]);

            // Wishlist heart state (only meaningful when logged in).
            User user = (User) request.getSession().getAttribute("user");

            if (user != null) {
                request.setAttribute("inWishlist",
                        wishlistDAO.contains(user.getUserId(), productId));

                request.setAttribute("myReview",
                        reviewDAO.findByUserAndProduct(
                                user.getUserId(), productId));

                request.setAttribute("hasPurchased",
                        reviewDAO.hasPurchased(user.getUserId(), productId));
            }

            // Return link for "Add to cart" / "Buy now" buttons.
            request.setAttribute("returnUrl",
                    request.getContextPath()
                            + "/product?id=" + productId);

            request.setAttribute("csrfToken", Csrf.token(request));

            request.getRequestDispatcher("/product-details.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load product details.", e);
        }
    }
}
