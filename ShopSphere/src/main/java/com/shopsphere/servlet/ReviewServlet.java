package com.shopsphere.servlet;

import com.shopsphere.dao.ReviewDAO;
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

/**
 * Customer reviews.
 *
 *   POST /review action=add    → write or update a review
 *   POST /review action=delete → remove one of my own reviews
 */
@WebServlet("/review")
public class ReviewServlet extends HttpServlet {

    private final ReviewDAO reviewDAO = new ReviewDAO();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            response.sendRedirect(request.getContextPath()
                    + "/products?error=session");
            return;
        }

        int userId = currentUserId(request);
        String action = request.getParameter("action");

        try {
            if ("delete".equals(action)) {

                int reviewId = parseInt(
                        request.getParameter("reviewId"), -1);
                if (reviewId > 0) {
                    reviewDAO.delete(reviewId, userId);
                }

                response.sendRedirect(request.getContextPath()
                        + "/products?deleted=1");
                return;
            }

            int productId = parseInt(
                    request.getParameter("productId"), -1);

            if (productId <= 0) {
                response.sendRedirect(request.getContextPath() + "/products");
                return;
            }

            int rating = parseInt(request.getParameter("rating"), 0);
            String title = trim(request.getParameter("title"));
            String comment = trim(request.getParameter("comment"));

            if (rating < 1 || rating > 5) {
                response.sendRedirect(request.getContextPath()
                        + "/product?id=" + productId
                        + "&error=Please+choose+a+rating.");
                return;
            }

            if (comment.length() < 5) {
                response.sendRedirect(request.getContextPath()
                        + "/product?id=" + productId
                        + "&error=Please+write+a+short+comment.");
                return;
            }

            Review review = new Review();
            review.setProductId(productId);
            review.setUserId(userId);
            review.setRating(rating);
            review.setTitle(title);
            review.setComment(comment);
            review.setVerifiedPurchase(
                    reviewDAO.hasPurchased(userId, productId));

            reviewDAO.save(review);

            response.sendRedirect(request.getContextPath()
                    + "/product?id=" + productId + "&reviewed=1");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to save your review.", e);
        }
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user.getUserId();
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
