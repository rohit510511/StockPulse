package StockPlus.demo.controller;

import StockPlus.demo.advisor.StrategyType;
import StockPlus.demo.dto.*;
import StockPlus.demo.model.*;
import StockPlus.demo.repository.PricingSuggestionRepository;
import StockPlus.demo.repository.ProductRepository;
import StockPlus.demo.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase2ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        pricingSuggestionRepository.deleteAll();
        reorderSuggestionRepository.deleteAll();
        productRepository.deleteAll();

        Product product = new Product("TEST-SKU-PHASE2", "Phase 2 Item", Category.APPAREL,
                new BigDecimal("50.00"), 10, 15, 8.0);
        testProduct = productRepository.save(product);
    }

    @Test
    void updateStock_updatesStockLevelAndRecalculatesStatus() throws Exception {
        StockUpdateRequest request = new StockUpdateRequest(25);

        mockMvc.perform(patch("/products/" + testProduct.getId() + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockLevel", is(25)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(25, updated.getStockLevel());
    }

    @Test
    void simulateOrder_decrementsStockAndBumpsVelocity() throws Exception {
        OrderSimulationRequest request = new OrderSimulationRequest(3);

        mockMvc.perform(post("/products/" + testProduct.getId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityOrdered", is(3)))
                .andExpect(jsonPath("$.remainingStock", is(7)))
                .andExpect(jsonPath("$.newDemandVelocity", is(13.0)))
                .andExpect(jsonPath("$.totalAmount", is(150.00)));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(7, updated.getStockLevel());
        assertEquals(13.0, updated.getDemandVelocity());
    }

    @Test
    void simulateOrder_insufficientStock_returnsConflict409() throws Exception {
        OrderSimulationRequest request = new OrderSimulationRequest(20); // only 10 in stock

        mockMvc.perform(post("/products/" + testProduct.getId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    @Test
    void suggestPricing_createsPendingSuggestionAndSetsReviewPendingStatus() throws Exception {
        mockMvc.perform(post("/products/" + testProduct.getId() + "/suggest-pricing"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.productId", is(testProduct.getId().intValue())))
                .andExpect(jsonPath("$.triggerReason", is("MANUAL")))
                .andExpect(jsonPath("$.source", is("RULE_BASED")));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(ProductStatus.PRICE_REVIEW_PENDING, updated.getStatus());
    }

    @Test
    void suggestReorder_createsPendingReorderSuggestion() throws Exception {
        mockMvc.perform(post("/products/" + testProduct.getId() + "/suggest-reorder"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.recommendedQuantity", greaterThan(0)));
    }

    @Test
    void acceptPricingSuggestion_updatesPriceAndResolvesStatus() throws Exception {
        // Generate suggestion
        MvcResult genResult = mockMvc.perform(post("/products/" + testProduct.getId() + "/suggest-pricing"))
                .andExpect(status().isCreated())
                .andReturn();

        PricingSuggestionResponse genDto = objectMapper.readValue(
                genResult.getResponse().getContentAsString(), PricingSuggestionResponse.class);

        // Accept suggestion
        SuggestionResolutionRequest acceptReq = new SuggestionResolutionRequest(SuggestionStatus.ACCEPTED);
        mockMvc.perform(patch("/pricing-suggestions/" + genDto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, genDto.getRecommendedPrice().compareTo(updated.getCurrentPrice()));
        assertEquals(ProductStatus.ACTIVE, updated.getStatus());

        // Attempting to accept again should return 409 Conflict
        mockMvc.perform(patch("/pricing-suggestions/" + genDto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void acceptReorderSuggestion_updatesStockLevel() throws Exception {
        // Generate reorder suggestion
        MvcResult genResult = mockMvc.perform(post("/products/" + testProduct.getId() + "/suggest-reorder"))
                .andExpect(status().isCreated())
                .andReturn();

        ReorderSuggestionResponse genDto = objectMapper.readValue(
                genResult.getResponse().getContentAsString(), ReorderSuggestionResponse.class);

        int initialStock = testProduct.getStockLevel();

        // Accept suggestion
        SuggestionResolutionRequest acceptReq = new SuggestionResolutionRequest(SuggestionStatus.ACCEPTED);
        mockMvc.perform(patch("/reorder-suggestions/" + genDto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(initialStock + genDto.getRecommendedQuantity(), updated.getStockLevel());
    }

    @Test
    void adminStrategy_getAndSetStrategy() throws Exception {
        mockMvc.perform(get("/admin/strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", notNullValue()));

        StrategyRequest putReq = new StrategyRequest(StrategyType.AI);
        mockMvc.perform(put("/admin/strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("AI")));

        // Switch back to RULE_BASED
        putReq.setStrategy(StrategyType.RULE_BASED);
        mockMvc.perform(put("/admin/strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(putReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", is("RULE_BASED")));
    }
}
