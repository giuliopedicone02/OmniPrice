package com.unict.dmi.omniprice.scheduler;

import com.unict.dmi.omniprice.messaging.AlertProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Scheduler per la scrittura in batch degli aggiornamenti di prezzo.
 *
 * Pattern: Request Batch
 * Raccoglie gli aggiornamenti di prezzo in una coda in-memory durante il periodo
 * di raccolta (es. 2 minuti), poi li invia tutti insieme come un unico messaggio
 * RabbitMQ, ottimizzando le operazioni di I/O sul database.
 *
 * Invece di N scritture separate: [upd1] [upd2] ... [updN] -> database
 * Invia un unico batch: [upd1, upd2, ..., updN] -> database (una transazione)
 */
@Component
public class BatchWriteScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchWriteScheduler.class);

    // Coda concorrente per raccogliere eventi di aggiornamento prezzo
    private final ConcurrentLinkedQueue<Map<String, Object>> pendingUpdates = new ConcurrentLinkedQueue<>();

    @Autowired(required = false)
    private AlertProducer alertProducer;

    /**
     * Aggiunge un aggiornamento prezzo alla coda per il batch successivo.
     * Chiamato ogni volta che un prezzo viene aggiornato.
     */
    public void queuePriceUpdate(String productId, String storeId, double newPrice) {
        Map<String, Object> update = new HashMap<>();
        update.put("productId", productId);
        update.put("storeId", storeId);
        update.put("price", newPrice);
        update.put("timestamp", System.currentTimeMillis());
        pendingUpdates.offer(update);
    }

    /**
     * Ogni 2 minuti svuota la coda e scrive in batch.
     * Pattern: Request Batch - riduce il numero di operazioni DB da N a 1.
     */
    @Scheduled(cron = "${omniprice.scheduler.batch-write-cron:0 */2 * * * *}")
    public void flushBatch() {
        if (pendingUpdates.isEmpty()) return;

        // Drain: raccoglie tutti gli aggiornamenti pendenti
        List<Map<String, Object>> batch = new ArrayList<>();
        Map<String, Object> item;
        while ((item = pendingUpdates.poll()) != null) {
            batch.add(item);
        }

        log.info("Batch write: {} aggiornamenti prezzo in un'unica operazione", batch.size());

        if (alertProducer != null) {
            String batchId = UUID.randomUUID().toString();
            alertProducer.publishBatchWrite(batchId, batch);
        } else {
            // Simulazione scrittura batch diretta (senza RabbitMQ)
            log.info("Batch write diretto completato per {} record", batch.size());
        }
    }
}
