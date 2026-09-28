package StockPlus.demo.controller;

import StockPlus.demo.advisor.StrategyRegistry;
import StockPlus.demo.dto.StrategyRequest;
import StockPlus.demo.dto.StrategyResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/strategy")
@CrossOrigin(origins = "*")
public class AdminStrategyController {

    private final StrategyRegistry strategyRegistry;

    public AdminStrategyController(StrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @GetMapping
    public ResponseEntity<StrategyResponse> getStrategy() {
        return ResponseEntity.ok(new StrategyResponse(strategyRegistry.getActiveStrategyType()));
    }

    @PutMapping
    public ResponseEntity<StrategyResponse> updateStrategy(@Valid @RequestBody StrategyRequest request) {
        strategyRegistry.setActiveStrategyType(request.getStrategy());
        return ResponseEntity.ok(new StrategyResponse(strategyRegistry.getActiveStrategyType()));
    }
}
