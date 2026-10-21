package com.shopsphere.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An order with its money breakdown, address snapshot, payment details
 * and line items.
 */
public class Order {

    private int orderId;
    private String orderNumber = "";
    private int userId;

    // ---- money breakdown ----
    private BigDecimal subtotalAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String orderStatus = "PLACED";
    // PLACED | CONFIRMED | PACKED | SHIPPED | DELIVERED | CANCELLED | REFUNDED

    private String couponCode;
    private String shippingMethodCode;
    private String shippingMethodName;

    // ---- address snapshot ----
    private String fullName = "";
    private String phone = "";
    private String addressLine1 = "";
    private String addressLine2 = "";
    private String city = "";
    private String state = "";
    private String pincode = "";
    private String shippingAddress = "";

    private String notes;
    private String trackingNumber;
    private LocalDate estimatedDelivery;

    private String paymentMethod = "COD";
    private String paymentStatus = "PENDING";

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- joined display fields ----
    private String customerName = "";
    private String customerEmail = "";

    private List<OrderItem> items = new ArrayList<>();
    private List<OrderEvent> events = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();

    public Order() {
    }

    // ---- identifiers ----

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // ---- money ----

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    // ---- status ----

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getShippingMethodCode() {
        return shippingMethodCode;
    }

    public void setShippingMethodCode(String shippingMethodCode) {
        this.shippingMethodCode = shippingMethodCode;
    }

    public String getShippingMethodName() {
        return shippingMethodName;
    }

    public void setShippingMethodName(String shippingMethodName) {
        this.shippingMethodName = shippingMethodName;
    }

    // ---- address snapshot ----

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public LocalDate getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDate estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    // ---- payment ----

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    // ---- timestamps ----

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

    // ---- joined display fields ----

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public List<OrderEvent> getEvents() {
        return events;
    }

    public void setEvents(List<OrderEvent> events) {
        this.events = events;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    // ---- derived helpers ----

    /** True while the customer can still cancel the order. */
    public boolean isCancellable() {
        return "PLACED".equals(orderStatus) || "CONFIRMED".equals(orderStatus);
    }

    /** Simple step index used by the tracking timeline (0..3). */
    public int getStatusStep() {
        return switch (orderStatus == null ? "" : orderStatus) {
            case "PLACED" -> 0;
            case "CONFIRMED", "PACKED" -> 1;
            case "SHIPPED" -> 2;
            case "DELIVERED" -> 3;
            default -> -1;   // CANCELLED / REFUNDED
        };
    }

    /** Total number of units across every line. */
    public int getTotalQuantity() {
        int total = 0;
        for (OrderItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    /** A short label for the payment method, e.g. "Cash on Delivery". */
    public String getPaymentMethodLabel() {
        if (paymentMethod == null) {
            return "";
        }
        return switch (paymentMethod) {
            case "COD" -> "Cash on Delivery";
            case "CARD" -> "Card";
            case "UPI" -> "UPI";
            case "NETBANKING" -> "Net Banking";
            case "WALLET" -> "Wallet";
            default -> paymentMethod;
        };
    }
}
