package com.unict.dmi.omniprice.controller;

import com.unict.dmi.omniprice.annotation.RequiresRole;
import com.unict.dmi.omniprice.dto.AlertDTO;
import com.unict.dmi.omniprice.model.User;
import com.unict.dmi.omniprice.repository.UserRepository;
import com.unict.dmi.omniprice.service.AlertService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller per la gestione degli alert di prezzo.
 * Pattern: Session State - ogni utente mantiene la propria lista di alert attivi.
 */
@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;
    private final UserRepository userRepository;

    public AlertController(AlertService alertService, UserRepository userRepository) {
        this.alertService = alertService;
        this.userRepository = userRepository;
    }

    // GET /api/alerts
    @GetMapping
    @RequiresRole({"STANDARD", "PREMIUM", "ADMIN"})
    public ResponseEntity<List<AlertDTO>> getUserAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(alertService.getUserAlerts(userId));
    }

    // POST /api/alerts
    @PostMapping
    @RequiresRole({"STANDARD", "PREMIUM", "ADMIN"})
    public ResponseEntity<AlertDTO> createAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {

        String userId = getUserId(userDetails);
        String productId = (String) request.get("productId");
        String productName = (String) request.get("productName");
        Double targetPrice = Double.parseDouble(request.get("targetPrice").toString());

        if (productId == null || productName == null || targetPrice == null) {
            return ResponseEntity.badRequest().build();
        }

        AlertDTO alert = alertService.createAlert(userId, productId, productName, targetPrice);
        return ResponseEntity.status(HttpStatus.CREATED).body(alert);
    }

    // DELETE /api/alerts/{id}
    @DeleteMapping("/{id}")
    @RequiresRole({"STANDARD", "PREMIUM", "ADMIN"})
    public ResponseEntity<AlertDTO> cancelAlert(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);
        Optional<AlertDTO> result = alertService.cancelAlert(id, userId);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    }
}
