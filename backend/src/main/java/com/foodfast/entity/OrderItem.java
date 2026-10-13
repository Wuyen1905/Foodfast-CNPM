package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(length = 40, name = "product_id")
    @JsonProperty("productId")
    private String productId;

    @Column(nullable = false)
    @JsonProperty("name")
    private String name;

    @Column(nullable = false)
    @JsonProperty("qty")
    private int qty;

    @Column(nullable = false)
    @JsonProperty("price")
    private int price;

    // Additional field for productName (for frontend compatibility)
    @Column(length = 200, name = "product_name")
    @JsonProperty("productName")
    private String productName;

    @Column(length = 2048, name = "product_image")
    @JsonProperty("productImage")
    private String productImage;

    @Column(name = "item_note", length = 200)
    private String itemNote;

    @Column(name = "original_product_id", length = 40)
    private String originalProductId;

    @Column(name = "original_product_name", length = 200)
    private String originalProductName;

    @Column(name = "substitution_proposal_id")
    private Long substitutionProposalId;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItemOption> selectedOptions = new ArrayList<>();

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    public OrderItem() {
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // JSON getter that returns id as String for frontend compatibility
    @JsonProperty("id")
    public String getIdAsString() {
        return id != null ? String.valueOf(id) : null;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = Math.max(price, 0);
    }
    
    public void setPrice(long p) {
        try {
            this.price = Math.toIntExact(p);
        } catch (Exception e) {
            this.price = 0;
        }
    }
    
    // Backward compatibility getter
    @JsonProperty("quantity")
    public int getQuantity() {
        return qty;
    }

    public void setQuantity(int quantity) {
        this.qty = quantity;
    }

    public String getProductName() {
        return productName != null ? productName : name;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public String getItemNote() { return itemNote; }
    public void setItemNote(String itemNote) { this.itemNote = itemNote; }
    public List<OrderItemOption> getSelectedOptions() { return selectedOptions; }
    public void setSelectedOptions(List<OrderItemOption> selectedOptions) {
        this.selectedOptions.clear();
        if (selectedOptions != null) selectedOptions.forEach(this::addSelectedOption);
    }
    public void addSelectedOption(OrderItemOption option) {
        option.setOrderItem(this);
        this.selectedOptions.add(option);
    }
    public String getOriginalProductId() { return originalProductId; }
    public void setOriginalProductId(String originalProductId) { this.originalProductId = originalProductId; }
    public String getOriginalProductName() { return originalProductName; }
    public void setOriginalProductName(String originalProductName) { this.originalProductName = originalProductName; }
    public Long getSubstitutionProposalId() { return substitutionProposalId; }
    public void setSubstitutionProposalId(Long substitutionProposalId) { this.substitutionProposalId = substitutionProposalId; }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public int lineTotal() {
        return price * qty;
    }
    
    @Override
    public String toString() {
        return "OrderItem{name=" + name + ", qty=" + qty + ", price=" + price + "}";
    }
}

