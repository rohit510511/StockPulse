package StockPlus.demo.dto;

import StockPlus.demo.model.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSimulationResponse {

    private Long productId;
    private String sku;
    private String productName;
    private Integer quantityOrdered;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Integer remainingStock;
    private Double newDemandVelocity;
    private ProductStatus productStatus;
    private LocalDateTime timestamp;

    public OrderSimulationResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public OrderSimulationResponse(Long productId, String sku, String productName,
                                   Integer quantityOrdered, BigDecimal unitPrice,
                                   BigDecimal totalAmount, Integer remainingStock,
                                   Double newDemandVelocity, ProductStatus productStatus) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.quantityOrdered = quantityOrdered;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.remainingStock = remainingStock;
        this.newDemandVelocity = newDemandVelocity;
        this.productStatus = productStatus;
        this.timestamp = LocalDateTime.now();
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(Integer quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(Integer remainingStock) {
        this.remainingStock = remainingStock;
    }

    public Double getNewDemandVelocity() {
        return newDemandVelocity;
    }

    public void setNewDemandVelocity(Double newDemandVelocity) {
        this.newDemandVelocity = newDemandVelocity;
    }

    public ProductStatus getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(ProductStatus productStatus) {
        this.productStatus = productStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
