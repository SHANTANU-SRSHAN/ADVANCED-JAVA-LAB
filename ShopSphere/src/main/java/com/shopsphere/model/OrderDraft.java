package com.shopsphere.model;

import java.util.List;

/**
 * Everything needed to create an order, collected in one place.
 */
public class OrderDraft {

    private int userId;

    private List<CartItem> items;

    // ---- address snapshot ----
    private String fullName = "";
    private String phone = "";
    private String addressLine1 = "";
    private String addressLine2 = "";
    private String city = "";
    private String state = "";
    private String pincode = "";

    // ---- options ----
    private ShippingMethod shippingMethod;
    private Coupon coupon;
    private String notes;

    private String paymentMethod = "COD";

    /** true when the money has already been taken. */
    private boolean paid;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

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

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    /** The address as one line. */
    public String toSingleLine() {
        StringBuilder line = new StringBuilder();
        line.append(addressLine1 == null ? "" : addressLine1.trim());

        if (addressLine2 != null && !addressLine2.isBlank()) {
            line.append(", ").append(addressLine2.trim());
        }
        line.append(", ").append(city == null ? "" : city.trim());

        if (state != null && !state.isBlank()) {
            line.append(", ").append(state.trim());
        }
        line.append(" - ").append(pincode == null ? "" : pincode.trim());

        return line.toString();
    }
}
