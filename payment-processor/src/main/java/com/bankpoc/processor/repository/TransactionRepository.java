package com.bankpoc.processor.repository;

import com.bankpoc.processor.model.entity.Transaction;
import com.bankpoc.processor.model.enums.FailureCategory;
import com.bankpoc.processor.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByBatchId(String batchId);

    List<Transaction> findByBatchIdAndStatus(String batchId, TransactionStatus status);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByFailureCategory(FailureCategory category);

    boolean existsByTransactionId(String transactionId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByBatchIdAndStatusIn(String batchId, List<TransactionStatus> statuses);

    @Query("SELECT t.status, COUNT(t) FROM Transaction t WHERE t.batchId = :batchId GROUP BY t.status")
    List<Object[]> countByBatchIdGroupByStatus(String batchId);

    @Query("SELECT t.failureCategory, COUNT(t) FROM Transaction t WHERE DATE(t.createdAt) = CURRENT_DATE AND t.failureCategory IS NOT NULL GROUP BY t.failureCategory")
    List<Object[]> countByFailureCategoryToday();
}
