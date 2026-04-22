package com.unict.dmi.omniprice.service;

import com.unict.dmi.omniprice.dto.AlertDTO;
import com.unict.dmi.omniprice.model.Alert;
import com.unict.dmi.omniprice.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servizio per la gestione degli alert di prezzo.
 * Pattern: Session State - mantiene la configurazione degli alert utente persistita in DB.
 */
@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public AlertDTO createAlert(String userId, String productId, String productName, Double targetPrice) {
        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setProductId(productId);
        alert.setProductName(productName);
        alert.setTargetPrice(targetPrice);
        alert.setStatus("ACTIVE");
        return toDTO(alertRepository.save(alert));
    }

    public List<AlertDTO> getUserAlerts(String userId) {
        return alertRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getActiveAlerts() {
        return alertRepository.findByStatus("ACTIVE").stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<AlertDTO> cancelAlert(String alertId, String userId) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty() || !alertOpt.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        Alert alert = alertOpt.get();
        alert.setStatus("CANCELLED");
        return Optional.of(toDTO(alertRepository.save(alert)));
    }

    /**
     * Chiamato dallo scheduler quando un prezzo scende sotto la soglia target.
     */
    public void triggerAlert(String alertId, String storeName, double triggeredPrice) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            if ("ACTIVE".equals(alert.getStatus())) {
                alert.setStatus("TRIGGERED");
                alert.setTriggeredAt(LocalDateTime.now());
                alert.setTriggeredStore(storeName);
                alert.setTriggeredPrice(triggeredPrice);
                alertRepository.save(alert);
            }
        });
    }

    private AlertDTO toDTO(Alert alert) {
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setProductId(alert.getProductId());
        dto.setProductName(alert.getProductName());
        dto.setTargetPrice(alert.getTargetPrice());
        dto.setStatus(alert.getStatus());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setTriggeredAt(alert.getTriggeredAt());
        dto.setTriggeredStore(alert.getTriggeredStore());
        dto.setTriggeredPrice(alert.getTriggeredPrice());
        return dto;
    }
}
