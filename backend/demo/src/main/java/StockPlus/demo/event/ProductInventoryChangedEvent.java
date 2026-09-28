package StockPlus.demo.event;

import java.time.LocalDateTime;

public class ProductInventoryChangedEvent {

    private final Long productId;
    private final Integer previousStock;
    private final Integer newStock;
    private final Double demandVelocity;
    private final LocalDateTime timestamp;

    public ProductInventoryChangedEvent(Long productId, Integer previousStock, Integer newStock, Double demandVelocity) {
        this.productId = productId;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.demandVelocity = demandVelocity;
        this.timestamp = LocalDateTime.now();
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public Double getDemandVelocity() {
        return demandVelocity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
