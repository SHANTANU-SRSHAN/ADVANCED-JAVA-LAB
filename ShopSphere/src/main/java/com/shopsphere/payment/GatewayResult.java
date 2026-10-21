package com.shopsphere.payment;

import java.math.BigDecimal;

/**
 * The result of asking a gateway to authorise a payment.
 *
 * @param success        true when the payment was approved
 * @param transactionId  the gateway's transaction id (null on failure)
 * @param referenceId    a short reference shown to the customer
 * @param failureReason  why the payment was declined (null on success)
 * @param cardBrand      detected card brand, e.g. "Visa" (may be null)
 * @param cardLast4      last four digits of the card (may be null)
 * @param upiId          the UPI id that was charged (may be null)
 * @param bankName       the bank used for net banking (may be null)
 * @param message        a human readable message for the customer
 */
public record GatewayResult(
        boolean success,
        String transactionId,
        String referenceId,
        String failureReason,
        String cardBrand,
        String cardLast4,
        String upiId,
        String bankName,
        String message) {

    public static GatewayResult approved(String transactionId,
                                         String referenceId,
                                         String cardBrand,
                                         String cardLast4,
                                         String upiId,
                                         String bankName) {
        return new GatewayResult(true, transactionId, referenceId, null,
                cardBrand, cardLast4, upiId, bankName, "Payment approved.");
    }

    public static GatewayResult declined(String referenceId,
                                         String failureReason) {
        return new GatewayResult(false, null, referenceId, failureReason,
                null, null, null, null, "Payment was declined.");
    }
}
