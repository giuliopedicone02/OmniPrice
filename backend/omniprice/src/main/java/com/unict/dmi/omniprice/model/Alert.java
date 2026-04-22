package com.unict.dmi.omniprice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entita Alert prezzi.
 * Pattern: Session State - ogni alert mantiene lo stato di configurazione dell'utente.
 */
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Double targetPrice;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, TRIGGERED, CANCELLED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "triggered_store")
    private String triggeredStore;

    @Column(name = "triggered_price")
    private Double triggeredPrice;

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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
