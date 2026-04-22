package com.unict.dmi.omniprice.messaging;

import com.unict.dmi.omniprice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Produttore messaggi RabbitMQ per gli alert di prezzo.
 * Pattern: Request Pipeline - pubblica il messaggio nella pipeline asincrona.
 */
@Component
@ConditionalOnProperty(name = "omniprice.rabbitmq.enabled", havingValue = "true")
public class AlertProducer {

    private static final Logger log = LoggerFactory.getLogger(AlertProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public AlertProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Pubblica un messaggio di alert triggered nella coda RabbitMQ.
     * Ogni messaggio ha un messageId univoco per il pattern Idempotent Receiver.
     */
    public void publishAlertTriggered(String alertId, String userId,
                                       String productName, String storeName,
                                       double targetPrice, double triggeredPrice) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("messageType", "ALERT_TRIGGERED");
        message.put("alertId", alertId);
        message.put("userId", userId);
        message.put("productName", productName);
        message.put("storeName", storeName);
        message.put("targetPrice", targetPrice);
        message.put("triggeredPrice", triggeredPrice);
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ALERT_EXCHANGE,
                RabbitMQConfig.ALERT_ROUTING_KEY,
                message
        );

        log.info("Alert pubblicato: {} per prodotto '{}'", alertId, productName);
    }

    /**
     * Pubblica un batch di aggiornamenti prezzi per la scrittura ottimizzata su DB.
     * Pattern: Request Batch - raccoglie operazioni e le invia in un unico messaggio.
     */
    public void publishBatchWrite(String batchId, Object batchData) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("messageType", "BATCH_WRITE");
        message.put("batchId", batchId);
        message.put("data", batchData);
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ALERT_EXCHANGE,
                RabbitMQConfig.BATCH_ROUTING_KEY,
                message
        );

        log.info("Batch write pubblicato: {}", batchId);
    }
}
