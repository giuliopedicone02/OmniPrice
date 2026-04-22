package com.unict.dmi.omniprice.scheduler;

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

    @Autowired(required = false)
    private AlertProducer alertProducer;  // null se RabbitMQ disabilitato

    public PriceCheckScheduler(AlertService alertService, DatasetService datasetService) {
        this.alertService = alertService;
        this.datasetService = datasetService;
    }

    @Scheduled(cron = "${omniprice.scheduler.price-check-cron:0 */5 * * * *}")
    public void checkPrices() {
        List<AlertDTO> activeAlerts = alertService.getActiveAlerts();
        if (activeAlerts.isEmpty()) return;

        log.info("Price check: verifica {} alert attivi", activeAlerts.size());

        for (AlertDTO alert : activeAlerts) {
            checkAlert(alert);
        }
    }

    private void checkAlert(AlertDTO alert) {
        List<PriceDTO> prices = datasetService.getAllPricesForProduct(alert.getProductId());

        Optional<PriceDTO> bestPrice = prices.stream()
                .filter(p -> p.getFinalPrice() <= alert.getTargetPrice())
                .min((a, b) -> Double.compare(a.getFinalPrice(), b.getFinalPrice()));

        if (bestPrice.isPresent()) {
            PriceDTO triggering = bestPrice.get();
            log.info("Alert {} scattato: {} a {:.2f}€ su {} (target: {:.2f}€)",
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
