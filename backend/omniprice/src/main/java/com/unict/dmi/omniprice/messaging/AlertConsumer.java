package com.unict.dmi.omniprice.messaging;

import com.unict.dmi.omniprice.config.RabbitMQConfig;
import com.unict.dmi.omniprice.model.ProcessedMessage;
import com.unict.dmi.omniprice.repository.ProcessedMessageRepository;
import com.unict.dmi.omniprice.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    // Finestra di ritenzione del registro di idempotenza (in minuti).
    @Value("${omniprice.idempotency.retention-minutes:60}")
    private long retentionMinutes;

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

        // === Request Batch (ISD §1.2 fasi 4-5): disaggregazione e smistamento ===
        // Il Batch Processor riceve il lotto aggregato, lo disaggrega nelle richieste
        // originali e processa ciascuna identificandola tramite il suo requestId.
        int applied = 0;
        Object data = message.get("data");
        if (data instanceof List<?> batch) {
            for (Object item : batch) {
                if (item instanceof Map<?, ?> update) {
                    log.debug("Batch {} - richiesta {}: prodotto {} su store {} = {}€",
                            batchId, update.get("requestId"), update.get("productId"),
                            update.get("storeId"), update.get("price"));
                    applied++;
                }
            }
        }
        log.info("Batch write {} elaborato: {} richieste disaggregate e applicate", batchId, applied);
    }

    /**
     * Pattern Idempotent Receiver (ISD §5): i messaggi gia' elaborati vengono
     * conservati solo per una finestra di ritenzione, poi cancellati. Senza questa
     * pulizia periodica il registro crescerebbe indefinitamente.
     */
    @Scheduled(cron = "${omniprice.idempotency.purge-cron:0 0 * * * *}")
    @Transactional
    public void purgeProcessedMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(retentionMinutes);
        long removed = processedMessageRepository.deleteByProcessedAtBefore(cutoff);
        if (removed > 0) {
            log.info("Idempotent Receiver: {} messaggi piu' vecchi di {} min purgati dal registro",
                    removed, retentionMinutes);
        }
    }

    private void processAlertTriggered(Map<String, Object> message) {
        String alertId = (String) message.get("alertId");
        String productName = (String) message.get("productName");
        String storeName = (String) message.get("storeName");
        double triggeredPrice = Double.parseDouble(message.get("triggeredPrice").toString());

        alertService.triggerAlert(alertId, storeName, triggeredPrice);

        log.info("Alert elaborato: {} - prodotto '{}' a {}€ su {}",
                alertId, productName, String.format("%.2f", triggeredPrice), storeName);
    }
}
