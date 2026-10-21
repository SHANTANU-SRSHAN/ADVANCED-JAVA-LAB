package com.shopsphere.servlet;

import com.shopsphere.dao.AddressDAO;
import com.shopsphere.dao.CartDAO;
import com.shopsphere.dao.CouponDAO;
import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.ShippingMethodDAO;
import com.shopsphere.model.Address;
import com.shopsphere.model.CartItem;
import com.shopsphere.model.Coupon;
import com.shopsphere.model.OrderTotals;
import com.shopsphere.model.ShippingMethod;
import com.shopsphere.model.User;
import com.shopsphere.service.CartService;
import com.shopsphere.service.PricingService;
import com.shopsphere.util.Csrf;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checkout — where the customer chooses where the order goes, how fast
 * it should arrive, how it is paid for and which coupon to use.
 *
 *   GET  /checkout            → the address / delivery / payment form
 *   POST /checkout            → validate and continue to payment
 *   POST /checkout action=coupon → apply or remove a coupon
 *
 * <p>The totals shown here are produced by {@link PricingService}, the
 * same service the cart and the receipt use, so the three pages always
 * agree.
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{10}$");
    private static final Pattern PINCODE_PATTERN =
            Pattern.compile("^[0-9]{6}$");

    /** Session attribute holding the coupon the customer has applied. */
    private static final String COUPON_SESSION_KEY = "appliedCoupon";

    private final CartDAO cartDAO = new CartDAO();
    private final CartService cartService = new CartService();
    private final OrderDAO orderDAO = new OrderDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final ShippingMethodDAO shippingMethodDAO =
            new ShippingMethodDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<CartItem> items = cartService.getItems(request);

            if (items.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/cart");
                return;
            }

            preparePage(request, response, items, null, null, null);

        } catch (SQLException e) {
            throw new ServletException("Unable to open checkout.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        if (!Csrf.isValid(request)) {
            request.setAttribute("error",
                    "Your session expired. Please try again.");
            try {
                preparePage(request, response,
                        cartService.getItems(request), null, null, null);
            } catch (SQLException e) {
                throw new ServletException("Unable to open checkout.", e);
            }
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("coupon".equals(action)) {
                handleCoupon(request, response);
            } else if ("coupon-remove".equals(action)) {
                request.getSession().removeAttribute(COUPON_SESSION_KEY);
                request.getSession().setAttribute("couponMessage",
                        "The coupon was removed.");
                response.sendRedirect(
                        request.getContextPath() + "/checkout");
            } else if ("saveAddress".equals(action)) {
                handleSaveAddress(request, response);
            } else {
                handleCheckout(request, response);
            }

        } catch (SQLException e) {
            throw new ServletException("Unable to place your order.", e);
        }
    }

    // ------------------------------------------------------------
    // The main checkout
    // ------------------------------------------------------------

    private void handleCheckout(HttpServletRequest request,
                                HttpServletResponse response)
            throws SQLException, IOException, ServletException {

        int userId = currentUserId(request);

        List<CartItem> items = cartService.getItems(request);

        if (items.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        // ---- read the form ----
        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String addressLine1 = trim(request.getParameter("addressLine1"));
        String addressLine2 = trim(request.getParameter("addressLine2"));
        String city = trim(request.getParameter("city"));
        String state = trim(request.getParameter("state"));
        String pincode = trim(request.getParameter("pincode"));
        String notes = trim(request.getParameter("notes"));
        String method = trim(request.getParameter("paymentMethod"))
                .toUpperCase();
        String shippingCode = trim(request.getParameter("shippingMethod"))
                .toUpperCase();

        String error = validate(fullName, phone, addressLine1, city,
                pincode, method);

        // ---- resolve the delivery option ----
        ShippingMethod shipping = shippingMethodDAO.findByCode(shippingCode);

        if (shipping == null || !shipping.isActive()) {
            shipping = shippingMethodDAO.findByCode("STANDARD");
        }
        if (shipping == null) {
            error = "No delivery option is available right now.";
        }

        // ---- resolve the coupon ----
        Coupon coupon = appliedCoupon(request);
        BigDecimal subtotal = PricingService.subtotal(items);

        if (coupon != null) {
            CouponDAO.Validation validation =
                    couponDAO.validate(coupon.getCode(), userId, subtotal);

            if (!validation.isValid()) {
                // The coupon stopped being usable — drop it politely.
                request.getSession().removeAttribute(COUPON_SESSION_KEY);
                request.getSession().setAttribute("couponMessage",
                        validation.error());
                coupon = null;
            } else {
                coupon = validation.coupon();
            }
        }

        if (error != null) {
            preparePage(request, response, items, error, coupon, shipping);
            return;
        }

        // ---- save the address to the address book (optional) ----
        if ("on".equals(request.getParameter("saveAddress"))) {
            saveAddress(userId, fullName, phone, addressLine1,
                    addressLine2, city, state, pincode);
        }

        // ---- Cash on Delivery can be placed straight away ----
        if ("COD".equals(method)) {
            com.shopsphere.model.OrderDraft draft = buildDraft(
                    userId, items, fullName, phone, addressLine1,
                    addressLine2, city, state, pincode, shipping, coupon,
                    notes, method, false);

            int orderId = orderDAO.placeOrder(draft);

            if (orderId == OrderDAO.STOCK_FAILURE) {
                response.sendRedirect(request.getContextPath()
                        + "/cart?error=stock");
                return;
            }

            recordCouponUse(userId, coupon, orderId);

            request.getSession().removeAttribute(COUPON_SESSION_KEY);

            response.sendRedirect(request.getContextPath()
                    + "/order?id=" + orderId + "&placed=1");
            return;
        }

        // ---- Card / UPI / Net banking / Wallet → the payment step ----
        java.util.Map<String, String> pending = new java.util.HashMap<>();

        pending.put("fullName", fullName);
        pending.put("phone", phone);
        pending.put("addressLine1", addressLine1);
        pending.put("addressLine2", addressLine2);
        pending.put("city", city);
        pending.put("state", state);
        pending.put("pincode", pincode);
        pending.put("notes", notes);
        pending.put("paymentMethod", method);
        pending.put("shippingMethod", shipping.getCode());
        pending.put("couponCode", coupon == null ? "" : coupon.getCode());

        request.getSession().setAttribute("pendingCheckout", pending);

        response.sendRedirect(request.getContextPath() + "/payment");
    }

    // ------------------------------------------------------------
    // Coupons
    // ------------------------------------------------------------

    private void handleCoupon(HttpServletRequest request,
                              HttpServletResponse response)
            throws SQLException, IOException {

        int userId = currentUserId(request);
        List<CartItem> items = cartService.getItems(request);
        BigDecimal subtotal = PricingService.subtotal(items);

        String code = trim(request.getParameter("couponCode"));
        String remove = request.getParameter("remove");

        if ("1".equals(remove)) {
            request.getSession().removeAttribute(COUPON_SESSION_KEY);
            request.getSession().setAttribute("couponMessage",
                    "The coupon was removed.");
        } else if (code.isEmpty()) {
            request.getSession().setAttribute("couponMessage",
                    "Please enter a coupon code.");
        } else {
            CouponDAO.Validation validation =
                    couponDAO.validate(code, userId, subtotal);

            if (validation.isValid()) {
                request.getSession().setAttribute(COUPON_SESSION_KEY,
                        validation.coupon());
                request.getSession().setAttribute("couponMessage",
                        "Coupon " + validation.coupon().getCode()
                                + " applied.");
            } else {
                request.getSession().removeAttribute(COUPON_SESSION_KEY);
                request.getSession().setAttribute("couponMessage",
                        validation.error());
            }
        }

        response.sendRedirect(request.getContextPath() + "/checkout");
    }

    // ------------------------------------------------------------
    // Address book
    // ------------------------------------------------------------

    private void handleSaveAddress(HttpServletRequest request,
                                   HttpServletResponse response)
            throws SQLException, IOException {

        int userId = currentUserId(request);

        saveAddress(userId,
                trim(request.getParameter("fullName")),
                trim(request.getParameter("phone")),
                trim(request.getParameter("addressLine1")),
                trim(request.getParameter("addressLine2")),
                trim(request.getParameter("city")),
                trim(request.getParameter("state")),
                trim(request.getParameter("pincode")));

        request.getSession().setAttribute("couponMessage",
                "The address was saved to your address book.");

        response.sendRedirect(request.getContextPath() + "/checkout");
    }

    private void saveAddress(int userId, String fullName, String phone,
                             String addressLine1, String addressLine2,
                             String city, String state, String pincode)
            throws SQLException {

        Address address = new Address();
        address.setFullName(fullName);
        address.setPhone(phone);
        address.setAddressLine1(addressLine1);
        address.setAddressLine2(addressLine2);
        address.setCity(city);
        address.setState(state);
        address.setPincode(pincode);
        address.setLabel("Home");

        addressDAO.insert(userId, address);
    }

    // ------------------------------------------------------------
    // Page preparation
    // ------------------------------------------------------------

    /**
     * Put everything the checkout page needs into the request.
     */
    private void preparePage(HttpServletRequest request,
                             HttpServletResponse response,
                             List<CartItem> items,
                             String error,
                             Coupon coupon,
                             ShippingMethod shipping)
            throws SQLException, IOException, ServletException {

        int userId = currentUserId(request);

        if (coupon == null) {
            coupon = appliedCoupon(request);
        }

        if (shipping == null) {
            shipping = shippingMethodDAO.findByCode(
                    trim(request.getParameter("shippingMethod")));
        }

        OrderTotals totals = PricingService.totals(items, shipping, coupon);

        request.setAttribute("items", items);
        request.setAttribute("subtotal", totals.getSubtotal());
        request.setAttribute("totals", totals);
        request.setAttribute("discount", totals.getDiscount());
        request.setAttribute("shippingAmount", totals.getShipping());
        request.setAttribute("taxAmount", totals.getTax());
        request.setAttribute("total", totals.getTotal());

        request.setAttribute("coupon", coupon);
        request.setAttribute("shipping", shipping);
        request.setAttribute("shippingMethods",
                shippingMethodDAO.findActive());
        request.setAttribute("availableCoupons",
                couponDAO.findAvailable(userId, totals.getSubtotal()));

        request.setAttribute("addresses", addressDAO.findByUser(userId));
        request.setAttribute("defaultAddress", addressDAO.findDefault(userId));

        request.setAttribute("csrfToken", Csrf.token(request));

        request.getRequestDispatcher("/checkout.jsp")
               .forward(request, response);
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private com.shopsphere.model.OrderDraft buildDraft(
            int userId, List<CartItem> items,
            String fullName, String phone, String addressLine1,
            String addressLine2, String city, String state, String pincode,
            ShippingMethod shipping, Coupon coupon, String notes,
            String method, boolean paid) {

        com.shopsphere.model.OrderDraft draft =
                new com.shopsphere.model.OrderDraft();

        draft.setUserId(userId);
        draft.setItems(items);
        draft.setFullName(fullName);
        draft.setPhone(phone);
        draft.setAddressLine1(addressLine1);
        draft.setAddressLine2(addressLine2);
        draft.setCity(city);
        draft.setState(state);
        draft.setPincode(pincode);
        draft.setShippingMethod(shipping);
        draft.setCoupon(coupon);
        draft.setNotes(notes);
        draft.setPaymentMethod(method);
        draft.setPaid(paid);

        return draft;
    }

    /** Record that the customer used the coupon on this order. */
    private void recordCouponUse(int userId, Coupon coupon, int orderId)
            throws SQLException {

        if (coupon == null) {
            return;
        }

        BigDecimal amount = coupon.getDiscountValue();
        if (coupon.isPercent()) {
            amount = coupon.getMaxDiscount() != null
                    ? coupon.getMaxDiscount() : coupon.getDiscountValue();
        }

        couponDAO.recordRedemption(coupon.getCouponId(), userId, orderId,
                amount);
    }

    /** The coupon currently applied in the session, if any. */
    private Coupon appliedCoupon(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object stored = session.getAttribute(COUPON_SESSION_KEY);
        return stored instanceof Coupon ? (Coupon) stored : null;
    }

    private String validate(String fullName, String phone, String address,
                            String city, String pincode, String method) {

        if (fullName.length() < 2) {
            return "Please enter the recipient's full name.";
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "Please enter a 10-digit phone number.";
        }
        if (address.length() < 5) {
            return "Please enter your full street address.";
        }
        if (city.length() < 2) {
            return "Please enter your city.";
        }
        if (!PINCODE_PATTERN.matcher(pincode).matches()) {
            return "Please enter a 6-digit pincode.";
        }
        if (!"COD".equals(method) && !"CARD".equals(method)
                && !"UPI".equals(method) && !"NETBANKING".equals(method)
                && !"WALLET".equals(method)) {
            return "Please choose a payment method.";
        }

        return null;
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user.getUserId();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
