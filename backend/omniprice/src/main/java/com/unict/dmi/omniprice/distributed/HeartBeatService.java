package com.unict.dmi.omniprice.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servizio HeartBeat per il monitoraggio dei worker nel sistema distribuito.
 *
 * Pattern: HeartBeat
 * Ogni worker registra periodicamente un "battito cardiaco" (timestamp).
 * Il monitor verifica che tutti i worker siano vivi: se un worker non manda
 * il heartbeat entro il timeout, viene considerato "morto" (failed) e rimosso.
 *
 * Questo pattern e' fondamentale nei sistemi distribuiti per rilevare i nodi
 * guasti senza necessita' di comunicazione diretta (failure detection).
 *
 * In OmniPrice: monitora i thread del WorkerPool e i microservizi store simulati.
 */
@Service
public class HeartBeatService {

    private static final Logger log = LoggerFactory.getLogger(HeartBeatService.class);

    private static final long HEARTBEAT_TIMEOUT_MS = 30_000; // 30 secondi

    // workerId -> ultimo heartbeat timestamp
    private final Map<String, Long> heartbeats = new ConcurrentHashMap<>();

    /**
     * Il worker registra il proprio heartbeat.
     * Chiamato periodicamente da ciascun worker/store service.
     */
    public void beat(String workerId) {
        heartbeats.put(workerId, Instant.now().toEpochMilli());
        log.debug("Heartbeat ricevuto da: {}", workerId);
    }

    /**
     * Restituisce l'elenco dei worker attivi (heartbeat recente).
     */
    public List<String> getAliveWorkers() {
        long now = Instant.now().toEpochMilli();
        return heartbeats.entrySet().stream()
                .filter(e -> (now - e.getValue()) < HEARTBEAT_TIMEOUT_MS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Restituisce l'elenco dei worker considerati morti (timeout heartbeat).
     */
    public List<String> getDeadWorkers() {
        long now = Instant.now().toEpochMilli();
        return heartbeats.entrySet().stream()
                .filter(e -> (now - e.getValue()) >= HEARTBEAT_TIMEOUT_MS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Monitor periodico: ogni 10 secondi verifica lo stato dei worker.
     * Log dei worker morti per segnalare potenziali problemi.
     */
    @Scheduled(fixedDelay = 10_000)
    public void monitorWorkers() {
        List<String> dead = getDeadWorkers();
        List<String> alive = getAliveWorkers();

        if (!dead.isEmpty()) {
            log.warn("HeartBeat: {} nodi/worker morti rilevati: {}", dead.size(), dead);
            // La reazione (es. nuova elezione del leader) e' demandata a chi usa il
            // rilevatore: per il cluster e' il ClusterCoordinator (ISD §2.2.3).
            dead.forEach(heartbeats::remove);
        }

        if (!alive.isEmpty()) {
            log.debug("HeartBeat: {} worker attivi: {}", alive.size(), alive);
        }
    }

    /**
     * Rimuove un worker dal monitoraggio (es. shutdown volontario).
     */
    public void deregister(String workerId) {
        heartbeats.remove(workerId);
        log.info("Worker deregistrato: {}", workerId);
    }

    public Map<String, Long> getAllHeartbeats() {
        return Map.copyOf(heartbeats);
    }
}
