package StockPlus.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reorder_suggestions", indexes = {
    @Index(name = "idx_reorder_sugg_product", columnList = "product_id"),
    @Index(name = "idx_reorder_sugg_status", columnList = "status")
})
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Column(name = "recommended_quantity", nullable = false)
    private Integer recommendedQuantity;

    @Column(name = "suggested_lead_time_days", nullable = false)
    private Integer suggestedLeadTimeDays;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false, length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 32)
    private TriggerReason triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SuggestionSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public ReorderSuggestion() {
    }

    public ReorderSuggestion(Product product, Integer currentStock, Integer recommendedQuantity,
                             Integer suggestedLeadTimeDays, Double confidence, String reasoning,
                             TriggerReason triggerReason, SuggestionSource source) {
        this.product = product;
        this.currentStock = currentStock;
        this.recommendedQuantity = recommendedQuantity;
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.triggerReason = triggerReason;
        this.source = source;
        this.status = SuggestionStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = SuggestionStatus.PENDING;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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
