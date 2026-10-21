package com.shopsphere.payment;

import java.math.BigDecimal;

/**
 * The contract every payment provider must satisfy.
 *
 * <p>The application only ever talks to this interface, so swapping the
 * simulated provider for a real one (Razorpay, Stripe, PayPal…) means
 * writing a single new class and changing one line in
 * {@link PaymentGateways#create()}. Nothing in the servlets, the DAOs or
 * the JSPs has to change.
 *
 * <p>Two rules every implementation must follow:
 * <ol>
 *   <li>Never throw for a declined payment — return a failed
 *       {@link GatewayResult} instead. A decline is a normal outcome.</li>
 *   <li>Never store full card numbers, CVVs or UPI PINs. Only the
 *       brand and the last four digits may be kept.</li>
 * </ol>
 */
public interface PaymentGateway {

    /** Short name stored on the payment row, e.g. "SIMULATED". */
    String name();

    /**
     * Attempt to authorise a payment.
     *
     * @return the outcome — approved or declined, never an exception
     */
    GatewayResult charge(GatewayRequest request);

    /**
     * Reverse a previous charge.
     *
     * @param transactionId the transaction to refund
     * @param amount        how much to refund
     * @param reason        why (shown in the admin area)
     * @return the outcome of the refund
     */
    GatewayResult refund(String transactionId, BigDecimal amount,
                         String reason);

    /**
     * Check whether this gateway supports the given method.
     */
    boolean supports(String method);
}
