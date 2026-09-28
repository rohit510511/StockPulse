package StockPlus.demo.repository;

import StockPlus.demo.model.PricingSuggestion;
import StockPlus.demo.model.SuggestionStatus;
import StockPlus.demo.model.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    @Query("SELECT p FROM PricingSuggestion p WHERE (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    List<PricingSuggestion> findByStatusFilter(@Param("status") SuggestionStatus status);

    List<PricingSuggestion> findByProductIdAndStatus(Long productId, SuggestionStatus status);

    boolean existsByProductIdAndStatus(Long productId, SuggestionStatus status);

    boolean existsByProductIdAndTriggerReasonAndStatus(Long productId, TriggerReason triggerReason, SuggestionStatus status);

    long countByProductIdAndStatus(Long productId, SuggestionStatus status);
}
