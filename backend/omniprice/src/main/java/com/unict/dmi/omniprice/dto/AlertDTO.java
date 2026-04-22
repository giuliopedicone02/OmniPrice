package com.unict.dmi.omniprice.dto;

import java.time.LocalDateTime;

/**
 * DTO per Alert prezzi.
 * Pattern: DTO - trasferisce solo i dati necessari al frontend.
 */
public class AlertDTO {

    private String id;
    private String productId;
    private String productName;
    private Double targetPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
    private String triggeredStore;
    private Double triggeredPrice;

    public AlertDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getTargetPrice() { return targetPrice; }
    public void setTargetPrice(Double targetPrice) { this.targetPrice = targetPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public String getTriggeredStore() { return triggeredStore; }
    public void setTriggeredStore(String triggeredStore) { this.triggeredStore = triggeredStore; }

    public Double getTriggeredPrice() { return triggeredPrice; }
    public void setTriggeredPrice(Double triggeredPrice) { this.triggeredPrice = triggeredPrice; }
}
