package StockPlus.demo.service;

import StockPlus.demo.model.*;
import StockPlus.demo.repository.PricingSuggestionRepository;
import StockPlus.demo.repository.ProductRepository;
import StockPlus.demo.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdempotencyTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        pricingSuggestionRepository.deleteAll();
        reorderSuggestionRepository.deleteAll();
        productRepository.deleteAll();

        Product product = new Product("SKU-IDEMPOTENT-1", "Idempotent Check Product",
                Category.HOME, new BigDecimal("40.00"), 5, 10, 2.0);
        testProduct = productRepository.save(product);
    }

    @Test
    void processTrigger_repeatedSequentialCalls_createsOnlyOnePendingPair() {
        // Trigger 1st time
        recommendationService.processTrigger(testProduct.getId(), TriggerReason.INVENTORY_LOW);
        // Trigger 2nd time
        recommendationService.processTrigger(testProduct.getId(), TriggerReason.INVENTORY_LOW);
        // Trigger 3rd time
        recommendationService.processTrigger(testProduct.getId(), TriggerReason.INVENTORY_LOW);

        long pricingCount = pricingSuggestionRepository
                .findByProductIdAndStatus(testProduct.getId(), SuggestionStatus.PENDING).size();
        long reorderCount = reorderSuggestionRepository
                .findByProductIdAndStatus(testProduct.getId(), SuggestionStatus.PENDING).size();

        assertEquals(1, pricingCount, "Expected exactly 1 pending pricing suggestion");
        assertEquals(1, reorderCount, "Expected exactly 1 pending reorder suggestion");
    }

    @Test
    void processTrigger_concurrentCalls_isThreadSafeAndCreatesOnlyOnePendingPair() throws InterruptedException {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    recommendationService.processTrigger(testProduct.getId(), TriggerReason.INVENTORY_LOW);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, finished);

        long pricingCount = pricingSuggestionRepository
                .findByProductIdAndStatus(testProduct.getId(), SuggestionStatus.PENDING).size();
        long reorderCount = reorderSuggestionRepository
                .findByProductIdAndStatus(testProduct.getId(), SuggestionStatus.PENDING).size();

        assertEquals(1, pricingCount, "Concurrent triggers must produce exactly 1 pending pricing suggestion");
        assertEquals(1, reorderCount, "Concurrent triggers must produce exactly 1 pending reorder suggestion");
    }
}
