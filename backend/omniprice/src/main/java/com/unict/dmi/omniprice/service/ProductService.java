package com.unict.dmi.omniprice.service;

import com.unict.dmi.omniprice.dto.PriceDTO;
import com.unict.dmi.omniprice.dto.ProductDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Servizio di aggregazione prodotti.
 *
 * Pattern implementati:
 * - CompletableFuture: interroga tutti e 4 gli store microservizi in PARALLELO
 * - Remote Facade: aggrega le risposte di store multipli in un unico DTO
 * - Timeout: se uno store non risponde entro il timeout, viene escluso dal risultato
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final int AGGREGATION_TIMEOUT_SECONDS = 5;

    private final DatasetService datasetService;
    private final StoreService storeService;

    public ProductService(DatasetService datasetService, StoreService storeService) {
        this.datasetService = datasetService;
        this.storeService = storeService;
    }

    /**
     * Cerca prodotti su tutti gli store in parallelo via CompletableFuture.
     * Pattern: Remote Facade - aggrega i risultati di 4 microservizi in un unico response DTO.
     */
    public List<ProductDTO> searchProducts(String query) {
        log.info("Ricerca '{}' su {} store in parallelo", query, storeService.getStoreIds().size());

        List<DatasetService.CatalogProduct> matches = datasetService.searchProducts(query);
        if (matches.isEmpty()) return Collections.emptyList();

        List<String> productIds = matches.stream()
                .map(p -> p.id)
                .collect(Collectors.toList());

        // Interrogazione parallela di tutti gli store per tutti i prodotti trovati
        CompletableFuture<Map<String, List<PriceDTO>>> allPricesFuture =
                storeService.fetchAllPricesForProducts(productIds);

        Map<String, List<PriceDTO>> allPrices;
        try {
            allPrices = allPricesFuture.get(AGGREGATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Timeout aggregazione prezzi, uso dati parziali: {}", e.getMessage());
            allPrices = new HashMap<>();
        }

        final Map<String, List<PriceDTO>> finalPrices = allPrices;

        return matches.stream()
                .map(product -> buildProductDTO(product, finalPrices.getOrDefault(product.id, List.of())))
                .collect(Collectors.toList());
    }

    /**
     * Recupera un prodotto per ID con tutti i prezzi aggiornati dagli store.
     */
    public Optional<ProductDTO> getProductById(String productId) {
        Optional<DatasetService.CatalogProduct> productOpt = datasetService.getProductById(productId);
        if (productOpt.isEmpty()) return Optional.empty();

        List<PriceDTO> prices = datasetService.getAllPricesForProduct(productId);
        return Optional.of(buildProductDTO(productOpt.get(), prices));
    }

    /**
     * Costruisce il ProductDTO aggregando le informazioni del catalogo e dei prezzi store.
     * Pattern: Remote Facade + DTO - restituisce un oggetto leggero e completo.
     */
    private ProductDTO buildProductDTO(DatasetService.CatalogProduct product, List<PriceDTO> prices) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.id);
        dto.setName(product.name);
        dto.setCategory(product.category);
        dto.setDescription(product.description);
        dto.setImageUrl(product.imageUrl);
        dto.setPrices(prices);

        if (!prices.isEmpty()) {
            double min = prices.stream().mapToDouble(PriceDTO::getFinalPrice).min().orElse(0);
            double max = prices.stream().mapToDouble(PriceDTO::getFinalPrice).max().orElse(0);
            double avg = prices.stream().mapToDouble(PriceDTO::getFinalPrice).average().orElse(0);
            dto.setMinPrice(min);
            dto.setMaxPrice(max);
            dto.setAvgPrice(Math.round(avg * 100.0) / 100.0);
        }

        return dto;
    }
}
