package StockPlus.demo.advisor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class StrategyRegistry {

    private final AtomicReference<StrategyType> activeStrategyType = new AtomicReference<>(StrategyType.RULE_BASED);
    private final ApplicationContext applicationContext;
    private final RuleBasedStrategy ruleBasedStrategy;

    public StrategyRegistry(
            @Value("${commerce.strategy.default:RULE_BASED}") String defaultStrategy,
            ApplicationContext applicationContext,
            RuleBasedStrategy ruleBasedStrategy) {
        this.applicationContext = applicationContext;
        this.ruleBasedStrategy = ruleBasedStrategy;
        try {
            this.activeStrategyType.set(StrategyType.valueOf(defaultStrategy.toUpperCase()));
        } catch (Exception e) {
            this.activeStrategyType.set(StrategyType.RULE_BASED);
        }
    }

    public StrategyType getActiveStrategyType() {
        return activeStrategyType.get();
    }

    public void setActiveStrategyType(StrategyType type) {
        if (type == null) {
            throw new IllegalArgumentException("Strategy type cannot be null");
        }
        this.activeStrategyType.set(type);
    }

    public CommerceAdvisor getActiveAdvisor() {
        StrategyType type = activeStrategyType.get();
        if (type == StrategyType.AI) {
            if (applicationContext.containsBean("aiCommerceStrategy")) {
                return applicationContext.getBean("aiCommerceStrategy", CommerceAdvisor.class);
            }
        }
        return ruleBasedStrategy;
    }
}
