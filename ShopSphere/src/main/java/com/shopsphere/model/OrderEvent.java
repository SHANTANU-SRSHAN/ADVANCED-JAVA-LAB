package com.shopsphere.model;

import java.time.LocalDateTime;

/**
 * One entry in an order's tracking timeline.
 */
public class OrderEvent {

    private int historyId;
    private int orderId;

    private String status = "";
    private String note = "";

    /** Who caused the change — the customer name or "admin@…". */
    private String changedBy = "";

    private LocalDateTime createdAt;

    public OrderEvent() {
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** A short description of the status for the timeline. */
    public String getTitle() {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "PLACED" -> "Order placed";
            case "CONFIRMED" -> "Payment confirmed";
            case "PACKED" -> "Packed and ready";
            case "SHIPPED" -> "Out for delivery";
            case "DELIVERED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            case "REFUNDED" -> "Refunded";
            default -> status;
        };
    }
}
