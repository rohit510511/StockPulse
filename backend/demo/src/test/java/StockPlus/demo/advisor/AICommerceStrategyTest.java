package StockPlus.demo.advisor;

import StockPlus.demo.llm.LlmClient;
import StockPlus.demo.model.Category;
import StockPlus.demo.model.PriceDirection;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.SuggestionSource;
import StockPlus.demo.model.TriggerReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AICommerceStrategyTest {

    private LlmClient mockLlmClient;
    private RuleBasedStrategy ruleBasedStrategy;
    private ObjectMapper objectMapper;
    private AICommerceStrategy strategy;

    private Product testProduct;
    private CategoryStats testStats;

    @BeforeEach
    void setUp() {
        mockLlmClient = Mockito.mock(LlmClient.class);
        ruleBasedStrategy = new RuleBasedStrategy();
        objectMapper = new ObjectMapper();
        strategy = new AICommerceStrategy(mockLlmClient, ruleBasedStrategy, objectMapper);

        testProduct = new Product("AI-TEST-1", "Smart Watch", Category.ELECTRONICS,
                new BigDecimal("100.00"), 5, 10, 15.0);
        testStats = new CategoryStats(5.0, 5);
    }

    @Test
    void recommend_successfulAIResponse_parsesCorrectly() {
        String validAiJson = """
                {
                  "pricing": {
                    "recommendedPrice": 115.00,
                    "direction": "INCREASE",
                    "confidence": 0.92,
                    "reasoning": "Strong demand velocity and constrained stock justify 15% increase"
                  },
                  "reorder": {
                    "recommendedQuantity": 25,
                    "suggestedLeadTimeDays": 5,
                    "confidence": 0.88,
                    "reasoning": "Targeting 30 units to satisfy run-rate"
                  }
                }
                """;

        when(mockLlmClient.chatCompletion(anyString(), anyString())).thenReturn(validAiJson);

        CommerceRecommendation rec = strategy.recommend(testProduct, TriggerReason.INVENTORY_LOW, testStats);

        assertNotNull(rec);
        assertEquals(SuggestionSource.AI, rec.getPricing().getSource());
        assertEquals(SuggestionSource.AI, rec.getReorder().getSource());
        assertEquals(0, new BigDecimal("115.00").compareTo(rec.getPricing().getRecommendedPrice()));
        assertEquals(PriceDirection.INCREASE, rec.getPricing().getDirection());
        assertEquals(0.92, rec.getPricing().getConfidence());
        assertEquals(25, rec.getReorder().getRecommendedQuantity());
        assertEquals(5, rec.getReorder().getSuggestedLeadTimeDays());
    }

    @Test
    void recommend_stripsMarkdownCodeFences() {
        String fencedJson = """
                ```json
                {
                  "pricing": {
                    "recommendedPrice": 105.00,
                    "direction": "INCREASE",
                    "confidence": 0.80,
                    "reasoning": "Fenced JSON response"
                  },
                  "reorder": {
                    "recommendedQuantity": 20,
                    "suggestedLeadTimeDays": 7,
                    "confidence": 0.85,
                    "reasoning": "Standard replenishment"
                  }
                }
                ```
                """;

        when(mockLlmClient.chatCompletion(anyString(), anyString())).thenReturn(fencedJson);

        CommerceRecommendation rec = strategy.recommend(testProduct, TriggerReason.DEMAND_SPIKE, testStats);

        assertEquals(SuggestionSource.AI, rec.getPricing().getSource());
        assertEquals(0, new BigDecimal("105.00").compareTo(rec.getPricing().getRecommendedPrice()));
    }

    @Test
    void recommend_toleratesSurroundingText() {
        String conversational = """
                Here is the optimization recommendation for your review:
                {
                  "pricing": {
                    "recommendedPrice": 110.00,
                    "direction": "INCREASE",
                    "confidence": 0.85,
                    "reasoning": "Extracted from text"
                  },
                  "reorder": {
                    "recommendedQuantity": 15,
                    "suggestedLeadTimeDays": 7,
                    "confidence": 0.80,
                    "reasoning": "Extracted"
                  }
                }
                Hope this helps!
                """;

        when(mockLlmClient.chatCompletion(anyString(), anyString())).thenReturn(conversational);

        CommerceRecommendation rec = strategy.recommend(testProduct, TriggerReason.MANUAL, testStats);

        assertEquals(SuggestionSource.AI, rec.getPricing().getSource());
        assertEquals(0, new BigDecimal("110.00").compareTo(rec.getPricing().getRecommendedPrice()));
    }

    @Test
    void recommend_onMalformedJson_fallsBackToRuleBased() {
        when(mockLlmClient.chatCompletion(anyString(), anyString())).thenReturn("I am unable to provide recommendations right now.");

        CommerceRecommendation rec = strategy.recommend(testProduct, TriggerReason.INVENTORY_LOW, testStats);

        assertNotNull(rec);
        assertEquals(SuggestionSource.RULE_FALLBACK, rec.getPricing().getSource());
        assertEquals(SuggestionSource.RULE_FALLBACK, rec.getReorder().getSource());
        // RuleBasedStrategy gives +10% (110.00) since stock 5 < threshold 10
        assertEquals(0, new BigDecimal("110.00").compareTo(rec.getPricing().getRecommendedPrice()));
        assertTrue(rec.getPricing().getReasoning().contains("Rule fallback"));
    }

    @Test
    void recommend_onPriceOutOfBounds_fallsBackToRuleBased() {
        // Current price 100.00, recommended 350.00 (>200% bound)
        String outOfBoundsJson = """
                {
                  "pricing": {
                    "recommendedPrice": 350.00,
                    "direction": "INCREASE",
                    "confidence": 0.99,
                    "reasoning": "Unreasonable price spike"
                  },
                  "reorder": {
                    "recommendedQuantity": 10,
                    "suggestedLeadTimeDays": 7,
                    "confidence": 0.90,
                    "reasoning": "Normal reorder"
                  }
                }
                """;

        when(mockLlmClient.chatCompletion(anyString(), anyString())).thenReturn(outOfBoundsJson);

        CommerceRecommendation rec = strategy.recommend(testProduct, TriggerReason.INVENTORY_LOW, testStats);

        assertEquals(SuggestionSource.RULE_FALLBACK, rec.getPricing().getSource());
        assertEquals(0, new BigDecimal("110.00").compareTo(rec.getPricing().getRecommendedPrice()));
    }

    @Test
    void recommend_onHttpExceptionOrTimeout_fallsBackToRuleBased() {
        when(mockLlmClient.chatCompletion(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connect timeout to litellm endpoint"));

        CommerceRecommendation rec = strategy.recommend(testProduct, TriggerReason.INVENTORY_LOW, testStats);

        assertNotNull(rec);
        assertEquals(SuggestionSource.RULE_FALLBACK, rec.getPricing().getSource());
        assertEquals(SuggestionSource.RULE_FALLBACK, rec.getReorder().getSource());
    }
}
