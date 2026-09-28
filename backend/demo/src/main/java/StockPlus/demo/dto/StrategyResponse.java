package StockPlus.demo.dto;

import StockPlus.demo.advisor.StrategyType;

public class StrategyResponse {

    private StrategyType strategy;

    public StrategyResponse() {
    }

    public StrategyResponse(StrategyType strategy) {
        this.strategy = strategy;
    }

    public StrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyType strategy) {
        this.strategy = strategy;
    }
}
