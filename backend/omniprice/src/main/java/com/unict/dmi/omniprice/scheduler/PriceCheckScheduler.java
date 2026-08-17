package com.unict.dmi.omniprice.scheduler;

import com.unict.dmi.omniprice.distributed.GenerationClockService;
import com.unict.dmi.omniprice.dto.AlertDTO;
import com.unict.dmi.omniprice.dto.PriceDTO;
import com.unict.dmi.omniprice.messaging.AlertProducer;
import com.unict.dmi.omniprice.service.AlertService;
import com.unict.dmi.omniprice.service.DatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Scheduler periodico per il controllo prezzi e trigger degli alert.
 *
 * Pattern: Request Pipeline
 * Questo scheduler e' la sorgente della pipeline:
 * PriceCheckScheduler -> pubblica in RabbitMQ -> AlertConsumer -> aggiorna DB
 *
 * Se RabbitMQ non e' abilitato, elabora gli alert direttamente (modalita' degradata).
 */
@Component
public class PriceCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceCheckScheduler.class);

    private final AlertService alertService;
    private final DatasetService datasetService;
    private final GenerationClockService generationClockService;
    private final BatchWriteScheduler batchWriteScheduler;

    @Autowired(required = false)
    private AlertProducer alertProducer;  // null se RabbitMQ disabilitato

    public PriceCheckScheduler(AlertService alertService,
                               DatasetService datasetService,
                               GenerationClockService generationClockService,
                               BatchWriteScheduler batchWriteScheduler) {
        this.alertService = alertService;
        this.datasetService = datasetService;
        this.generationClockService = generationClockService;
        this.batchWriteScheduler = batchWriteScheduler;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Price check iniziale all'avvio dell'applicazione...");
        checkPrices();
    }

    @Scheduled(cron = "${omniprice.scheduler.price-check-cron:0 */5 * * * *}")
    public void checkPrices() {
        List<AlertDTO> activeAlerts = alertService.getActiveAlerts();
        if (activeAlerts.isEmpty()) return;

        // === Generation Clock (epoca): tutti gli aggiornamenti di QUESTO ciclo
        // condividono l'epoca corrente del cluster. L'epoca cambia solo al cambio
        // di leader (nuova elezione), non ad ogni aggiornamento (ISD §2.4). ===
        long generation = generationClockService.getCurrentGeneration();

        log.info("Price check: verifica {} alert attivi (epoca {})", activeAlerts.size(), generation);

        for (AlertDTO alert : activeAlerts) {
            checkAlert(alert, generation);
        }
    }

    private void checkAlert(AlertDTO alert, long generation) {
        // Applica l'aggiornamento solo se l'epoca non e' obsoleta: un aggiornamento
        // proveniente da un leader con epoca inferiore a quella gia' applicata
        // sul prodotto (leader "zombie") viene scartato.
        if (!generationClockService.tryUpdate(alert.getProductId(), generation)) {
            log.debug("Aggiornamento obsoleto scartato per prodotto {} (gen {})",
                    alert.getProductId(), generation);
            return;
        }

        List<PriceDTO> prices = datasetService.getAllPricesForProduct(alert.getProductId());

        // === Request Batch: accoda ogni osservazione di prezzo per la scrittura batch ===
        // I prezzi vengono raccolti in ConcurrentLinkedQueue e scritti in batch
        // ogni 2 minuti da BatchWriteScheduler, riducendo le operazioni di I/O.
        for (PriceDTO price : prices) {
            batchWriteScheduler.queuePriceUpdate(
                    alert.getProductId(), price.getStore(), price.getFinalPrice()
            );
        }

        Optional<PriceDTO> bestPrice = prices.stream()
                .filter(p -> p.getFinalPrice() <= alert.getTargetPrice())
                .min((a, b) -> Double.compare(a.getFinalPrice(), b.getFinalPrice()));

        if (bestPrice.isPresent()) {
            PriceDTO triggering = bestPrice.get();
            log.info("Alert {} scattato: {} a {}€ su {} (target: {}€)",
                    alert.getId(), alert.getProductName(),
                    triggering.getFinalPrice(), triggering.getStore(),
                    alert.getTargetPrice());

            if (alertProducer != null) {
                // Via RabbitMQ (pattern pipeline asincrona)
                alertProducer.publishAlertTriggered(
                        alert.getId(), "system",
                        alert.getProductName(), triggering.getStore(),
                        alert.getTargetPrice(), triggering.getFinalPrice()
                );
            } else {
                // Modalita' degradata: aggiornamento diretto senza RabbitMQ
                alertService.triggerAlert(alert.getId(), triggering.getStore(), triggering.getFinalPrice());
            }
        }
    }
}
