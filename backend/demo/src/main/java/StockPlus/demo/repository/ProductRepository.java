package StockPlus.demo.repository;

import StockPlus.demo.model.Category;
import StockPlus.demo.model.Product;
import StockPlus.demo.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByCategory(Category category);

    List<ProductStatus> findByStatus(ProductStatus status);

    @Query("SELECT p FROM Product p WHERE (:status IS NULL OR p.status = :status) AND (:category IS NULL OR p.category = :category) ORDER BY p.id ASC")
    List<Product> findByFilters(@Param("status") ProductStatus status, @Param("category") Category category);

    @Query("SELECT COALESCE(AVG(p.demandVelocity), 0.0) FROM Product p WHERE p.category = :category")
    Double findAverageDemandVelocityByCategory(@Param("category") Category category);
}
