package StockPlus.demo.config;

import StockPlus.demo.model.Category;
import StockPlus.demo.model.Product;
import StockPlus.demo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Database already seeded with products. Skipping DataSeeder.");
            return;
        }

        log.info("Seeding initial 8 realistic products for StockPulse...");

        List<Product> seedProducts = List.of(
                // ELECTRONICS
                new Product("PRD-001", "Ultra-Slim Wireless Earbuds", Category.ELECTRONICS,
                        new BigDecimal("129.99"), 40, 10, 5.0),
                new Product("PRD-002", "Smart 4K Curved Monitor 27\"", Category.ELECTRONICS,
                        new BigDecimal("349.99"), 15, 5, 3.0),

                // APPAREL (Must include exact PRD-003 and PRD-008)
                // Initial APPAREL velocities: 12.0, 15.0, 1.0, 1.0, 1.0 -> Sum=30.0, Count=5, Avg=6.0, 3xAvg=18.0
                new Product("PRD-003", "Organic Cotton T-Shirt", Category.APPAREL,
                        new BigDecimal("28.00"), 8, 15, 12.0),
                new Product("PRD-008", "Hoodie — Heather Grey", Category.APPAREL,
                        new BigDecimal("55.00"), 11, 12, 15.0),
                new Product("PRD-004", "Everyday Canvas Tote Bag", Category.APPAREL,
                        new BigDecimal("19.99"), 35, 10, 1.0),
                new Product("PRD-005", "Merino Wool Winter Beanie", Category.APPAREL,
                        new BigDecimal("24.99"), 28, 8, 1.0),
                new Product("PRD-006", "Crew Athletic Performance Socks", Category.APPAREL,
                        new BigDecimal("12.99"), 60, 15, 1.0),

                // HOME
                new Product("PRD-007", "Artisan Ceramic Pour-Over Maker", Category.HOME,
                        new BigDecimal("39.50"), 20, 8, 4.0)
        );

        productRepository.saveAll(seedProducts);
        log.info("Successfully seeded 8 catalog products.");
    }
}
