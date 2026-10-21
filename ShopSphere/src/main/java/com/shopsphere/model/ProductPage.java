package com.shopsphere.model;

import java.util.List;

/**
 * One page of products plus the information needed to render a pager.
 *
 * <p>This is a plain class rather than a record on purpose: the JSP
 * expression language resolves record properties only through the
 * record components, so any extra helper method would be invisible to
 * the pages. A normal class with ordinary getters works everywhere.
 */
public class ProductPage {

    private final List<Product> products;
    private final int totalItems;
    private final int page;
    private final int pageSize;

    public ProductPage(List<Product> products, int totalItems,
                       int page, int pageSize) {

        this.products = products == null ? List.of() : products;
        this.totalItems = totalItems;
        this.page = page;
        this.pageSize = pageSize < 1 ? 9 : pageSize;
    }

    public List<Product> getProducts() {
        return products;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    /** Total number of pages. */
    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < getTotalPages();
    }

    /** The page number to show before this one. */
    public int getPreviousPage() {
        return Math.max(1, page - 1);
    }

    /** The page number to show after this one. */
    public int getNextPage() {
        return Math.min(getTotalPages(), page + 1);
    }

    /** The first product number shown on this page. */
    public int getFirstItemNumber() {
        return totalItems == 0 ? 0 : (page - 1) * pageSize + 1;
    }

    /** The last product number shown on this page. */
    public int getLastItemNumber() {
        return Math.min(totalItems, page * pageSize);
    }

    /** True when the search found nothing. */
    public boolean isEmpty() {
        return products.isEmpty();
    }
}
