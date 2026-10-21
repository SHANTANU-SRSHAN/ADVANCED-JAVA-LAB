package com.shopsphere.model;

import java.time.LocalDateTime;

/**
 * A delivery option offered at checkout.
 */
public class ShippingMethod {

    private int methodId;
    private String code = "";
    private String name = "";
    private String description = "";

    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;
    private java.math.BigDecimal minOrderAmount = java.math.BigDecimal.ZERO;

    private int maxDays = 5;
    private boolean active = true;
    private int sortOrder;

    public ShippingMethod() {
    }

    public int getMethodId() {
        return methodId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public java.math.BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(java.math.BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public int getMaxDays() {
        return maxDays;
    }

    public void setMaxDays(int maxDays) {
        this.maxDays = maxDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** True when this method may be used for an order of the given size. */
    public boolean isAvailableFor(java.math.BigDecimal orderTotal) {
        if (!active) {
            return false;
        }
        if (orderTotal == null) {
            return true;
        }
        return orderTotal.compareTo(minOrderAmount) >= 0;
    }

    /** e.g. "2-3 business days" */
    public String getDeliveryEstimate() {
        if (maxDays <= 1) {
            return "Next business day";
        }
        return "Within " + maxDays + " business days";
    }
}
