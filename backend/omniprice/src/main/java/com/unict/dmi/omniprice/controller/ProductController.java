package com.unict.dmi.omniprice.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unict.dmi.omniprice.dto.PriceDTO;
import com.unict.dmi.omniprice.dto.ProductDTO;

@RestController
@RequestMapping("/products") // Ricorda: Spring aggiunge /api da solo!
public class ProductController {

    // GET /api/products/search?q=laptop
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        // TODO: In futuro useremo CompletableFuture per chiamare i 3 microservizi degli
        // store

        // --- INIZIO DATI MOCK ---
        List<ProductDTO> results = new ArrayList<>();

        if (!query.isBlank()) {
            ProductDTO laptop = new ProductDTO();
            laptop.setId("prod_001");
            laptop.setName("Dell XPS 13 - " + query);
            laptop.setCategory("Computers");

            List<PriceDTO> prices = new ArrayList<>();
            prices.add(new PriceDTO("Amazon", 1299.99, "EUR", "https://amazon.com/..."));
            prices.add(new PriceDTO("eBay", 1199.99, "EUR", "https://ebay.com/..."));
            laptop.setPrices(prices);

            laptop.setMinPrice(1199.99);
            laptop.setMaxPrice(1299.99);
            laptop.setAvgPrice(1249.99);

            results.add(laptop);
        }
        // --- FINE DATI MOCK ---

        // Assembliamo la risposta nel formato esatto richiesto dalla documentazione
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("results", results);
        response.put("totalResults", results.size());
        response.put("page", page);
        response.put("hasMore", false);

        return ResponseEntity.ok(response);
    }
}