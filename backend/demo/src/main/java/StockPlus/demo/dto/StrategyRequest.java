package StockPlus.demo.dto;

import StockPlus.demo.advisor.StrategyType;
import jakarta.validation.constraints.NotNull;

public class StrategyRequest {

    @NotNull(message = "Strategy is required")
    private StrategyType strategy;

    public StrategyRequest() {
    }

    public StrategyRequest(StrategyType strategy) {
        this.strategy = strategy;
    }

    public StrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyType strategy) {
        this.strategy = strategy;
    }
}
