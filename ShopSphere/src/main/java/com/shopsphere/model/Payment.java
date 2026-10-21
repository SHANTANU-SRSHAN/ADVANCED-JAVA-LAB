package com.shopsphere.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One payment attempt against an order.
 */
public class Payment {

    private int paymentId;
    private int orderId;
    private int userId;

    private String gateway = "SIMULATED";
    private String method = "COD";

    private String transactionId;
    private String referenceId;

    private BigDecimal amount = BigDecimal.ZERO;
    private String currency = "INR";

    /** INITIATED | SUCCESS | FAILED | REFUNDED */
    private String status = "INITIATED";

    private String failureReason;

    private String cardBrand;
    private String cardLast4;
    private String upiId;
    private String bankName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment() {
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isRefunded() {
        return "REFUNDED".equals(status);
    }

    /** A short, human readable description of the method. */
    public String getMethodLabel() {
        if (method == null) {
            return "";
        }
        return switch (method) {
            case "COD" -> "Cash on Delivery";
            case "CARD" -> "Card";
            case "UPI" -> "UPI";
            case "NETBANKING" -> "Net Banking";
            case "WALLET" -> "Wallet";
            default -> method;
        };
    }
}
