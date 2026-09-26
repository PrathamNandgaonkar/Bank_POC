package com.bankpoc.processor.repository;

import com.bankpoc.processor.model.entity.Batch;
import com.bankpoc.processor.model.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    
    Optional<Batch> findByBatchId(String batchId);
    
    List<Batch> findByStatus(BatchStatus status);
    
    List<Batch> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByBatchId(String batchId);
    
    List<Batch> findByStatusIn(List<BatchStatus> statuses);

    @Query("SELECT COUNT(b) FROM Batch b WHERE DATE(b.createdAt) = CURRENT_DATE AND b.status = :status")
    long countByStatusToday(BatchStatus status);

    @Query("SELECT SUM(b.totalAmount) FROM Batch b WHERE DATE(b.createdAt) = CURRENT_DATE AND b.status = :status")
    BigDecimal sumAmountByStatusToday(BatchStatus status);
    
    @Query("SELECT COUNT(b) FROM Batch b WHERE DATE(b.createdAt) = CURRENT_DATE")
    long countBatchesToday();
}
