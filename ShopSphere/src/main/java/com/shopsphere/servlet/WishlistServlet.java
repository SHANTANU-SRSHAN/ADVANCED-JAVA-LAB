package com.shopsphere.servlet;

import com.shopsphere.dao.WishlistDAO;
import com.shopsphere.model.User;
import com.shopsphere.service.CartService;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Wishlist page and the "save / unsave" toggle used by the
 * product pages.
 *
 *   GET  /wishlist              → my saved products
 *   POST /wishlist action=toggle → save / remove a product
 *   POST /wishlist action=moveToCart → move a saved product to the cart
 */
@WebServlet("/wishlist")
public class WishlistServlet extends HttpServlet {

    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int userId = currentUserId(request);

            request.setAttribute("products",
                    wishlistDAO.getWishlist(userId));
            request.setAttribute("csrfToken", Csrf.token(request));

            request.getRequestDispatcher("/wishlist.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to load your wishlist.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            response.sendRedirect(request.getContextPath()
                    + "/wishlist?error=session");
            return;
        }

        String action = request.getParameter("action");
        String returnUrl = safeNext(request.getParameter("returnUrl"));

        try {
            int productId = Integer.parseInt(
                    request.getParameter("productId"));

            if ("moveToCart".equals(action)) {

                boolean added = cartService.add(request, productId, 1);

                if (added) {
                    wishlistDAO.remove(currentUserId(request), productId);
                }

                response.sendRedirect((returnUrl != null
                        ? returnUrl
                        : request.getContextPath() + "/wishlist")
                        + (added ? "?moved=1" : "?error=outofstock"));

                return;
            }

            boolean saved = wishlistDAO.toggle(
                    currentUserId(request), productId);

            response.sendRedirect((returnUrl != null
                    ? returnUrl
                    : request.getContextPath() + "/wishlist")
                    + (saved ? "?saved=1" : "?removed=1"));

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/wishlist");

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to update your wishlist.", e);
        }
    }

    private String safeNext(String next) {
        if (next == null || next.isBlank()) {
            return null;
        }
        if (next.startsWith("/") && !next.startsWith("//")
                && !next.contains("\\") && !next.contains(":")) {
            return next;
        }
        return null;
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user.getUserId();
    }
}
