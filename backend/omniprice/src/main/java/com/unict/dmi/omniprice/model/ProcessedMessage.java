package com.unict.dmi.omniprice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro messaggi gia' elaborati.
 * Pattern: Idempotent Receiver - evita doppia elaborazione di messaggi RabbitMQ duplicati.
 */
@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

    @Id
    private String messageId;

    @Column(nullable = false)
    private String messageType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedMessage() {}

    public ProcessedMessage(String messageId, String messageType) {
        this.messageId = messageId;
        this.messageType = messageType;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
