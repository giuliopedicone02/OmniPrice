package com.unict.dmi.omniprice.controller;

import com.unict.dmi.omniprice.annotation.RequiresRole;
import com.unict.dmi.omniprice.dto.ProductDTO;
import com.unict.dmi.omniprice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller per la ricerca prodotti.
 * Pattern: Remote Facade - espone un'API unificata che aggrega 4 microservizi store.
 * Pattern: DTO - restituisce ProductDTO leggeri con informazioni aggregate.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/products/search?q=laptop&page=0&size=20
    @GetMapping("/search")
    @RequiresRole({"STANDARD", "PREMIUM", "ADMIN"})
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        List<ProductDTO> results = query.isBlank()
                ? List.of()
                : productService.searchProducts(query);

        // Paginazione
        int from = Math.min(page * size, results.size());
        int to = Math.min(from + size, results.size());
        List<ProductDTO> paged = results.subList(from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("results", paged);
        response.put("totalResults", results.size());
        response.put("page", page);
        response.put("hasMore", to < results.size());

        return ResponseEntity.ok(response);
    }

    // GET /api/products/{id}
    @GetMapping("/{id}")
    @RequiresRole({"STANDARD", "PREMIUM", "ADMIN"})
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String id) {
        Optional<ProductDTO> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
