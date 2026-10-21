package com.shopsphere.payment;

import com.shopsphere.util.TokenUtil;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A self-contained payment provider that behaves like a real one.
 *
 * <p>It approves or declines payments, issues transaction ids, detects
 * the card brand and supports refunds — without contacting anything
 * outside the JVM. That makes it safe to demo, test and use in a lab.
 *
 * <p>Because the rest of the application only knows the
 * {@link PaymentGateway} interface, replacing this class with a real
 * provider requires no other change.
 *
 * <h2>Test cards</h2>
 * <table>
 *   <caption>Cards with special behaviour</caption>
 *   <tr><th>Number</th><th>Result</th></tr>
 *   <tr><td>4111 1111 1111 1111</td><td>Approved (Visa)</td></tr>
 *   <tr><td>5555 5555 5555 4444</td><td>Approved (Mastercard)</td></tr>
 *   <tr><td>3782 822463 10005</td><td>Approved (American Express)</td></tr>
 *   <tr><td>4000 0000 0000 0002</td><td>Declined — insufficient funds</td></tr>
 *   <tr><td>4000 0000 0000 9995</td><td>Declined — card declined by issuer</td></tr>
 * </table>
 */
public class SimulatedPaymentGateway implements PaymentGateway {

    /** Cards that are always declined, mapped to the decline reason. */
    private static final Map<String, String> DECLINED_CARDS = Map.of(
            "4000000000000002", "Insufficient funds",
            "4000000000009995", "Card declined by the issuer",
            "4000000000000069", "Expired card",
            "4000000000000127", "Incorrect CVV");

    private static final Set<String> SUPPORTED = Set.of(
            "COD", "CARD", "UPI", "NETBANKING", "WALLET");

    @Override
    public String name() {
        return "SIMULATED";
    }

    @Override
    public boolean supports(String method) {
        return method != null
                && SUPPORTED.contains(method.trim().toUpperCase(Locale.ROOT));
    }

    @Override
    public GatewayResult charge(GatewayRequest request) {

        if (request == null || request.amount() == null
                || request.amount().signum() <= 0) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "Invalid payment amount.");
        }

        String method = request.method() == null
                ? "" : request.method().toUpperCase(Locale.ROOT);

        return switch (method) {
            case "COD" -> chargeCashOnDelivery(request);
            case "CARD" -> chargeCard(request);
            case "UPI" -> chargeUpi(request);
            case "NETBANKING" -> chargeNetBanking(request);
            case "WALLET" -> chargeWallet(request);
            default -> GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "Unsupported payment method: " + method);
        };
    }

    @Override
    public GatewayResult refund(String transactionId, BigDecimal amount,
                                String reason) {

        if (transactionId == null || transactionId.isBlank()) {
            return GatewayResult.declined(null,
                    "Cannot refund without a transaction id.");
        }

        if (amount == null || amount.signum() <= 0) {
            return GatewayResult.declined(null,
                    "Refund amount must be greater than zero.");
        }

        String reference = TokenUtil.newReference("RFD");

        return new GatewayResult(true,
                "RFN-" + transactionId,
                reference,
                null,
                null, null, null, null,
                "Refunded " + amount + " (" + reason + ").");
    }

    // ------------------------------------------------------------
    // Method-specific behaviour
    // ------------------------------------------------------------

    private GatewayResult chargeCashOnDelivery(GatewayRequest request) {
        // Nothing is charged now — the money is collected on delivery.
        return new GatewayResult(true,
                null,
                TokenUtil.newReference("COD"),
                null,
                null, null, null, null,
                "Order confirmed. Pay " + request.amount()
                        + " when the order arrives.");
    }

    private GatewayResult chargeCard(GatewayRequest request) {

        String number = digits(request.detail("cardNumber"));
        String holder = request.detail("cardName");
        String expiry = request.detail("expiry");
        String cvv = request.detail("cvv");

        if (number.length() < 13 || number.length() > 16) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "The card number is not valid.");
        }

        if (holder == null || holder.trim().length() < 2) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "The name on the card is required.");
        }

        if (expiry == null || !expiry.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "The card expiry date is not valid.");
        }

        if (cvv == null || !cvv.matches("^\\d{3,4}$")) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "The card security code is not valid.");
        }

        String declineReason = DECLINED_CARDS.get(number);
        if (declineReason != null) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"), declineReason);
        }

        String brand = detectBrand(number);
        String last4 = number.substring(number.length() - 4);

        return GatewayResult.approved(
                "TXN" + TokenUtil.newReference("CARD").substring(5),
                TokenUtil.newReference("SIM"),
                brand,
                last4,
                null,
                null);
    }

    private GatewayResult chargeUpi(GatewayRequest request) {

        String vpa = request.detail("upiId");

        if (vpa == null || !vpa.matches(
                "^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$")) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "The UPI id is not valid.");
        }

        return GatewayResult.approved(
                "TXN" + TokenUtil.newReference("UPI").substring(5),
                TokenUtil.newReference("SIM"),
                null, null,
                vpa,
                null);
    }

    private GatewayResult chargeNetBanking(GatewayRequest request) {

        String bank = request.detail("bankName");

        if (bank == null || bank.trim().isEmpty()) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "Please choose your bank.");
        }

        return GatewayResult.approved(
                "TXN" + TokenUtil.newReference("NB").substring(5),
                TokenUtil.newReference("SIM"),
                null, null, null,
                bank.trim());
    }

    private GatewayResult chargeWallet(GatewayRequest request) {

        String wallet = request.detail("walletProvider");

        if (wallet == null || wallet.trim().isEmpty()) {
            return GatewayResult.declined(
                    TokenUtil.newReference("SIM"),
                    "Please choose a wallet.");
        }

        return GatewayResult.approved(
                "TXN" + TokenUtil.newReference("WAL").substring(5),
                TokenUtil.newReference("SIM"),
                null, null, null, null);
    }

    // ------------------------------------------------------------

    /** Work out the card brand from the leading digits. */
    public static String detectBrand(String number) {

        if (number == null || number.isEmpty()) {
            return "Unknown";
        }

        if (number.startsWith("4")) {
            return "Visa";
        }
        if (number.length() > 1) {
            char second = number.charAt(1);
            if (second >= '2' && second <= '5') {
                return "Mastercard";
            }
        }
        if (number.startsWith("34") || number.startsWith("37")) {
            return "American Express";
        }
        if (number.startsWith("60") || number.startsWith("65")
                || number.startsWith("81")) {
            return "RuPay";
        }
        if (number.startsWith("35")) {
            return "JCB";
        }
        return "Unknown";
    }

    /** Strip spaces and dashes from a card number. */
    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("[\\s-]", "");
    }
}
