package com.unict.dmi.omniprice.messaging;

import com.unict.dmi.omniprice.config.RabbitMQConfig;
import com.unict.dmi.omniprice.model.ProcessedMessage;
import com.unict.dmi.omniprice.repository.ProcessedMessageRepository;
import com.unict.dmi.omniprice.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumatore messaggi RabbitMQ per gli alert di prezzo.
 *
 * Pattern: Idempotent Receiver
 * Prima di elaborare ogni messaggio, verifica se il messageId e' gia' stato processato
 * (salvato in DB). Se duplicato, scarta silenziosamente.
 * Questo garantisce esattamente-una-volta la semantica anche in caso di ritrasmissioni.
 *
 * Pattern: Request Pipeline
 * Fa parte della pipeline asincrona: PriceCheckScheduler -> RabbitMQ -> AlertConsumer
 */
@Component
@ConditionalOnProperty(name = "omniprice.rabbitmq.enabled", havingValue = "true")
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final ProcessedMessageRepository processedMessageRepository;
    private final AlertService alertService;

    public AlertConsumer(ProcessedMessageRepository processedMessageRepository,
                         AlertService alertService) {
        this.processedMessageRepository = processedMessageRepository;
        this.alertService = alertService;
    }

    @RabbitListener(queues = RabbitMQConfig.ALERT_QUEUE)
    @Transactional
    public void processAlertMessage(Map<String, Object> message) {
        String messageId = (String) message.get("messageId");
        String messageType = (String) message.get("messageType");

        // === Idempotent Receiver: scarta messaggi duplicati ===
        if (processedMessageRepository.existsByMessageId(messageId)) {
            log.warn("Messaggio duplicato scartato: {}", messageId);
            return;
        }

        // Registra il messaggio come processato (prima dell'elaborazione per atomicita')
        processedMessageRepository.save(new ProcessedMessage(messageId, messageType));

        // Elaborazione del messaggio
        if ("ALERT_TRIGGERED".equals(messageType)) {
            processAlertTriggered(message);
        } else {
            log.warn("Tipo messaggio sconosciuto: {}", messageType);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.BATCH_QUEUE)
    @Transactional
    public void processBatchWrite(Map<String, Object> message) {
        String messageId = (String) message.get("messageId");

        // Idempotency check
        if (processedMessageRepository.existsByMessageId(messageId)) {
            log.warn("Batch duplicato scartato: {}", messageId);
            return;
        }

        processedMessageRepository.save(new ProcessedMessage(messageId, "BATCH_WRITE"));

        String batchId = (String) message.get("batchId");
        log.info("Elaborazione batch write: {}", batchId);
        // In produzione: scrittura ottimizzata in batch sul DB
    }

    private void processAlertTriggered(Map<String, Object> message) {
        String alertId = (String) message.get("alertId");
        String productName = (String) message.get("productName");
        String storeName = (String) message.get("storeName");
        double triggeredPrice = Double.parseDouble(message.get("triggeredPrice").toString());

        alertService.triggerAlert(alertId, storeName, triggeredPrice);

        log.info("Alert elaborato: {} - prodotto '{}' a {:.2f}€ su {}",
                alertId, productName, triggeredPrice, storeName);
    }
}
