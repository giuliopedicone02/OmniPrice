package com.unict.dmi.omniprice.controller;

import com.unict.dmi.omniprice.annotation.RequiresRole;
import com.unict.dmi.omniprice.dto.PriceHistoryDTO;
import com.unict.dmi.omniprice.service.DatasetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller per lo storico prezzi.
 *
 * Pattern: Serialized LOB (Serialized Large Object)
 * Lo storico dei prezzi e' un oggetto complesso (lista di record) che viene serializzato
 * e restituito SOLO su richiesta esplicita dell'utente (GET /price-history/{productId}).
 * Non viene incluso nella lista prodotti per evitare payload pesanti.
 * In produzione sarebbe stored come JSON blob nel DB e deserializzato on-demand.
 */
@RestController
@RequestMapping("/price-history")
public class PriceHistoryController {

    private final DatasetService datasetService;

    public PriceHistoryController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    // GET /api/price-history/{productId}
    // Carica lo storico SOLO quando richiesto esplicitamente (Serialized LOB)
    @GetMapping("/{productId}")
    @RequiresRole({"STANDARD", "PREMIUM", "ADMIN"})
    public ResponseEntity<PriceHistoryDTO> getPriceHistory(@PathVariable String productId) {
        Optional<PriceHistoryDTO> history = datasetService.getPriceHistory(productId);
        return history.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
