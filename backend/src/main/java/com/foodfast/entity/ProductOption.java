package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_options")
public class ProductOption {

    @Id
    @Column(length = 80)
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ProductOptionGroup group;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "price_delta", nullable = false)
    private int priceDelta;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ProductOptionGroup getGroup() { return group; }
    public void setGroup(ProductOptionGroup group) { this.group = group; }
    public String getGroupId() { return group == null ? null : group.getId(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPriceDelta() { return priceDelta; }
    public void setPriceDelta(int priceDelta) { this.priceDelta = Math.max(0, priceDelta); }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
