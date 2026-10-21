package com.shopsphere.payment;

/**
 * Chooses which payment provider the application should use.
 *
 * <p>To move to a real gateway, add a class that implements
 * {@link PaymentGateway} and change the body of {@link #create()} to
 * return it. Nothing else in the application needs to be touched.
 */
public final class PaymentGateways {

    private PaymentGateways() {
    }

    /**
     * The gateway used for every payment.
     *
     * <p>Swap this line for {@code return new RazorpayGateway();} when
     * you are ready to take real payments.
     */
    public static PaymentGateway create() {
        return new SimulatedPaymentGateway();
    }
}
