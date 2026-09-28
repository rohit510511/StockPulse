package StockPlus.demo.service;

import StockPlus.demo.advisor.*;
import StockPlus.demo.dto.PricingSuggestionResponse;
import StockPlus.demo.dto.ReorderSuggestionResponse;
import StockPlus.demo.exception.ConflictException;
import StockPlus.demo.exception.ResourceNotFoundException;
import StockPlus.demo.model.*;
import StockPlus.demo.repository.PricingSuggestionRepository;
import StockPlus.demo.repository.ProductRepository;
import StockPlus.demo.repository.ReorderSuggestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final StrategyRegistry strategyRegistry;
    private final TransactionTemplate transactionTemplate;

    // Per-product mutex to guarantee race-safe idempotency checks across threads
    private final Map<Long, Object> productLocks = new ConcurrentHashMap<>();

    public RecommendationService(
            ProductRepository productRepository,
            PricingSuggestionRepository pricingSuggestionRepository,
            ReorderSuggestionRepository reorderSuggestionRepository,
            StrategyRegistry strategyRegistry,
            PlatformTransactionManager transactionManager) {
        this.productRepository = productRepository;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.strategyRegistry = strategyRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private Object getLockForProduct(Long productId) {
        return productLocks.computeIfAbsent(productId, k -> new Object());
    }

    /**
     * Agentic Trigger Processor: Generates both pricing and reorder suggestions for a given trigger
     * enforcing strict idempotency and race protection across concurrent threads.
     */
    public void processTrigger(Long productId, TriggerReason triggerReason) {
        Object lock = getLockForProduct(productId);
        synchronized (lock) {
            transactionTemplate.execute(status -> {
                boolean hasPendingPricing = pricingSuggestionRepository
                        .existsByProductIdAndTriggerReasonAndStatus(productId, triggerReason, SuggestionStatus.PENDING);
                boolean hasPendingReorder = reorderSuggestionRepository
                        .existsByProductIdAndTriggerReasonAndStatus(productId, triggerReason, SuggestionStatus.PENDING);

                if (hasPendingPricing && hasPendingReorder) {
                    log.info("Idempotency: PENDING suggestions already exist for product [{}] with trigger [{}]. Skipping.",
                            productId, triggerReason);
                    return null;
                }

                Product product = getProductEntity(productId);
                CategoryStats stats = getCategoryStats(product.getCategory());
                CommerceAdvisor advisor = strategyRegistry.getActiveAdvisor();

                CommerceRecommendation recommendation = advisor.recommend(product, triggerReason, stats);

                if (!hasPendingPricing) {
                    PricingSuggestion pricingSuggestion = new PricingSuggestion(
                            product,
                            product.getCurrentPrice(),
                            recommendation.getPricing().getRecommendedPrice(),
                            recommendation.getPricing().getDirection(),
                            recommendation.getPricing().getConfidence(),
                            recommendation.getPricing().getReasoning(),
                            triggerReason,
                            recommendation.getPricing().getSource()
                    );
                    pricingSuggestionRepository.saveAndFlush(pricingSuggestion);

                    // Update product status to PRICE_REVIEW_PENDING
                    product.recalculateStatus(true);
                    productRepository.saveAndFlush(product);
                }

                if (!hasPendingReorder) {
                    ReorderSuggestion reorderSuggestion = new ReorderSuggestion(
                            product,
                            product.getStockLevel(),
                            recommendation.getReorder().getRecommendedQuantity(),
                            recommendation.getReorder().getSuggestedLeadTimeDays(),
                            recommendation.getReorder().getConfidence(),
                            recommendation.getReorder().getReasoning(),
                            triggerReason,
                            recommendation.getReorder().getSource()
                    );
                    reorderSuggestionRepository.saveAndFlush(reorderSuggestion);
                }

                return null;
            });
        }
    }

    public PricingSuggestionResponse generatePricingSuggestion(Long productId, TriggerReason triggerReason) {
        Object lock = getLockForProduct(productId);
        synchronized (lock) {
            return transactionTemplate.execute(status -> {
                // Idempotency: return existing if already pending for this trigger
                List<PricingSuggestion> existing = pricingSuggestionRepository
                        .findByProductIdAndStatus(productId, SuggestionStatus.PENDING)
                        .stream()
                        .filter(s -> s.getTriggerReason() == triggerReason)
                        .collect(Collectors.toList());

                if (!existing.isEmpty()) {
                    return PricingSuggestionResponse.fromEntity(existing.get(0));
                }

                Product product = getProductEntity(productId);
                CategoryStats stats = getCategoryStats(product.getCategory());
                CommerceAdvisor advisor = strategyRegistry.getActiveAdvisor();

                PricingRecommendation recommendation = advisor.recommendPricing(product, triggerReason, stats);

                PricingSuggestion suggestion = new PricingSuggestion(
                        product,
                        product.getCurrentPrice(),
                        recommendation.getRecommendedPrice(),
                        recommendation.getDirection(),
                        recommendation.getConfidence(),
                        recommendation.getReasoning(),
                        triggerReason,
                        recommendation.getSource()
                );

                PricingSuggestion saved = pricingSuggestionRepository.saveAndFlush(suggestion);

                product.recalculateStatus(true);
                productRepository.saveAndFlush(product);

                return PricingSuggestionResponse.fromEntity(saved);
            });
        }
    }

    public ReorderSuggestionResponse generateReorderSuggestion(Long productId, TriggerReason triggerReason) {
        Object lock = getLockForProduct(productId);
        synchronized (lock) {
            return transactionTemplate.execute(status -> {
                // Idempotency: return existing if already pending for this trigger
                List<ReorderSuggestion> existing = reorderSuggestionRepository
                        .findByProductIdAndStatus(productId, SuggestionStatus.PENDING)
                        .stream()
                        .filter(s -> s.getTriggerReason() == triggerReason)
                        .collect(Collectors.toList());

                if (!existing.isEmpty()) {
                    return ReorderSuggestionResponse.fromEntity(existing.get(0));
                }

                Product product = getProductEntity(productId);
                CategoryStats stats = getCategoryStats(product.getCategory());
                CommerceAdvisor advisor = strategyRegistry.getActiveAdvisor();

                ReorderRecommendation recommendation = advisor.recommendReorder(product, triggerReason, stats);

                ReorderSuggestion suggestion = new ReorderSuggestion(
                        product,
                        product.getStockLevel(),
                        recommendation.getRecommendedQuantity(),
                        recommendation.getSuggestedLeadTimeDays(),
                        recommendation.getConfidence(),
                        recommendation.getReasoning(),
                        triggerReason,
                        recommendation.getSource()
                );

                ReorderSuggestion saved = reorderSuggestionRepository.saveAndFlush(suggestion);
                return ReorderSuggestionResponse.fromEntity(saved);
            });
        }
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestionResponse> getPricingSuggestions(SuggestionStatus status) {
        return pricingSuggestionRepository.findByStatusFilter(status)
                .stream()
                .map(PricingSuggestionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionResponse> getReorderSuggestions(SuggestionStatus status) {
        return reorderSuggestionRepository.findByStatusFilter(status)
                .stream()
                .map(ReorderSuggestionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PricingSuggestionResponse resolvePricingSuggestion(Long id, SuggestionStatus targetStatus) {
        if (targetStatus != SuggestionStatus.ACCEPTED && targetStatus != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Suggestion resolution status must be ACCEPTED or REJECTED");
        }

        PricingSuggestion suggestion = pricingSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found with id: " + id));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ConflictException("Suggestion with id " + id + " is not PENDING. Current status: " + suggestion.getStatus());
        }

        Product product = suggestion.getProduct();

        suggestion.setStatus(targetStatus);
        suggestion.setResolvedAt(LocalDateTime.now());
        PricingSuggestion updated = pricingSuggestionRepository.saveAndFlush(suggestion);

        if (targetStatus == SuggestionStatus.ACCEPTED) {
            product.setCurrentPrice(suggestion.getRecommendedPrice());
        }

        // Recalculate product status based on remaining pending suggestions
        boolean hasPendingPricing = pricingSuggestionRepository.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING);
        product.recalculateStatus(hasPendingPricing);
        productRepository.save(product);

        return PricingSuggestionResponse.fromEntity(updated);
    }

    @Transactional
    public ReorderSuggestionResponse resolveReorderSuggestion(Long id, SuggestionStatus targetStatus) {
        if (targetStatus != SuggestionStatus.ACCEPTED && targetStatus != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Suggestion resolution status must be ACCEPTED or REJECTED");
        }

        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found with id: " + id));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ConflictException("Suggestion with id " + id + " is not PENDING. Current status: " + suggestion.getStatus());
        }

        Product product = suggestion.getProduct();

        suggestion.setStatus(targetStatus);
        suggestion.setResolvedAt(LocalDateTime.now());
        ReorderSuggestion updated = reorderSuggestionRepository.saveAndFlush(suggestion);

        if (targetStatus == SuggestionStatus.ACCEPTED) {
            int newStock = (product.getStockLevel() != null ? product.getStockLevel() : 0) + suggestion.getRecommendedQuantity();
            product.setStockLevel(newStock);
        }

        boolean hasPendingPricing = pricingSuggestionRepository.existsByProductIdAndStatus(product.getId(), SuggestionStatus.PENDING);
        product.recalculateStatus(hasPendingPricing);
        productRepository.save(product);

        return ReorderSuggestionResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public CategoryStats getCategoryStats(Category category) {
        Double avgVelocity = productRepository.findAverageDemandVelocityByCategory(category);
        long count = productRepository.findByCategory(category).size();
        return new CategoryStats(avgVelocity != null ? avgVelocity : 0.0, count);
    }

    private Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
