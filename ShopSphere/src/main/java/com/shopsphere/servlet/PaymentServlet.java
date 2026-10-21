package com.shopsphere.servlet;

import com.shopsphere.dao.AddressDAO;
import com.shopsphere.dao.CartDAO;
import com.shopsphere.dao.CouponDAO;
import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.PaymentDAO;
import com.shopsphere.dao.ShippingMethodDAO;
import com.shopsphere.model.CartItem;
import com.shopsphere.model.Coupon;
import com.shopsphere.model.OrderDraft;
import com.shopsphere.model.OrderTotals;
import com.shopsphere.model.Payment;
import com.shopsphere.model.ShippingMethod;
import com.shopsphere.model.User;
import com.shopsphere.payment.GatewayRequest;
import com.shopsphere.payment.GatewayResult;
import com.shopsphere.payment.PaymentGateway;
import com.shopsphere.payment.PaymentGateways;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The payment step.
 *
 * <p>The order is reserved first (so the stock is held while the
 * customer types their card details), then the gateway is asked to take
 * the money:
 *
 * <ul>
 *   <li>Approved → the order becomes CONFIRMED / PAID and a payment
 *       row is written.</li>
 *   <li>Declined → the order is cancelled, the stock is released and
 *       a failed payment row is written so the attempt is on record.
 *       The customer can try a different method straight away.</li>
 * </ul>
 *
 * <p>No real money moves: the gateway is the
 * {@link com.shopsphere.payment.SimulatedPaymentGateway}. Swapping in
 * a real provider is a one-line change in
 * {@link PaymentGateways#create()}.
 */
@WebServlet("/payment")
public class PaymentServlet extends HttpServlet {

    private static final String PENDING_KEY = "pendingCheckout";

    private final CartService cartService = new CartService();
    private final CartDAO cartDAO = new CartDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final ShippingMethodDAO shippingMethodDAO =
            new ShippingMethodDAO();
    private final AddressDAO addressDAO = new AddressDAO();

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        Map<String, String> pending = pendingCheckout(request);

        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/checkout");
            return;
        }

        try {
            List<CartItem> items = cartService.getItems(request);

            if (items.isEmpty()) {
                request.getSession().removeAttribute(PENDING_KEY);
                response.sendRedirect(request.getContextPath() + "/cart");
                return;
            }

            preparePage(request, response, pending, items, null);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to open the payment page.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        Map<String, String> pending = pendingCheckout(request);

        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/checkout");
            return;
        }

        if (!Csrf.isValid(request)) {
            try {
                request.setAttribute("error",
                        "Your session expired. Please try again.");
                preparePage(request, response, pending,
                        cartService.getItems(request), null);
            } catch (SQLException e) {
                throw new ServletException(
                        "Unable to open the payment page.", e);
            }
            return;
        }

        String method = pending.get("paymentMethod");
        int userId = currentUserId(request);

        try {
            List<CartItem> items = cartService.getItems(request);

            if (items.isEmpty()) {
                request.getSession().removeAttribute(PENDING_KEY);
                response.sendRedirect(request.getContextPath() + "/cart");
                return;
            }

            // ---- work out what is owed ----
            ShippingMethod shipping =
                    shippingMethodDAO.findByCode(pending.get("shippingMethod"));

            Coupon coupon = null;
            String couponCode = pending.get("couponCode");
            if (couponCode != null && !couponCode.isBlank()) {
                CouponDAO.Validation validation =
                        couponDAO.validate(couponCode, userId,
                                PricingService.subtotal(items));
                if (validation.isValid()) {
                    coupon = validation.coupon();
                }
            }

            OrderTotals totals =
                    PricingService.totals(items, shipping, coupon);

            // ---- 1. reserve the order (holds the stock) ----
            OrderDraft draft = buildDraft(userId, items, pending,
                    shipping, coupon, method, false);

            int orderId = orderDAO.placeOrder(draft);

            if (orderId == OrderDAO.STOCK_FAILURE) {
                request.getSession().removeAttribute(PENDING_KEY);
                response.sendRedirect(request.getContextPath()
                        + "/cart?error=stock");
                return;
            }

            // ---- 2. ask the gateway to take the money ----
            PaymentGateway gateway = PaymentGateways.create();

            GatewayResult result = gateway.charge(buildGatewayRequest(
                    request, pending, totals, method, orderId));

            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setUserId(userId);
            payment.setGateway(gateway.name());
            payment.setMethod(method);
            payment.setAmount(totals.getTotal());
            payment.setCurrency("INR");
            payment.setTransactionId(result.transactionId());
            payment.setReferenceId(result.referenceId());
            payment.setCardBrand(result.cardBrand());
            payment.setCardLast4(result.cardLast4());
            payment.setUpiId(result.upiId());
            payment.setBankName(result.bankName());

            if (result.success()) {

                payment.setStatus("SUCCESS");
                paymentDAO.insert(payment);

                // The order is paid and confirmed.
                orderDAO.updateStatus(orderId, "CONFIRMED",
                        "Payment received (" + method + ").",
                        "system");

                markOrderPaid(orderId);
                recordCouponUse(userId, coupon, orderId);

                request.getSession().removeAttribute(PENDING_KEY);

                response.sendRedirect(request.getContextPath()
                        + "/order?id=" + orderId + "&placed=1");
                return;

            }

            // ---- declined ----
            payment.setStatus("FAILED");
            payment.setFailureReason(result.failureReason());
            paymentDAO.insert(payment);

            // Release the stock and cancel the reserved order.
            orderDAO.cancelOrder(orderId, userId);

            request.setAttribute("error",
                    "The payment was declined: " + result.failureReason()
                            + ". Please try a different method.");
            request.setAttribute("failedReference", result.referenceId());

            preparePage(request, response, pending, items, result);

        } catch (SQLException e) {
            throw new ServletException(
                    "Unable to complete the payment.", e);
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    /** Put everything the payment page needs into the request. */
    private void preparePage(HttpServletRequest request,
                             HttpServletResponse response,
                             Map<String, String> pending,
                             List<CartItem> items,
                             GatewayResult failure)
            throws SQLException, IOException, ServletException {

        int userId = currentUserId(request);

        ShippingMethod shipping =
                shippingMethodDAO.findByCode(pending.get("shippingMethod"));

        Coupon coupon = null;
        String couponCode = pending.get("couponCode");
        if (couponCode != null && !couponCode.isBlank()) {
            CouponDAO.Validation validation =
                    couponDAO.validate(couponCode, userId,
                            PricingService.subtotal(items));
            if (validation.isValid()) {
                coupon = validation.coupon();
            }
        }

        OrderTotals totals =
                PricingService.totals(items, shipping, coupon);

        request.setAttribute("pending", pending);
        request.setAttribute("items", items);
        request.setAttribute("subtotal", totals.getSubtotal());
        request.setAttribute("totals", totals);
        request.setAttribute("discount", totals.getDiscount());
        request.setAttribute("shippingAmount", totals.getShipping());
        request.setAttribute("taxAmount", totals.getTax());
        request.setAttribute("total", totals.getTotal());
        request.setAttribute("coupon", coupon);
        request.setAttribute("shipping", shipping);
        request.setAttribute("csrfToken", Csrf.token(request));

        // The form fields the customer already typed, so a decline does
        // not wipe the page.
        request.setAttribute("cardNumber",
                request.getParameter("cardNumber"));
        request.setAttribute("cardName", request.getParameter("cardName"));
        request.setAttribute("expiry", request.getParameter("expiry"));
        request.setAttribute("cvv", request.getParameter("cvv"));
        request.setAttribute("upiId", request.getParameter("upiId"));
        request.setAttribute("bankName", request.getParameter("bankName"));

        request.getRequestDispatcher("/payment.jsp")
               .forward(request, response);
    }

    private GatewayRequest buildGatewayRequest(
            HttpServletRequest request,
            Map<String, String> pending,
            OrderTotals totals,
            String method,
            int orderId) {

        Map<String, String> details = new HashMap<>();

        details.put("cardNumber", request.getParameter("cardNumber"));
        details.put("cardName", request.getParameter("cardName"));
        details.put("expiry", request.getParameter("expiry"));
        details.put("cvv", request.getParameter("cvv"));
        details.put("upiId", request.getParameter("upiId"));
        details.put("bankName", request.getParameter("bankName"));
        details.put("walletProvider",
                request.getParameter("walletProvider"));

        User user = (User) request.getSession().getAttribute("user");

        return new GatewayRequest(
                totals.getTotal(),
                "INR",
                method,
                orderId,
                "",
                user == null ? "" : user.getEmail(),
                pending.get("phone"),
                details);
    }

    private OrderDraft buildDraft(int userId, List<CartItem> items,
                                  Map<String, String> pending,
                                  ShippingMethod shipping,
                                  Coupon coupon, String method,
                                  boolean paid) {

        OrderDraft draft = new OrderDraft();

        draft.setUserId(userId);
        draft.setItems(items);
        draft.setFullName(pending.get("fullName"));
        draft.setPhone(pending.get("phone"));
        draft.setAddressLine1(pending.get("addressLine1"));
        draft.setAddressLine2(pending.get("addressLine2"));
        draft.setCity(pending.get("city"));
        draft.setState(pending.get("state"));
        draft.setPincode(pending.get("pincode"));
        draft.setNotes(pending.get("notes"));
        draft.setShippingMethod(shipping);
        draft.setCoupon(coupon);
        draft.setPaymentMethod(method);
        draft.setPaid(paid);

        return draft;
    }

    /** Mark the order's own payment status as paid. */
    private void markOrderPaid(int orderId) throws SQLException {

        String sql = """
                UPDATE orders SET payment_status = 'PAID'
                WHERE order_id = ?
                """;

        try (java.sql.Connection connection =
                     com.shopsphere.dao.DBConnection.getConnection();
             java.sql.PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, orderId);
            statement.executeUpdate();
        }
    }

    private void recordCouponUse(int userId, Coupon coupon, int orderId)
            throws SQLException {

        if (coupon == null) {
            return;
        }

        java.math.BigDecimal amount = coupon.getDiscountValue();
        if (coupon.isPercent()) {
            amount = coupon.getMaxDiscount() != null
                    ? coupon.getMaxDiscount() : coupon.getDiscountValue();
        }

        couponDAO.recordRedemption(coupon.getCouponId(), userId, orderId,
                amount);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> pendingCheckout(HttpServletRequest request) {
        return (Map<String, String>)
                request.getSession().getAttribute(PENDING_KEY);
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        return user.getUserId();
    }
}
