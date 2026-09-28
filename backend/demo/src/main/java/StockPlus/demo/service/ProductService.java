package StockPlus.demo.service;

import StockPlus.demo.dto.*;
import StockPlus.demo.event.ProductInventoryChangedEvent;
import StockPlus.demo.exception.ConflictException;
import StockPlus.demo.exception.ResourceNotFoundException;
import StockPlus.demo.model.Category;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.ProductStatus;
import StockPlus.demo.model.SuggestionStatus;
import StockPlus.demo.repository.PricingSuggestionRepository;
import StockPlus.demo.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository,
                          PricingSuggestionRepository pricingSuggestionRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.eventPublisher = eventPublisher;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("Product already exists with SKU: " + request.getSku());
        }

        Product product = new Product(
                request.getSku(),
                request.getName(),
                request.getCategory(),
                request.getCurrentPrice(),
                request.getStockLevel(),
                request.getReorderThreshold(),
                request.getDemandVelocity()
        );

        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        if (request.getSupplierId() != null) {
            product.setSupplierId(request.getSupplierId());
        }

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(ProductStatus status, Category category) {
        return productRepository.findByFilters(status, category)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = getProductEntity(id);
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public ProductResponse updateStock(Long id, StockUpdateRequest request) {
        Product product = getProductEntity(id);
        int previousStock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        product.setStockLevel(request.getStockLevel());

        boolean hasPendingPricing = pricingSuggestionRepository.existsByProductIdAndStatus(id, SuggestionStatus.PENDING);
        product.recalculateStatus(hasPendingPricing);

        Product saved = productRepository.save(product);

        // Publish event for agentic loop
        eventPublisher.publishEvent(new ProductInventoryChangedEvent(
                saved.getId(), previousStock, saved.getStockLevel(), saved.getDemandVelocity()));

        return ProductResponse.fromEntity(saved);
    }

    public OrderSimulationResponse simulateOrder(Long id, OrderSimulationRequest request) {
        Product product = getProductEntity(id);
        int currentStock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        int requestedQty = request.getQuantity();

        if (currentStock < requestedQty) {
            throw new ConflictException(String.format(
                    "Insufficient stock for product %s. Requested: %d, Available: %d",
                    product.getSku(), requestedQty, currentStock));
        }

        int remainingStock = currentStock - requestedQty;
        product.setStockLevel(remainingStock);

        // Bump demand velocity (+5.0 per order simulation)
        double currentVelocity = product.getDemandVelocity() != null ? product.getDemandVelocity() : 0.0;
        double newVelocity = currentVelocity + 5.0;
        product.setDemandVelocity(newVelocity);

        boolean hasPendingPricing = pricingSuggestionRepository.existsByProductIdAndStatus(id, SuggestionStatus.PENDING);
        product.recalculateStatus(hasPendingPricing);

        Product saved = productRepository.save(product);

        BigDecimal totalAmount = saved.getCurrentPrice().multiply(BigDecimal.valueOf(requestedQty));

        // Publish event for agentic loop
        eventPublisher.publishEvent(new ProductInventoryChangedEvent(
                saved.getId(), currentStock, saved.getStockLevel(), saved.getDemandVelocity()));

        return new OrderSimulationResponse(
                saved.getId(),
                saved.getSku(),
                saved.getName(),
                requestedQty,
                saved.getCurrentPrice(),
                totalAmount,
                saved.getStockLevel(),
                saved.getDemandVelocity(),
                saved.getStatus()
        );
    }
}
