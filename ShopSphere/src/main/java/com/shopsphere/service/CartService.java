package com.shopsphere.service;

import com.shopsphere.dao.CartDAO;
import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.CartItem;
import com.shopsphere.model.Product;
import com.shopsphere.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One cart for the whole application.
 *
 * <p>A visitor who is not signed in still gets a cart — it lives in
 * their session. The moment they sign in, the guest cart is merged into
 * their account cart, so nothing is lost and the badge stays correct.
 *
 * <p>Every page that shows a cart calls this service, which is why the
 * cart badge, the cart page and the checkout summary can never show
 * different numbers.
 */
public class CartService {

    /** Session attribute that holds the guest cart. */
    public static final String GUEST_CART = "guestCart";

    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();

    // ------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------

    /** The signed-in customer, or null for a guest. */
    public User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("user");
    }

    /** The current cart — the account cart, or the guest cart. */
    public List<CartItem> getItems(HttpServletRequest request)
            throws SQLException {

        User user = currentUser(request);

        if (user != null) {
            return cartDAO.getItems(user.getUserId());
        }

        return guestCart(request);
    }

    /** The number of units in the current cart (for the badge). */
    public int count(HttpServletRequest request) throws SQLException {

        User user = currentUser(request);

        if (user != null) {
            return cartDAO.countItems(user.getUserId());
        }

        int total = 0;
        for (CartItem item : guestCart(request)) {
            total += item.getQuantity();
        }
        return total;
    }

    /** The subtotal of the current cart. */
    public BigDecimal subtotal(HttpServletRequest request)
            throws SQLException {
        return PricingService.subtotal(getItems(request));
    }

    // ------------------------------------------------------------
    // Changing
    // ------------------------------------------------------------

    /**
     * Add a product to the current cart.
     *
     * @return true when the cart was changed
     */
    public boolean add(HttpServletRequest request, int productId,
                       int quantity) throws SQLException {

        if (quantity < 1) {
            quantity = 1;
        }

        Product product = productDAO.getProductById(productId);

        if (product == null || product.getStock() <= 0) {
            return false;
        }

        User user = currentUser(request);

        if (user != null) {
            return cartDAO.addItem(user.getUserId(), productId, quantity);
        }

        // ---- guest cart ----
        Map<Integer, CartItem> cart = guestCartMap(request);

        CartItem existing = cart.get(productId);

        if (existing == null) {
            cart.put(productId, new CartItem(0, 0, productId, quantity,
                    product));
        } else {
            int wanted = existing.getQuantity() + quantity;
            int capped = Math.min(wanted, product.getStock());
            cart.put(productId, new CartItem(0, 0, productId, capped,
                    product));
        }

        storeGuestCart(request, cart);
        return true;
    }

    /**
     * Change the quantity of a line in the current cart.
     *
     * @return true when the cart was changed
     */
    public boolean update(HttpServletRequest request, int productId,
                          int quantity) throws SQLException {

        User user = currentUser(request);

        if (user != null) {
            return cartDAO.updateQuantity(user.getUserId(), productId,
                    quantity);
        }

        Map<Integer, CartItem> cart = guestCartMap(request);

        if (!cart.containsKey(productId)) {
            return false;
        }

        if (quantity <= 0) {
            cart.remove(productId);
            storeGuestCart(request, cart);
            return true;
        }

        Product product = productDAO.getProductById(productId);
        if (product == null) {
            cart.remove(productId);
            storeGuestCart(request, cart);
            return false;
        }

        cart.put(productId, new CartItem(0, 0, productId,
                Math.min(quantity, product.getStock()), product));

        storeGuestCart(request, cart);
        return true;
    }

    /**
     * Remove a line from the current cart.
     */
    public boolean remove(HttpServletRequest request, int productId)
            throws SQLException {

        User user = currentUser(request);

        if (user != null) {
            return cartDAO.removeItem(user.getUserId(), productId);
        }

        Map<Integer, CartItem> cart = guestCartMap(request);

        boolean removed = cart.remove(productId) != null;

        storeGuestCart(request, cart);
        return removed;
    }

    /** Empty the current cart. */
    public void clear(HttpServletRequest request) throws SQLException {

        User user = currentUser(request);

        if (user != null) {
            cartDAO.clearCart(user.getUserId());
        } else {
            request.getSession().removeAttribute(GUEST_CART);
        }
    }

    // ------------------------------------------------------------
    // Merging
    // ------------------------------------------------------------

    /**
     * Move the guest cart into the customer's account cart.
     *
     * <p>Called right after a successful sign-in.
     *
     * @return the number of guest lines that were merged
     */
    public int mergeGuestCartIntoAccount(HttpServletRequest request,
                                          int userId) throws SQLException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            return 0;
        }

        @SuppressWarnings("unchecked")
        Map<Integer, CartItem> guestCart =
                (Map<Integer, CartItem>) session.getAttribute(GUEST_CART);

        if (guestCart == null || guestCart.isEmpty()) {
            return 0;
        }

        int merged = cartDAO.mergeIntoCart(userId,
                new ArrayList<>(guestCart.values()));

        session.removeAttribute(GUEST_CART);
        return merged;
    }

    // ------------------------------------------------------------
    // Guest cart storage
    // ------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<Integer, CartItem> guestCartMap(HttpServletRequest request) {

        HttpSession session = request.getSession(true);

        Object stored = session.getAttribute(GUEST_CART);

        if (stored instanceof Map) {
            return (Map<Integer, CartItem>) stored;
        }

        return new LinkedHashMap<>();
    }

    private void storeGuestCart(HttpServletRequest request,
                                Map<Integer, CartItem> cart) {
        request.getSession().setAttribute(GUEST_CART, cart);
    }

    private List<CartItem> guestCart(HttpServletRequest request) {
        return new ArrayList<>(guestCartMap(request).values());
    }
}
