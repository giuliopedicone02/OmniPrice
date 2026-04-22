package com.unict.dmi.omniprice.dto;

import java.util.List;

/**
 * DTO per storico prezzi completo.
 * Pattern: Serialized LOB - lo storico e' serializzato come oggetto complesso e restituito
 * solo su richiesta esplicita dell'utente (non incluso nella lista prodotti).
 */
public class PriceHistoryDTO {

    private String productId;
    private String productName;
    private List<PriceRecordDTO> records;

    public PriceHistoryDTO() {}

    public PriceHistoryDTO(String productId, String productName, List<PriceRecordDTO> records) {
        this.productId = productId;
        this.productName = productName;
        this.records = records;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public List<PriceRecordDTO> getRecords() { return records; }
    public void setRecords(List<PriceRecordDTO> records) { this.records = records; }
}
