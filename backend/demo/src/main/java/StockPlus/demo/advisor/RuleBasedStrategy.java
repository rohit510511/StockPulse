package StockPlus.demo.advisor;

import StockPlus.demo.model.PriceDirection;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.SuggestionSource;
import StockPlus.demo.model.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ruleBasedStrategy")
public class RuleBasedStrategy implements CommerceAdvisor {

    @Override
    public CommerceRecommendation recommend(Product product, TriggerReason triggerReason, CategoryStats stats) {
        PricingRecommendation pricing = recommendPricing(product, triggerReason, stats);
        ReorderRecommendation reorder = recommendReorder(product, triggerReason, stats);
        return new CommerceRecommendation(pricing, reorder);
    }

    @Override
    public PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, CategoryStats stats) {
        BigDecimal currentPrice = product.getCurrentPrice();
        int stock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;
        double demandVelocity = product.getDemandVelocity() != null ? product.getDemandVelocity() : 0.0;
        double categoryAvg = stats != null ? stats.getAverageDemandVelocity() : 0.0;

        BigDecimal recommendedPrice;
        PriceDirection direction;
        double confidence;
        String reasoning;

        if (stock < threshold) {
            // Rule: if stock < reorderThreshold -> +10%
            recommendedPrice = currentPrice.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);
            direction = PriceDirection.INCREASE;
            confidence = 0.85;
            reasoning = String.format("Stock level (%d) is below reorder threshold (%d). Recommending 10%% price increase to protect remaining inventory and margins.",
                    stock, threshold);
        } else if (demandVelocity > 2.0 * categoryAvg && categoryAvg > 0) {
            // Rule: else if demandVelocity > 2 x category average -> +5%
            recommendedPrice = currentPrice.multiply(new BigDecimal("1.05")).setScale(2, RoundingMode.HALF_UP);
            direction = PriceDirection.INCREASE;
            confidence = 0.80;
            reasoning = String.format("Demand velocity (%.2f) exceeds 2x category average (%.2f). Recommending 5%% price increase to capitalize on strong demand momentum.",
                    demandVelocity, categoryAvg);
        } else {
            // Rule: else HOLD
            recommendedPrice = currentPrice.setScale(2, RoundingMode.HALF_UP);
            direction = PriceDirection.HOLD;
            confidence = 0.90;
            reasoning = String.format("Stock level (%d) and demand velocity (%.2f) are stable relative to category average (%.2f). Recommending price hold.",
                    stock, demandVelocity, categoryAvg);
        }

        return new PricingRecommendation(recommendedPrice, direction, confidence, reasoning, SuggestionSource.RULE_BASED);
    }

    @Override
    public ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, CategoryStats stats) {
        int stock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;

        // Rule: max(1, reorderThreshold * 3 - currentStock)
        int recommendedQuantity = Math.max(1, (threshold * 3) - stock);
        int leadTimeDays = 7;
        double confidence = 0.85;
        String reasoning = String.format("Replenishing stock to reach target inventory level of 3x reorder threshold (%d) from current stock (%d). Recommended reorder: %d units.",
                threshold * 3, stock, recommendedQuantity);

        return new ReorderRecommendation(recommendedQuantity, leadTimeDays, confidence, reasoning, SuggestionSource.RULE_BASED);
    }
}
