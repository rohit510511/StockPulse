package StockPlus.demo.advisor;

public class CommerceRecommendation {

    private final PricingRecommendation pricing;
    private final ReorderRecommendation reorder;

    public CommerceRecommendation(PricingRecommendation pricing, ReorderRecommendation reorder) {
        this.pricing = pricing;
        this.reorder = reorder;
    }

    public PricingRecommendation getPricing() {
        return pricing;
    }

    public ReorderRecommendation getReorder() {
        return reorder;
    }
}
