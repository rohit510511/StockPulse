package StockPlus.demo.advisor;

import StockPlus.demo.llm.LlmClient;
import StockPlus.demo.model.PriceDirection;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.SuggestionSource;
import StockPlus.demo.model.TriggerReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("aiCommerceStrategy")
public class AICommerceStrategy implements CommerceAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AICommerceStrategy.class);

    private final LlmClient llmClient;
    private final RuleBasedStrategy ruleBasedStrategy;
    private final ObjectMapper objectMapper;

    public AICommerceStrategy(LlmClient llmClient,
                              RuleBasedStrategy ruleBasedStrategy,
                              ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.ruleBasedStrategy = ruleBasedStrategy;
        this.objectMapper = objectMapper;
    }

    @Override
    public CommerceRecommendation recommend(Product product, TriggerReason triggerReason, CategoryStats stats) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(product, triggerReason, stats);

            String rawResponse = llmClient.chatCompletion(systemPrompt, userPrompt);
            return parseAndValidate(rawResponse, product, triggerReason);
        } catch (Throwable t) {
            log.warn("AI recommendation failed for product SKU [{}]. Falling back to RuleBasedStrategy: {}",
                    product != null ? product.getSku() : "unknown", t.getMessage());
            return executeFallback(product, triggerReason, stats, t.getMessage());
        }
    }

    @Override
    public PricingRecommendation recommendPricing(Product product, TriggerReason triggerReason, CategoryStats stats) {
        return recommend(product, triggerReason, stats).getPricing();
    }

    @Override
    public ReorderRecommendation recommendReorder(Product product, TriggerReason triggerReason, CategoryStats stats) {
        return recommend(product, triggerReason, stats).getReorder();
    }

    private String buildSystemPrompt() {
        return "You are StockPulse AI Commerce Advisor — an expert inventory optimization and dynamic pricing engine. " +
                "Analyze the provided product metrics, category benchmark, and trigger context to output an optimal pricing adjustment and replenishment reorder quantity. " +
                "You MUST return ONLY a single valid JSON object with NO surrounding markdown formatting, code fences, or explanation. " +
                "The response MUST strictly match this JSON schema:\n" +
                "{\n" +
                "  \"pricing\": {\n" +
                "    \"recommendedPrice\": 129.99,\n" +
                "    \"direction\": \"INCREASE\" | \"DECREASE\" | \"HOLD\",\n" +
                "    \"confidence\": 0.85,\n" +
                "    \"reasoning\": \"Short 1-2 sentence justification\"\n" +
                "  },\n" +
                "  \"reorder\": {\n" +
                "    \"recommendedQuantity\": 25,\n" +
                "    \"suggestedLeadTimeDays\": 7,\n" +
                "    \"confidence\": 0.90,\n" +
                "    \"reasoning\": \"Short 1-2 sentence justification\"\n" +
                "  }\n" +
                "}";
    }

    private String buildUserPrompt(Product product, TriggerReason triggerReason, CategoryStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("Product Details:\n");
        sb.append("- Name: ").append(product.getName()).append("\n");
        sb.append("- SKU: ").append(product.getSku()).append("\n");
        sb.append("- Category: ").append(product.getCategory()).append("\n");
        sb.append("- Current Price: $").append(product.getCurrentPrice()).append("\n");
        sb.append("- Current Stock: ").append(product.getStockLevel()).append(" units\n");
        sb.append("- Reorder Threshold: ").append(product.getReorderThreshold()).append(" units\n");
        sb.append("- Demand Velocity: ").append(product.getDemandVelocity()).append(" units/day\n");
        sb.append("- Category Avg Demand Velocity: ").append(String.format("%.2f", stats != null ? stats.getAverageDemandVelocity() : 0.0)).append(" units/day\n");
        sb.append("- Trigger Reason: ").append(triggerReason).append("\n\n");

        sb.append("Trigger Guidance:\n");
        if (triggerReason == TriggerReason.INVENTORY_LOW) {
            sb.append("INVENTORY_LOW triggered: Current stock is below reorder threshold. Prioritize stock protection via a modest price increase (+5% to +15%) while ordering sufficient units to restore safety stock (target 3x reorder threshold).\n");
        } else if (triggerReason == TriggerReason.DEMAND_SPIKE) {
            sb.append("DEMAND_SPIKE triggered: Demand velocity significantly outpaces category average. Recommend a moderate price increase (+3% to +10%) to capture margin and adjust reorder quantity to prevent premature stockouts.\n");
        } else {
            sb.append("MANUAL / GENERAL review: Balance margin optimization and inventory turnover. Hold price if stable, or make gentle adjustments.\n");
        }

        return sb.toString();
    }

    private CommerceRecommendation parseAndValidate(String rawResponse, Product product, TriggerReason triggerReason) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalArgumentException("Raw LLM response was empty");
        }

        String cleanedJson = extractJson(rawResponse);

        try {
            JsonNode root = objectMapper.readTree(cleanedJson);
            if (!root.has("pricing") || !root.has("reorder")) {
                throw new IllegalArgumentException("JSON missing 'pricing' or 'reorder' root keys");
            }

            // 1. Parse & Validate Pricing
            JsonNode pricingNode = root.get("pricing");
            double recPriceVal = pricingNode.get("recommendedPrice").asDouble();
            BigDecimal recommendedPrice = BigDecimal.valueOf(recPriceVal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentPrice = product.getCurrentPrice();

            // Validate price > 0 and within 50% - 200% of current price
            if (recommendedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Recommended price must be > 0. Got: " + recommendedPrice);
            }
            BigDecimal minBound = currentPrice.multiply(BigDecimal.valueOf(0.50));
            BigDecimal maxBound = currentPrice.multiply(BigDecimal.valueOf(2.00));
            if (recommendedPrice.compareTo(minBound) < 0 || recommendedPrice.compareTo(maxBound) > 0) {
                throw new IllegalArgumentException(String.format("Recommended price %s is outside 50%%-200%% bounds [%s, %s]",
                        recommendedPrice, minBound, maxBound));
            }

            // Direction consistency check
            PriceDirection direction;
            if (recommendedPrice.compareTo(currentPrice) > 0) {
                direction = PriceDirection.INCREASE;
            } else if (recommendedPrice.compareTo(currentPrice) < 0) {
                direction = PriceDirection.DECREASE;
            } else {
                direction = PriceDirection.HOLD;
            }

            double pricingConfidence = pricingNode.has("confidence") ? pricingNode.get("confidence").asDouble() : 0.85;
            pricingConfidence = Math.max(0.0, Math.min(1.0, pricingConfidence));
            String pricingReasoning = pricingNode.has("reasoning") ? pricingNode.get("reasoning").asText() : "AI price recommendation";

            PricingRecommendation pricingRec = new PricingRecommendation(
                    recommendedPrice, direction, pricingConfidence, pricingReasoning, SuggestionSource.AI);

            // 2. Parse & Validate Reorder
            JsonNode reorderNode = root.get("reorder");
            int recommendedQuantity = reorderNode.get("recommendedQuantity").asInt();
            if (recommendedQuantity <= 0) {
                throw new IllegalArgumentException("Recommended reorder quantity must be > 0. Got: " + recommendedQuantity);
            }

            int leadTime = reorderNode.has("suggestedLeadTimeDays") ? reorderNode.get("suggestedLeadTimeDays").asInt() : 7;
            if (leadTime <= 0) {
                leadTime = 7;
            }

            double reorderConfidence = reorderNode.has("confidence") ? reorderNode.get("confidence").asDouble() : 0.85;
            reorderConfidence = Math.max(0.0, Math.min(1.0, reorderConfidence));
            String reorderReasoning = reorderNode.has("reasoning") ? reorderNode.get("reasoning").asText() : "AI reorder recommendation";

            ReorderRecommendation reorderRec = new ReorderRecommendation(
                    recommendedQuantity, leadTime, reorderConfidence, reorderReasoning, SuggestionSource.AI);

            return new CommerceRecommendation(pricingRec, reorderRec);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse and validate AI response: " + e.getMessage(), e);
        }
    }

    private CommerceRecommendation executeFallback(Product product, TriggerReason triggerReason, CategoryStats stats, String errorReason) {
        CommerceRecommendation ruleRec = ruleBasedStrategy.recommend(product, triggerReason, stats);

        PricingRecommendation fallbackPricing = new PricingRecommendation(
                ruleRec.getPricing().getRecommendedPrice(),
                ruleRec.getPricing().getDirection(),
                ruleRec.getPricing().getConfidence(),
                ruleRec.getPricing().getReasoning() + " [Rule fallback due to AI error: " + errorReason + "]",
                SuggestionSource.RULE_FALLBACK
        );

        ReorderRecommendation fallbackReorder = new ReorderRecommendation(
                ruleRec.getReorder().getRecommendedQuantity(),
                ruleRec.getReorder().getSuggestedLeadTimeDays(),
                ruleRec.getReorder().getConfidence(),
                ruleRec.getReorder().getReasoning() + " [Rule fallback due to AI error: " + errorReason + "]",
                SuggestionSource.RULE_FALLBACK
        );

        return new CommerceRecommendation(fallbackPricing, fallbackReorder);
    }

    private String extractJson(String raw) {
        String text = raw.trim();
        // Remove markdown code fences if present
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

        // Extract substring between first '{' and last '}'
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }
}
