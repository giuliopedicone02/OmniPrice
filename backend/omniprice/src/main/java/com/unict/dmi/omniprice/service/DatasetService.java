package com.unict.dmi.omniprice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unict.dmi.omniprice.dto.PriceDTO;
import com.unict.dmi.omniprice.dto.PriceHistoryDTO;
import com.unict.dmi.omniprice.dto.PriceRecordDTO;
import com.unict.dmi.omniprice.dto.ProductDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servizio che carica i dataset JSON dal classpath e li espone ai microservizi store.
 * I file JSON del dataset vengono inclusi nel classpath via pom.xml (resources section).
 */
@Service
public class DatasetService {

    private static final Logger log = LoggerFactory.getLogger(DatasetService.class);

    private final ObjectMapper objectMapper;

    // Cache in-memory dei dati caricati
    private List<CatalogProduct> products = new ArrayList<>();
    // productId -> storeId -> StorePriceEntry
    private Map<String, Map<String, StorePriceEntry>> pricesByProductAndStore = new HashMap<>();
    // storeId -> store metadata
    private Map<String, StoreMetadata> storeMetadata = new HashMap<>();
    // productId -> PriceHistoryDTO
    private Map<String, PriceHistoryDTO> priceHistories = new HashMap<>();

    public DatasetService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadDataset() {
        try {
            loadProducts();
            loadStorePrices();
            loadPriceHistory();
            log.info("Dataset caricato: {} prodotti, {} store", products.size(), storeMetadata.size());
        } catch (IOException e) {
            log.error("Errore caricamento dataset: {}", e.getMessage());
        }
    }

    // ===== Accesso ai dati =====

    public List<CatalogProduct> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public List<CatalogProduct> searchProducts(String query) {
        String q = query.toLowerCase();
        return products.stream()
                .filter(p -> p.name.toLowerCase().contains(q)
                        || p.category.toLowerCase().contains(q)
                        || p.subcategory.toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public Optional<CatalogProduct> getProductById(String productId) {
        return products.stream().filter(p -> p.id.equals(productId)).findFirst();
    }

    /**
     * Restituisce i prezzi di un prodotto per uno store specifico.
     * Usato da StoreService per simulare la risposta di ciascun microservizio store.
     */
    public Optional<PriceDTO> getPriceForStore(String productId, String storeId) {
        Map<String, StorePriceEntry> byStore = pricesByProductAndStore.get(productId);
        if (byStore == null) return Optional.empty();
        StorePriceEntry entry = byStore.get(storeId);
        if (entry == null) return Optional.empty();
        return Optional.of(toDTO(entry, storeId));
    }

    /**
     * Restituisce tutti i prezzi di un prodotto per tutti gli store.
     */
    public List<PriceDTO> getAllPricesForProduct(String productId) {
        Map<String, StorePriceEntry> byStore = pricesByProductAndStore.get(productId);
        if (byStore == null) return Collections.emptyList();
        return byStore.entrySet().stream()
                .map(e -> toDTO(e.getValue(), e.getKey()))
                .collect(Collectors.toList());
    }

    public Map<String, StoreMetadata> getStoreMetadata() {
        return Collections.unmodifiableMap(storeMetadata);
    }

    /**
     * Restituisce lo storico prezzi di un prodotto (Serialized LOB pattern).
     */
    public Optional<PriceHistoryDTO> getPriceHistory(String productId) {
        return Optional.ofNullable(priceHistories.get(productId));
    }

    // ===== Caricamento JSON =====

    private void loadProducts() throws IOException {
        ClassPathResource res = new ClassPathResource("dataset/product-catalog.json");
        ProductCatalog catalog = objectMapper.readValue(res.getInputStream(), ProductCatalog.class);
        this.products = catalog.products != null ? catalog.products : new ArrayList<>();
    }

    private void loadStorePrices() throws IOException {
        ClassPathResource res = new ClassPathResource("dataset/store-prices.json");
        StoreCatalog catalog = objectMapper.readValue(res.getInputStream(), StoreCatalog.class);
        if (catalog.stores == null) return;

        for (StoreData store : catalog.stores) {
            storeMetadata.put(store.storeId, new StoreMetadata(store.storeId, store.storeName, store.storeUrl));
            if (store.prices != null) {
                for (StorePriceEntry entry : store.prices) {
                    entry.storeId = store.storeId;
                    entry.storeName = store.storeName;
                    pricesByProductAndStore
                            .computeIfAbsent(entry.productId, k -> new HashMap<>())
                            .put(store.storeId, entry);
                }
            }
        }
    }

    private void loadPriceHistory() throws IOException {
        ClassPathResource res = new ClassPathResource("dataset/price-history.json");
        PriceHistoryCatalog catalog = objectMapper.readValue(res.getInputStream(), PriceHistoryCatalog.class);
        if (catalog.priceHistory == null) return;

        for (ProductHistory ph : catalog.priceHistory) {
            List<PriceRecordDTO> records = ph.records == null ? new ArrayList<>() :
                    ph.records.stream()
                            .map(r -> new PriceRecordDTO(r.date, r.storeId, r.storeName,
                                    r.price, r.discount, r.finalPrice))
                            .collect(Collectors.toList());
            priceHistories.put(ph.productId, new PriceHistoryDTO(ph.productId, ph.productName, records));
        }
    }

    private PriceDTO toDTO(StorePriceEntry entry, String storeId) {
        StoreMetadata meta = storeMetadata.get(storeId);
        PriceDTO dto = new PriceDTO();
        dto.setStore(entry.storeName);
        dto.setStoreId(storeId);
        dto.setPrice(entry.price);
        dto.setDiscount(entry.discount);
        dto.setFinalPrice(entry.finalPrice);
        dto.setCurrency("EUR");
        dto.setUrl(meta != null ? meta.storeUrl + "/product/" + entry.productId : "#");
        dto.setAvailability(entry.availability);
        dto.setEstimatedDelivery(entry.estimatedDelivery);
        dto.setRating(entry.rating);
        dto.setReviews(entry.reviews);
        return dto;
    }

    // ===== Classi interne per deserializzazione JSON =====

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogProduct {
        public String id;
        public String name;
        public String category;
        public String subcategory;
        public String description;
        public String imageUrl;
        public String releaseDate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ProductCatalog {
        public List<CatalogProduct> products;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StoreCatalog {
        public List<StoreData> stores;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StoreData {
        public String storeId;
        public String storeName;
        public String storeUrl;
        public List<StorePriceEntry> prices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class StorePriceEntry {
        public String productId;
        public String productName;
        public double price;
        public double discount;
        public double finalPrice;
        public String availability;
        public String estimatedDelivery;
        public double rating;
        public int reviews;
        // Popolati dopo il caricamento
        public String storeId;
        public String storeName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PriceHistoryCatalog {
        public List<ProductHistory> priceHistory;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ProductHistory {
        public String productId;
        public String productName;
        public List<HistoryRecord> records;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class HistoryRecord {
        public String date;
        public String storeId;
        public String storeName;
        public double price;
        public double discount;
        public double finalPrice;
    }

    public static class StoreMetadata {
        public final String storeId;
        public final String storeName;
        public final String storeUrl;

        public StoreMetadata(String storeId, String storeName, String storeUrl) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.storeUrl = storeUrl;
        }
    }
}
