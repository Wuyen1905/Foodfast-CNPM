package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_option_groups")
public class ProductOptionGroup {

    @Id
    @Column(length = 80)
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false, length = 20)
    private ProductOptionSelectionType selectionType = ProductOptionSelectionType.SINGLE;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "min_selections", nullable = false)
    private int minSelections;

    @Column(name = "max_selections", nullable = false)
    private int maxSelections = 1;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC, name ASC")
    private List<ProductOption> options = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductId() { return product == null ? null : product.getId(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProductOptionSelectionType getSelectionType() { return selectionType; }
    public void setSelectionType(ProductOptionSelectionType selectionType) { this.selectionType = selectionType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public int getMinSelections() { return minSelections; }
    public void setMinSelections(int minSelections) { this.minSelections = Math.max(0, minSelections); }
    public int getMaxSelections() { return maxSelections; }
    public void setMaxSelections(int maxSelections) { this.maxSelections = Math.max(1, maxSelections); }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<ProductOption> getOptions() { return options; }
    public void setOptions(List<ProductOption> options) {
        this.options.clear();
        if (options != null) {
            options.forEach(this::addOption);
        }
    }
    public void addOption(ProductOption option) {
        option.setGroup(this);
        this.options.add(option);
    }
}
