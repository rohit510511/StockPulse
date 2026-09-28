package StockPlus.demo.advisor;

import StockPlus.demo.model.Product;
import StockPlus.demo.model.TriggerReason;

public interface CommerceAdvisor {

    /**
     * Generates a unified recommendation containing both pricing and reorder suggestions.
     */
    CommerceRecommendation recommend(Product product, TriggerReason triggerReason, CategoryStats stats);

    default PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, CategoryStats stats) {
        return recommend(product, triggerReason, stats).getPricing();
    }

    default ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, CategoryStats stats) {
        return recommend(product, triggerReason, stats).getReorder();
    }
}
