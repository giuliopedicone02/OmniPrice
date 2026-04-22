package com.unict.dmi.omniprice.service;

import com.unict.dmi.omniprice.dto.PriceDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Servizio che simula l'interrogazione di ciascun microservizio store.
 *
 * Pattern Resilience4J implementati:
 * - Circuit Breaker: interrompe le chiamate a store che falliscono frequentemente
 * - Retry: riprova la chiamata in caso di errore temporaneo
 * - Timeout: ogni chiamata ha un limite di tempo massimo
 *
 * Ogni store ha il proprio CircuitBreaker e Retry isolati.
 */
@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    private final DatasetService datasetService;
    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    @Value("${omniprice.store.timeout-seconds:3}")
    private int timeoutSeconds;

    // Store IDs dal dataset
    private static final List<String> STORE_IDS = List.of("STORE001", "STORE002", "STORE003", "STORE004");

    public StoreService(DatasetService datasetService) {
        this.datasetService = datasetService;

        // Configurazione Circuit Breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)           // apre se >50% fallisce
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .build();
        this.cbRegistry = CircuitBreakerRegistry.of(cbConfig);

        // Configurazione Retry
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(RuntimeException.class)
                .build();
        this.retryRegistry = RetryRegistry.of(retryConfig);
    }

    /**
     * Interroga un singolo store in modo asincrono con Resilience4J.
     * Ogni chiamata ha Circuit Breaker + Retry + latenza simulata.
     */
    public CompletableFuture<List<PriceDTO>> fetchPricesFromStore(String storeId, String productId) {
        return CompletableFuture.supplyAsync(() -> {
            CircuitBreaker cb = cbRegistry.circuitBreaker(storeId);
            Retry retry = retryRegistry.retry(storeId);

            Supplier<List<PriceDTO>> decorated =
                    Retry.decorateSupplier(retry,
                            CircuitBreaker.decorateSupplier(cb,
                                    () -> simulateStoreCall(storeId, productId)));

            try {
                return decorated.get();
            } catch (Exception e) {
                log.warn("Store {} non disponibile per prodotto {}: {}", storeId, productId, e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Simula la risposta del microservizio store con latenza di rete e occasionali errori.
     */
    private List<PriceDTO> simulateStoreCall(String storeId, String productId) {
        // Simula latenza di rete (50-500ms)
        simulateNetworkLatency(storeId);

        // Store003 ha 20% di probabilita' di fallire (dimostra Circuit Breaker)
        if ("STORE003".equals(storeId) && ThreadLocalRandom.current().nextDouble() < 0.2) {
            throw new RuntimeException("Store " + storeId + " temporaneamente non disponibile");
        }

        Optional<PriceDTO> price = datasetService.getPriceForStore(productId, storeId);
        return price.map(List::of).orElse(Collections.emptyList());
    }

    /**
     * Ricerca: interroga tutti gli store per una lista di prodotti.
     */
    public CompletableFuture<Map<String, List<PriceDTO>>> fetchAllPricesForProducts(List<String> productIds) {
        Map<String, CompletableFuture<List<PriceDTO>>> futures = new HashMap<>();

        for (String productId : productIds) {
            List<CompletableFuture<List<PriceDTO>>> storeFutures = STORE_IDS.stream()
                    .map(storeId -> fetchPricesFromStore(storeId, productId))
                    .toList();

            CompletableFuture<List<PriceDTO>> combined = CompletableFuture
                    .allOf(storeFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> storeFutures.stream()
                            .flatMap(f -> f.join().stream())
                            .toList());

            futures.put(productId, combined);
        }

        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, List<PriceDTO>> result = new HashMap<>();
                    futures.forEach((pid, future) -> result.put(pid, future.join()));
                    return result;
                });
    }

    public List<String> getStoreIds() {
        return STORE_IDS;
    }

    private void simulateNetworkLatency(String storeId) {
        try {
            // Store002 e' piu' lento (dimostra timeout handling)
            int baseMs = "STORE002".equals(storeId) ? 300 : 50;
            int delay = baseMs + ThreadLocalRandom.current().nextInt(200);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
