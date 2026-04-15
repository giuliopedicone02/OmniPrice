package com.unict.dmi.omniprice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public List<ProductDTO> getProducts() {
        try {
            // Percorso relativo dalla cartella omniprice al dataset
            // omniprice è in backend/omniprice
            // dataset è in backend/dataset
            // quindi servono due cartelle indietro (..) poi entra in dataset
            Path filePath = Paths.get("../../dataset/product-catalog.json").toAbsolutePath();

            System.out.println("📂 Cercando file in: " + filePath);

            // Legge il contenuto del file
            String content = Files.readString(filePath);

            // Se il JSON inizia con { è un oggetto, estrai la proprietà che contiene
            // l'array
            if (content.trim().startsWith("{")) {
                // Il JSON è un oggetto, assumiamo che i prodotti siano in una proprietà
                // "products"
                var jsonObject = objectMapper.readValue(content, java.util.Map.class);

                // Prova con diversi nomi di proprietà comuni
                List<ProductDTO> products = null;

                if (jsonObject.containsKey("products")) {
                    products = objectMapper.convertValue(
                            jsonObject.get("products"),
                            new TypeReference<List<ProductDTO>>() {
                            });
                } else if (jsonObject.containsKey("data")) {
                    products = objectMapper.convertValue(
                            jsonObject.get("data"),
                            new TypeReference<List<ProductDTO>>() {
                            });
                } else if (jsonObject.containsKey("items")) {
                    products = objectMapper.convertValue(
                            jsonObject.get("items"),
                            new TypeReference<List<ProductDTO>>() {
                            });
                } else {
                    // Se non trovi la proprietà, stampa le chiavi disponibili
                    System.err.println("❌ Proprietà non trovata. Chiavi disponibili: " + jsonObject.keySet());
                    return Collections.emptyList();
                }

                System.out.println("✓ Caricati " + products.size() + " prodotti dal JSON");
                return products;

            } else if (content.trim().startsWith("[")) {
                // Il JSON è direttamente un array
                List<ProductDTO> products = objectMapper.readValue(content,
                        new TypeReference<List<ProductDTO>>() {
                        });
                System.out.println("✓ Caricati " + products.size() + " prodotti dal JSON");
                return products;
            }

            System.err.println("❌ Formato JSON non supportato");
            return Collections.emptyList();

        } catch (IOException e) {
            System.err.println("❌ ERRORE nella lettura del JSON: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}