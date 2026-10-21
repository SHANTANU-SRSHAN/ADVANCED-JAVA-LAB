package com.shopsphere.model;

import com.shopsphere.util.Images;

import java.math.BigDecimal;

/**
 * One line of an order. unit_price is the price at purchase time.
 */
public class OrderItem {

    private int orderItemId;
    private int orderId;
    private int productId;
    private int quantity;
    private BigDecimal unitPrice;

    /** Stored line total; null means "compute it from unit price". */
    private BigDecimal lineTotal;

    // Display-only fields joined from products
    private String productName;
    private String imageUrl;

    public OrderItem() {
    }

    public OrderItem(int orderItemId, int orderId, int productId,
                     int quantity, BigDecimal unitPrice,
                     String productName, String imageUrl) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.productName = productName;
        this.imageUrl = imageUrl;
    }

    public BigDecimal getLineTotal() {
        if (lineTotal != null) {
            return lineTotal;
        }
        if (unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** Image URL ready for a JSP {@code src} attribute. */
    public String resolveImage(String contextPath) {
        return Images.resolve(contextPath, imageUrl);
    }
}
