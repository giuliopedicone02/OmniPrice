package com.unict.dmi.omniprice;

public class ProductDTO {
    private Long id;
    private String name;
    private String category;
    private String subcategory;

    // Costruttore vuoto
    public ProductDTO() {
    }

    // Costruttore con parametri
    public ProductDTO(Long id, String name, String category, String subcategory) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.subcategory = subcategory;
    }

    // Getter e Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
        return "ProductDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                '}';
    }
}