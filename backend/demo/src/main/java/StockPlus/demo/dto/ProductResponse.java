package StockPlus.demo.dto;

import StockPlus.demo.model.Category;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private Category category;
    private BigDecimal currentPrice;
    private Integer stockLevel;
    private Integer reorderThreshold;
    private Double demandVelocity;
    private ProductStatus status;
    private BigDecimal costPrice;
    private String supplierId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse() {
    }

    public static ProductResponse fromEntity(Product p) {
        if (p == null) {
            return null;
        }
        ProductResponse dto = new ProductResponse();
        dto.setId(p.getId());
        dto.setSku(p.getSku());
        dto.setName(p.getName());
        dto.setCategory(p.getCategory());
        dto.setCurrentPrice(p.getCurrentPrice());
        dto.setStockLevel(p.getStockLevel());
        dto.setReorderThreshold(p.getReorderThreshold());
        dto.setDemandVelocity(p.getDemandVelocity());
        dto.setStatus(p.getStatus());
        dto.setCostPrice(p.getCostPrice());
        dto.setSupplierId(p.getSupplierId());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
