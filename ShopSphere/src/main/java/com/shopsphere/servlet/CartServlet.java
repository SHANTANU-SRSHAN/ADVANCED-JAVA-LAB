package com.shopsphere.servlet;

import com.shopsphere.model.CartItem;
import com.shopsphere.service.CartService;
import com.shopsphere.service.PricingService;
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
 * The shopping cart.
 *
 *   GET  /cart                 → show the cart
 *   POST /cart action=add      → add a product (productId, quantity)
 *   POST /cart action=update   → change quantity
 *   POST /cart action=remove   → remove a line
 *   POST /cart action=clear    → empty the cart
 *
 * <p>Guests may use the cart too — their cart lives in the session and
 * is merged into their account cart when they sign in.
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<CartItem> items = cartService.getItems(request);

            request.setAttribute("items", items);
            request.setAttribute("subtotal",
                    PricingService.subtotal(items));
            request.setAttribute("itemCount",
                    cartService.count(request));
            request.setAttribute("csrfToken", Csrf.token(request));

            request.getRequestDispatcher("/cart.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException("Unable to load your cart.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            response.sendRedirect(request.getContextPath()
                    + "/cart?error=session");
            return;
        }

        String action = request.getParameter("action");
        int productId = parseInt(request.getParameter("productId"), -1);

        try {
            if ("add".equals(action) && productId > 0) {
                handleAdd(request, response, productId);

            } else if ("update".equals(action) && productId > 0) {
                int quantity = parseInt(request.getParameter("quantity"), 1);
                cartService.update(request, productId, quantity);
                response.sendRedirect(
                        request.getContextPath() + "/cart?updated=1");

            } else if ("remove".equals(action) && productId > 0) {
                cartService.remove(request, productId);
                response.sendRedirect(
                        request.getContextPath() + "/cart?removed=1");

            } else if ("clear".equals(action)) {
                cartService.clear(request);
                response.sendRedirect(
                        request.getContextPath() + "/cart?cleared=1");

            } else {
                response.sendRedirect(request.getContextPath() + "/cart");
            }

        } catch (SQLException e) {
            throw new ServletException("Unable to update your cart.", e);
        }
    }

    // ------------------------------------------------------------

    private void handleAdd(HttpServletRequest request,
                           HttpServletResponse response,
                           int productId)
            throws SQLException, IOException {

        int quantity = parseInt(request.getParameter("quantity"), 1);

        boolean changed = cartService.add(request, productId, quantity);

        String flag = changed ? "added=1" : "error=outofstock";
        redirectTo(request, response,
                request.getParameter("returnUrl"), flag);
    }

    /**
     * Send the visitor back to where they clicked "Add to cart".
     *
     * <p>The flag is added as a query parameter, using "&amp;" when the
     * target already has a query string of its own.
     */
    private void redirectTo(HttpServletRequest request,
                            HttpServletResponse response,
                            String returnUrl, String flag)
            throws IOException {

        String target = safeNext(returnUrl);

        if (target == null) {
            target = request.getContextPath() + "/cart";
        }

        String separator = target.contains("?") ? "&" : "?";

        response.sendRedirect(target + separator + flag);
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

    private int parseInt(String value, int fallback) {
        try {
            int number = Integer.parseInt(value);
            return number < 0 ? fallback : number;
        } catch (Exception e) {
            return fallback;
        }
    }
}
