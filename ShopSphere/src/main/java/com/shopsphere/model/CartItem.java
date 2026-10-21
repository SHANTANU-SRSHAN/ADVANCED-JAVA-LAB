package com.shopsphere.model;

import java.math.BigDecimal;

/**
 * A row of the shopping cart joined with its product details.
 */
public class CartItem {

    private int cartItemId;
    private int userId;
    private int productId;
    private int quantity;
    private Product product;   // loaded from the products table

    public CartItem() {
    }

    public CartItem(int cartItemId, int userId, int productId,
                    int quantity, Product product) {
        this.cartItemId = cartItemId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.product = product;
    }

    /** price * quantity */
    public BigDecimal getLineTotal() {
        if (product == null || product.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public int getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(int cartItemId) {
        this.cartItemId = cartItemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
