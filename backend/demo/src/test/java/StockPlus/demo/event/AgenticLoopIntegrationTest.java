package StockPlus.demo.event;

import StockPlus.demo.dto.OrderSimulationRequest;
import StockPlus.demo.model.Category;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.SuggestionStatus;
import StockPlus.demo.model.TriggerReason;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgenticLoopIntegrationTest {

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

        // Stock 10, Threshold 8. An order of 4 drops stock to 6 (< 8) triggering INVENTORY_LOW
        Product product = new Product("SKU-ASYNC-LOOP", "Async Item", Category.ELECTRONICS,
                new BigDecimal("80.00"), 10, 8, 3.0);
        testProduct = productRepository.save(product);
    }

    @Test
    void simulateOrder_triggersAsyncAgenticLoop_suggestionsAppearInDatabase() throws Exception {
        OrderSimulationRequest request = new OrderSimulationRequest(4);

        // Synchronous HTTP call returns immediately with 201 Created
        mockMvc.perform(post("/products/" + testProduct.getId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Await asynchronously generated suggestions via AFTER_COMMIT listener
        await().atMost(Duration.ofSeconds(6))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                        testProduct.getId(), TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING));

        assertFalse(pricingSuggestionRepository
                .findByProductIdAndStatus(testProduct.getId(), SuggestionStatus.PENDING).isEmpty());
        assertFalse(reorderSuggestionRepository
                .findByProductIdAndStatus(testProduct.getId(), SuggestionStatus.PENDING).isEmpty());
    }
}
