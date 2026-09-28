package StockPlus.demo.controller;

import StockPlus.demo.dto.ReorderSuggestionResponse;
import StockPlus.demo.dto.SuggestionResolutionRequest;
import StockPlus.demo.model.SuggestionStatus;
import StockPlus.demo.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reorder-suggestions")
@CrossOrigin(origins = "*")
public class ReorderSuggestionController {

    private final RecommendationService recommendationService;

    public ReorderSuggestionController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<ReorderSuggestionResponse>> getReorderSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        List<ReorderSuggestionResponse> suggestions = recommendationService.getReorderSuggestions(status);
        return ResponseEntity.ok(suggestions);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReorderSuggestionResponse> resolveReorderSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody SuggestionResolutionRequest request) {
        ReorderSuggestionResponse updated = recommendationService.resolveReorderSuggestion(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
