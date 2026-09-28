package StockPlus.demo.dto;

import StockPlus.demo.model.*;

import java.time.LocalDateTime;

public class ReorderSuggestionResponse {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Integer currentStock;
    private Integer recommendedQuantity;
    private Integer suggestedLeadTimeDays;
    private Double confidence;
    private String reasoning;
    private SuggestionStatus status;
    private TriggerReason triggerReason;
    private SuggestionSource source;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public ReorderSuggestionResponse() {
    }

    public static ReorderSuggestionResponse fromEntity(ReorderSuggestion s) {
        if (s == null) {
            return null;
        }
        ReorderSuggestionResponse dto = new ReorderSuggestionResponse();
        dto.setId(s.getId());
        if (s.getProduct() != null) {
            dto.setProductId(s.getProduct().getId());
            dto.setProductSku(s.getProduct().getSku());
            dto.setProductName(s.getProduct().getName());
        }
        dto.setCurrentStock(s.getCurrentStock());
        dto.setRecommendedQuantity(s.getRecommendedQuantity());
        dto.setSuggestedLeadTimeDays(s.getSuggestedLeadTimeDays());
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

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getRecommendedQuantity() {
        return recommendedQuantity;
    }

    public void setRecommendedQuantity(Integer recommendedQuantity) {
        this.recommendedQuantity = recommendedQuantity;
    }

    public Integer getSuggestedLeadTimeDays() {
        return suggestedLeadTimeDays;
    }

    public void setSuggestedLeadTimeDays(Integer suggestedLeadTimeDays) {
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
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
