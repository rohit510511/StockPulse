package StockPlus.demo.advisor;

import StockPlus.demo.model.SuggestionSource;

public class ReorderRecommendation {

    private final Integer recommendedQuantity;
    private final Integer suggestedLeadTimeDays;
    private final Double confidence;
    private final String reasoning;
    private final SuggestionSource source;

    public ReorderRecommendation(Integer recommendedQuantity, Integer suggestedLeadTimeDays,
                                 Double confidence, String reasoning, SuggestionSource source) {
        this.recommendedQuantity = recommendedQuantity;
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.source = source;
    }

    public Integer getRecommendedQuantity() {
        return recommendedQuantity;
    }

    public Integer getSuggestedLeadTimeDays() {
        return suggestedLeadTimeDays;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public SuggestionSource getSource() {
        return source;
    }
}
