package com.unict.dmi.omniprice.dto;

public class PriceDTO {
    private String store;
    private Double price;
    private String currency;
    private String url;

    // Costruttore vuoto
    public PriceDTO() {
    }

    // Costruttore con parametri
    public PriceDTO(String store, Double price, String currency, String url) {
        this.store = store;
        this.price = price;
        this.currency = currency;
        this.url = url;
    }

    // Getter e Setter
    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}