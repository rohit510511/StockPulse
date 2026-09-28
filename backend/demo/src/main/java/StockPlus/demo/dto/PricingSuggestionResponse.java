package StockPlus.demo.dto;

import StockPlus.demo.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PricingSuggestionResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal currentPrice;
    private BigDecimal recommendedPrice;
    private PriceDirection direction;
    private Double confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;
    private SuggestionSource source;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public PricingSuggestionResponse() {
    }

    public static PricingSuggestionResponse fromEntity(PricingSuggestion s) {
        if (s == null) {
            return null;
        }
        PricingSuggestionResponse dto = new PricingSuggestionResponse();
        dto.setId(s.getId());
        if (s.getProduct() != null) {
            dto.setProductId(s.getProduct().getId());
            dto.setProductSku(s.getProduct().getSku());
            dto.setProductName(s.getProduct().getName());
        }
        dto.setCurrentPrice(s.getCurrentPrice());
        dto.setRecommendedPrice(s.getRecommendedPrice());
        dto.setDirection(s.getDirection());
        dto.setConfidence(s.getConfidence());
        dto.setReasoning(s.getReasoning());
        dto.setStatus(s.getStatus());
        dto.setTriggerReason(s.getTriggerReason());
        dto.setSource(s.getSource());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setResolvedAt(s.getResolvedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getRecommendedPrice() {
        return recommendedPrice;
    }

    public void setRecommendedPrice(BigDecimal recommendedPrice) {
        this.recommendedPrice = recommendedPrice;
    }

    public PriceDirection getDirection() {
        return direction;
    }

    public void setDirection(PriceDirection direction) {
        this.direction = direction;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public TriggerReason getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(TriggerReason triggerReason) {
        this.triggerReason = triggerReason;
    }

    public SuggestionSource getSource() {
        return source;
    }

    public void setSource(SuggestionSource source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
