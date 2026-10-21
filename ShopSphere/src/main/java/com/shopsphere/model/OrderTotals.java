package com.shopsphere.model;

import java.math.BigDecimal;

/**
 * The full money breakdown of an order.
 *
 * <p>A plain class rather than a record, so that the JSP expression
 * language can reach every helper method (the EL record resolver only
 * exposes the record components).
 */
public class OrderTotals {

    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal shipping;
    private final BigDecimal tax;
    private final BigDecimal total;

    public OrderTotals(BigDecimal subtotal, BigDecimal discount,
                       BigDecimal shipping, BigDecimal tax,
                       BigDecimal total) {

        this.subtotal = nz(subtotal);
        this.discount = nz(discount);
        this.shipping = nz(shipping);
        this.tax = nz(tax);
        this.total = nz(total);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getShipping() {
        return shipping;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    /** True when a coupon was applied. */
    public boolean hasDiscount() {
        return discount.signum() > 0;
    }

    /** True when delivery is free. */
    public boolean isFreeShipping() {
        return shipping.signum() == 0;
    }
}
