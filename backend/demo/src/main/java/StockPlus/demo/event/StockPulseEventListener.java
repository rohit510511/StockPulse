package StockPlus.demo.event;

import StockPlus.demo.model.Product;
import StockPlus.demo.model.TriggerReason;
import StockPlus.demo.repository.ProductRepository;
import StockPlus.demo.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StockPulseEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockPulseEventListener.class);

    private final ProductRepository productRepository;
    private final RecommendationService recommendationService;
    private final double demandSpikeMultiplier;

    public StockPulseEventListener(
            ProductRepository productRepository,
            RecommendationService recommendationService,
            @Value("${commerce.demand-spike-multiplier:3.0}") double demandSpikeMultiplier) {
        this.productRepository = productRepository;
        this.recommendationService = recommendationService;
        this.demandSpikeMultiplier = demandSpikeMultiplier;
    }

    @Async("stockPulseTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryChanged(ProductInventoryChangedEvent event) {
        log.info("StockPulse Agent Loop: Received inventory changed event for product ID [{}]", event.getProductId());

        Product product = productRepository.findById(event.getProductId()).orElse(null);
        if (product == null) {
            log.warn("Product not found for event productId: {}", event.getProductId());
            return;
        }

        int stock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;
        double velocity = product.getDemandVelocity() != null ? product.getDemandVelocity() : 0.0;

        Double categoryAvg = productRepository.findAverageDemandVelocityByCategory(product.getCategory());
        double avg = categoryAvg != null ? categoryAvg : 0.0;

        // Trigger A: INVENTORY_LOW
        if (stock < threshold) {
            log.info("Trigger A (INVENTORY_LOW) activated for SKU [{}]: stock ({}) < threshold ({})",
                    product.getSku(), stock, threshold);
            recommendationService.processTrigger(product.getId(), TriggerReason.INVENTORY_LOW);
        }

        // Trigger B: DEMAND_SPIKE
        if (avg > 0 && velocity > demandSpikeMultiplier * avg) {
            log.info("Trigger B (DEMAND_SPIKE) activated for SKU [{}]: velocity ({}) > {}x category avg ({})",
                    product.getSku(), velocity, demandSpikeMultiplier, avg);
            recommendationService.processTrigger(product.getId(), TriggerReason.DEMAND_SPIKE);
        }
    }
}
