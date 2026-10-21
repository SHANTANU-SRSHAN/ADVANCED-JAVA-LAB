package com.shopsphere.service;

import com.shopsphere.config.AppConfig;
import com.shopsphere.model.CartItem;
import com.shopsphere.model.Coupon;
import com.shopsphere.model.OrderTotals;
import com.shopsphere.model.ShippingMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Works out what an order costs.
 *
 * <p>Every part of the application that shows money — the cart, the
 * checkout summary, the payment page and the order receipt — calls this
 * one service, so those pages can never disagree about the total.
 */
public final class PricingService {

    private PricingService() {
    }

    /** Sum of the line totals of a cart. */
    public static BigDecimal subtotal(List<CartItem> items) {

        BigDecimal total = BigDecimal.ZERO;

        if (items != null) {
            for (CartItem item : items) {
                if (item != null) {
                    total = total.add(item.getLineTotal());
                }
            }
        }

        return money(total);
    }

    /**
     * Work out the whole breakdown.
     *
     * @param items           the cart
     * @param shippingMethod  the chosen delivery option (may be null)
     * @param coupon          the applied coupon (may be null)
     */
    public static OrderTotals totals(List<CartItem> items,
                                     ShippingMethod shippingMethod,
                                     Coupon coupon) {

        BigDecimal subtotal = subtotal(items);

        // --- discount ---
        BigDecimal discount = BigDecimal.ZERO;
        if (coupon != null && subtotal.signum() > 0) {
            discount = coupon.discountFor(subtotal);
        }

        BigDecimal discounted = subtotal.subtract(discount);

        // --- shipping ---
        BigDecimal shipping = BigDecimal.ZERO;
        if (shippingMethod != null) {
            shipping = shippingMethod.getPrice() == null
                    ? BigDecimal.ZERO : shippingMethod.getPrice();

            // A coupon may remove the delivery charge.
            if ("FREESHIP".equalsIgnoreCase(couponCode(coupon))
                    || "FREESHIP".equalsIgnoreCase(
                            shippingMethod.getCode())) {
                shipping = BigDecimal.ZERO;
            }
        }

        // --- tax ---
        BigDecimal taxPercent = AppConfig.taxPercent();
        BigDecimal tax = discounted.multiply(taxPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // --- total ---
        BigDecimal total = discounted.add(shipping).add(tax);

        return new OrderTotals(
                money(subtotal),
                money(discount),
                money(shipping),
                money(tax),
                money(total));
    }

    /** The delivery charge for a given method and cart value. */
    public static BigDecimal shippingFor(ShippingMethod method,
                                         BigDecimal orderTotal) {

        if (method == null) {
            return BigDecimal.ZERO;
        }
        if (!method.isAvailableFor(orderTotal)) {
            return BigDecimal.ZERO;
        }
        return method.getPrice() == null
                ? BigDecimal.ZERO : method.getPrice();
    }

    /** Round to two decimal places, the way money is stored. */
    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String couponCode(Coupon coupon) {
        return coupon == null ? "" : coupon.getCode();
    }
}
