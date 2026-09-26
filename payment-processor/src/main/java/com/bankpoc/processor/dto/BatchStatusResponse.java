package com.bankpoc.processor.dto;

import com.bankpoc.processor.model.enums.BatchStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BatchStatusResponse {
    private String batchId;
    private BatchStatus status;
    private int totalTransactions;
    private int successfulTransactions;
    private int failedTransactions;
    private int pendingTransactions;
    private BigDecimal totalAmount;
    private BigDecimal successfulAmount;
    private BigDecimal failedAmount;
    private double successRate;
    private long processingTimeMs;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
