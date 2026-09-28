package StockPlus.demo.advisor;

import StockPlus.demo.model.Category;
import StockPlus.demo.model.PriceDirection;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.TriggerReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedStrategyTest {

    private RuleBasedStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RuleBasedStrategy();
    }

    @Test
    void recommendPricing_whenStockBelowThreshold_recommends10PercentIncrease() {
        // Stock 5 < threshold 10 -> +10% on price 100.00 => 110.00
        Product product = new Product("SKU-1", "Test Item", Category.ELECTRONICS,
                new BigDecimal("100.00"), 5, 10, 2.0);
        CategoryStats stats = new CategoryStats(5.0, 10);

        PricingRecommendation result = strategy.recommendPricing(product, TriggerReason.INVENTORY_LOW, stats);

        assertEquals(0, new BigDecimal("110.00").compareTo(result.getRecommendedPrice()));
        assertEquals(PriceDirection.INCREASE, result.getDirection());
        assertTrue(result.getReasoning().contains("below reorder threshold"));
    }

    @Test
    void recommendPricing_whenDemandVelocityExceeds2xCategoryAvg_recommends5PercentIncrease() {
        // Stock 20 >= threshold 10, Demand 15.0 > 2 * 6.0 (12.0) -> +5% on price 100.00 => 105.00
        Product product = new Product("SKU-2", "Trending Item", Category.APPAREL,
                new BigDecimal("100.00"), 20, 10, 15.0);
        CategoryStats stats = new CategoryStats(6.0, 10);

        PricingRecommendation result = strategy.recommendPricing(product, TriggerReason.DEMAND_SPIKE, stats);

        assertEquals(0, new BigDecimal("105.00").compareTo(result.getRecommendedPrice()));
        assertEquals(PriceDirection.INCREASE, result.getDirection());
        assertTrue(result.getReasoning().contains("exceeds 2x category average"));
    }

    @Test
    void recommendPricing_whenNormalDemandAndStock_recommendsHold() {
        // Stock 20 >= threshold 10, Demand 5.0 <= 2 * 6.0 -> HOLD
        Product product = new Product("SKU-3", "Standard Item", Category.HOME,
                new BigDecimal("50.00"), 20, 10, 5.0);
        CategoryStats stats = new CategoryStats(6.0, 10);

        PricingRecommendation result = strategy.recommendPricing(product, TriggerReason.MANUAL, stats);

        assertEquals(0, new BigDecimal("50.00").compareTo(result.getRecommendedPrice()));
        assertEquals(PriceDirection.HOLD, result.getDirection());
        assertTrue(result.getReasoning().contains("stable"));
    }

    @Test
    void recommendReorder_calculatesBufferQuantityProperly() {
        // Threshold 10 * 3 = 30. Current stock = 8. Recommended = 30 - 8 = 22.
        Product product = new Product("SKU-4", "Restock Item", Category.APPAREL,
                new BigDecimal("30.00"), 8, 10, 4.0);
        CategoryStats stats = new CategoryStats(4.0, 5);

        ReorderRecommendation result = strategy.recommendReorder(product, TriggerReason.INVENTORY_LOW, stats);

        assertEquals(22, result.getRecommendedQuantity());
        assertEquals(7, result.getSuggestedLeadTimeDays());
        assertTrue(result.getReasoning().contains("22 units"));
    }

    @Test
    void recommendReorder_whenStockAboveTarget_returnsMinimumOneUnit() {
        // Threshold 5 * 3 = 15. Current stock = 20. Max(1, 15 - 20) = 1.
        Product product = new Product("SKU-5", "Overstocked Item", Category.ELECTRONICS,
                new BigDecimal("30.00"), 20, 5, 2.0);
        CategoryStats stats = new CategoryStats(2.0, 5);

        ReorderRecommendation result = strategy.recommendReorder(product, TriggerReason.MANUAL, stats);

        assertEquals(1, result.getRecommendedQuantity());
    }
}
