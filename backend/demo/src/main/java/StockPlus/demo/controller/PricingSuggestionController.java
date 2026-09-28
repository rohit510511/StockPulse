package StockPlus.demo.controller;

import StockPlus.demo.dto.PricingSuggestionResponse;
import StockPlus.demo.dto.SuggestionResolutionRequest;
import StockPlus.demo.model.SuggestionStatus;
import StockPlus.demo.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pricing-suggestions")
@CrossOrigin(origins = "*")
public class PricingSuggestionController {

    private final RecommendationService recommendationService;

    public PricingSuggestionController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<PricingSuggestionResponse>> getPricingSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        List<PricingSuggestionResponse> suggestions = recommendationService.getPricingSuggestions(status);
        return ResponseEntity.ok(suggestions);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PricingSuggestionResponse> resolvePricingSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionResolutionRequest request) {
        PricingSuggestionResponse updated = recommendationService.resolvePricingSuggestion(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
