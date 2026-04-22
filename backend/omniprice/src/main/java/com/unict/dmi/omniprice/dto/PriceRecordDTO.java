package com.unict.dmi.omniprice.dto;

/**
 * DTO per un singolo record del storico prezzi.
 */
public class PriceRecordDTO {

    private String date;
    private String storeId;
    private String storeName;
    private double price;
    private double discount;
    private double finalPrice;

    public PriceRecordDTO() {}

    public PriceRecordDTO(String date, String storeId, String storeName,
                          double price, double discount, double finalPrice) {
        this.date = date;
        this.storeId = storeId;
        this.storeName = storeName;
        this.price = price;
        this.discount = discount;
        this.finalPrice = finalPrice;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
}
