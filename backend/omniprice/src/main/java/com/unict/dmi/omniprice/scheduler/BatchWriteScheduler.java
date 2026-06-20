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
     *
     * A ogni richiesta viene associato un requestId univoco (ISD §1.2 fase 2:
     * "Identificazione e Tracciamento") che permette al Batch Processor di
     * disaggregare il lotto e smistare ogni risultato alla richiesta originale.
     */
    public void queuePriceUpdate(String productId, String storeId, double newPrice) {
        Map<String, Object> update = new HashMap<>();
        update.put("requestId", UUID.randomUUID().toString());
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
            // Modalita' degradata (senza RabbitMQ): il Batch Processor gira in-process
            processBatch("local-" + UUID.randomUUID(), batch);
        }
    }

    /**
     * Batch Processor (ISD §1.2 fasi 4-5): riceve il lotto aggregato, lo disaggrega
     * nelle richieste originali, processa ciascuna e smista il risultato in base
     * al requestId associato. Qui la "scrittura" e' simulata (il layer store del
     * progetto e' in-memory), ma la disaggregazione per requestId e' reale.
     */
    void processBatch(String batchId, List<Map<String, Object>> batch) {
        int applied = 0;
        for (Map<String, Object> update : batch) {
            log.debug("Batch {} - richiesta {}: prodotto {} su store {} = {}€",
                    batchId, update.get("requestId"), update.get("productId"),
                    update.get("storeId"), update.get("price"));
            applied++;
        }
        log.info("Batch write {} completato: {} richieste disaggregate e applicate", batchId, applied);
    }
}
