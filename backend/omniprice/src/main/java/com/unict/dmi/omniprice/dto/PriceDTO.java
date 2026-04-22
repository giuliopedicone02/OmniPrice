package com.unict.dmi.omniprice.dto;

/**
 * DTO per il prezzo di un prodotto in uno store specifico.
 * Pattern: Remote Facade - aggrega informazioni di store e prezzo senza esporre dettagli interni.
 */
public class PriceDTO {

    private String store;
    private String storeId;
    private double price;
    private double discount;
    private double finalPrice;
    private String currency;
    private String url;
    private String availability;
    private String estimatedDelivery;
    private double rating;
    private int reviews;

    public PriceDTO() {}

    public PriceDTO(String store, double price, String currency, String url) {
        this.store = store;
        this.price = price;
        this.finalPrice = price;
        this.currency = currency;
        this.url = url;
    }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviews() { return reviews; }
    public void setReviews(int reviews) { this.reviews = reviews; }
}
