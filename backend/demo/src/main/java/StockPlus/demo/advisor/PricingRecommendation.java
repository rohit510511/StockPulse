package StockPlus.demo.advisor;

import StockPlus.demo.model.PriceDirection;
import StockPlus.demo.model.SuggestionSource;

import java.math.BigDecimal;

public class PricingRecommendation {

    private final BigDecimal recommendedPrice;
    private final PriceDirection direction;
    private final Double confidence;
    private final String reasoning;
    private final SuggestionSource source;

    public PricingRecommendation(BigDecimal recommendedPrice, PriceDirection direction,
                                 Double confidence, String reasoning, SuggestionSource source) {
        this.recommendedPrice = recommendedPrice;
        this.direction = direction;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.source = source;
    }

    public BigDecimal getRecommendedPrice() {
        return recommendedPrice;
    }

    public PriceDirection getDirection() {
        return direction;
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
