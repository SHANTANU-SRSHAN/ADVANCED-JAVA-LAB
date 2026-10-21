package com.shopsphere.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A discount coupon.
 */
public class Coupon {

    private int couponId;
    private String code = "";
    private String description = "";

    /** PERCENT or FIXED. */
    private String discountType = "PERCENT";

    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private BigDecimal maxDiscount;

    private Integer usageLimit;
    private int usedCount;
    private int perUserLimit = 1;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private boolean active = true;

    public Coupon() {
    }

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public int getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(int perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPercent() {
        return "PERCENT".equalsIgnoreCase(discountType);
    }

    /**
     * The discount this coupon gives on the given amount.
     *
     * <p>Percentage coupons are capped by {@code maxDiscount} when one
     * is set. The result is never negative and never more than the
     * amount itself.
     */
    public BigDecimal discountFor(BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;

        if (isPercent()) {
            discount = amount.multiply(discountValue)
                    .divide(new BigDecimal("100"), 2,
                            java.math.RoundingMode.HALF_UP);

            if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }

        } else {
            discount = discountValue;
        }

        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }

        return discount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * True when the coupon is currently usable at all (active, in date,
     * and under its total usage limit).
     */
    public boolean isCurrentlyValid() {
        if (!active) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }
        return usageLimit == null || usedCount < usageLimit;
    }
}
