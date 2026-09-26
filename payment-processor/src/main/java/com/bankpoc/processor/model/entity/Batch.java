package com.bankpoc.processor.model.entity;

import com.bankpoc.processor.model.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Batch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", unique = true, nullable = false)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(name = "total_transactions")
    private int totalTransactions;

    @Column(name = "successful_transactions")
    private int successfulTransactions;

    @Column(name = "failed_transactions")
    private int failedTransactions;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "error_message")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public void incrementSuccessful() {
        this.successfulTransactions++;
    }

    public void incrementFailed() {
        this.failedTransactions++;
    }

    public boolean isComplete() {
        return (this.successfulTransactions + this.failedTransactions) >= this.totalTransactions;
    }

    public double getSuccessRate() {
        if (totalTransactions == 0) return 0.0;
        return (double) successfulTransactions / totalTransactions * 100.0;
    }
}
