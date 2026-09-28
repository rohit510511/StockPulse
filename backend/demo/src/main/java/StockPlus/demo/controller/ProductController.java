package StockPlus.demo.controller;

import StockPlus.demo.dto.*;
import StockPlus.demo.model.Category;
import StockPlus.demo.model.ProductStatus;
import StockPlus.demo.model.TriggerReason;
import StockPlus.demo.service.ProductService;
import StockPlus.demo.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final RecommendationService recommendationService;

    public ProductController(ProductService productService,
                             RecommendationService recommendationService) {
        this.productService = productService;
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        List<ProductResponse> products = productService.getProducts(status, category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {
        ProductResponse updated = productService.updateStock(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<OrderSimulationResponse> simulateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderSimulationRequest request) {
        OrderSimulationResponse orderResponse = productService.simulateOrder(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<PricingSuggestionResponse> suggestPricing(@PathVariable Long id) {
        PricingSuggestionResponse suggestion = recommendationService.generatePricingSuggestion(id, TriggerReason.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(suggestion);
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<ReorderSuggestionResponse> suggestReorder(@PathVariable Long id) {
        ReorderSuggestionResponse suggestion = recommendationService.generateReorderSuggestion(id, TriggerReason.MANUAL);
        return ResponseEntity.status(HttpStatus.CREATED).body(suggestion);
    }
}
