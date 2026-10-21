
package com.shopsphere.model;

import com.shopsphere.util.Images;

import java.math.BigDecimal;

public class Product {

    private int productId;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private int stock;
    private String imageUrl;

    public Product() {
    }

    public Product(int productId, String name, String description,
                   String category, BigDecimal price, int stock,
                   String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * The image URL ready to be used in an {@code src} attribute — handles
     * the context path and falls back to the bundled placeholder.
     * Called from the JSPs as {@code ${product.resolveImage(contextPath)}}.
     */
    public String resolveImage(String contextPath) {
        return Images.resolve(contextPath, imageUrl);
    }
}
