package StockPlus.demo.controller;

import StockPlus.demo.dto.ProductCreateRequest;
import StockPlus.demo.model.Category;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

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

    @BeforeEach
    void setUp() {
        pricingSuggestionRepository.deleteAll();
        reorderSuggestionRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void createProduct_success() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-TEST-1",
                "Test Product",
                Category.ELECTRONICS,
                new BigDecimal("99.99"),
                50,
                10,
                5.0
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("SKU-TEST-1")))
                .andExpect(jsonPath("$.name", is("Test Product")))
                .andExpect(jsonPath("$.category", is("ELECTRONICS")))
                .andExpect(jsonPath("$.currentPrice", is(99.99)))
                .andExpect(jsonPath("$.stockLevel", is(50)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void createProduct_duplicateSku_returnsConflict() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-DUPE",
                "Test Product",
                Category.APPAREL,
                new BigDecimal("49.99"),
                20,
                5,
                2.0
        );

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("SKU-DUPE")));
    }

    @Test
    void createProduct_validationError_returnsBadRequest() throws Exception {
        ProductCreateRequest invalidRequest = new ProductCreateRequest();
        // Missing required fields

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void getProducts_withFilter() throws Exception {
        ProductCreateRequest p1 = new ProductCreateRequest("SKU-P1", "Shirt", Category.APPAREL, new BigDecimal("25.00"), 10, 5, 2.0);
        ProductCreateRequest p2 = new ProductCreateRequest("SKU-P2", "Laptop", Category.ELECTRONICS, new BigDecimal("1200.00"), 5, 2, 1.0);

        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p1)));
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p2)));

        mockMvc.perform(get("/products?category=APPAREL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku", is("SKU-P1")));
    }

    @Test
    void getProductById_notFound() throws Exception {
        mockMvc.perform(get("/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
