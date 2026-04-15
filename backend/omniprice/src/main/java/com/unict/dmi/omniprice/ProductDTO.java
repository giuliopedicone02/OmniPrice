package com.unict.dmi.omniprice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Questa annotazione evita il crash se il JSON contiene campi extra (come "description")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO {
    private String id;
    private String name;
    private String category;
    private String subcategory;

    // Costruttore vuoto
    public ProductDTO() {
    }

    // Costruttore con parametri
    public ProductDTO(String id, String name, String category, String subcategory) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.subcategory = subcategory;
    }

    // Getter e Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    @Override
    public String toString() {
        // Ho aggiunto gli apici singoli ('') attorno all'id visto che è una Stringa
        return "ProductDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                '}';
    }
}