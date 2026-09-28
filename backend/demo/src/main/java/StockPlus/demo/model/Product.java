package StockPlus.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_sku", columnList = "sku", unique = true),
    @Index(name = "idx_products_category", columnList = "category"),
    @Index(name = "idx_products_status", columnList = "status")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;

    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold;

    @Column(name = "demand_velocity", nullable = false)
    private Double demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "supplier_id", length = 64)
    private String supplierId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(String sku, String name, Category category, BigDecimal currentPrice,
                   Integer stockLevel, Integer reorderThreshold, Double demandVelocity) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity != null ? demandVelocity : 0.0;
        this.status = (stockLevel != null && stockLevel <= 0) ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.demandVelocity == null) {
            this.demandVelocity = 0.0;
        }
        if (this.status == null) {
            this.status = (this.stockLevel != null && this.stockLevel <= 0)
                    ? ProductStatus.OUT_OF_STOCK
                    : ProductStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Recalculates product status based on business rules:
     * - OUT_OF_STOCK when stock hits 0
     * - PRICE_REVIEW_PENDING while a pending pricing suggestion exists
     * - back to ACTIVE when resolved and stock > 0
     */
    public void recalculateStatus(boolean hasPendingPricingSuggestion) {
        if (this.stockLevel == null || this.stockLevel <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else if (hasPendingPricingSuggestion) {
            this.status = ProductStatus.PRICE_REVIEW_PENDING;
        } else {
            this.status = ProductStatus.ACTIVE;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Integer getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(Integer stockLevel) {
        this.stockLevel = stockLevel;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public Double getDemandVelocity() {
        return demandVelocity;
    }

    public void setDemandVelocity(Double demandVelocity) {
        this.demandVelocity = demandVelocity;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
