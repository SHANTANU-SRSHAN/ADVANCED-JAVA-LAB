package com.shopsphere.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The details a gateway needs in order to attempt a payment.
 *
 * @param amount      how much to charge
 * @param currency    three-letter currency code
 * @param method      COD | CARD | UPI | NETBANKING | WALLET
 * @param orderId     the order this payment belongs to (may be 0)
 * @param orderNumber the human-readable order number
 * @param email       the customer's e-mail (for the receipt)
 * @param phone       the customer's phone number
 * @param details     method-specific fields (card number, upi id, bank…)
 */
public record GatewayRequest(
        BigDecimal amount,
        String currency,
        String method,
        int orderId,
        String orderNumber,
        String email,
        String phone,
        Map<String, String> details) {

    /** Convenience accessor for a method-specific field. */
    public String detail(String key) {
        if (details == null) {
            return null;
        }
        return details.get(key);
    }
}
