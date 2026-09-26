package com.bankpoc.processor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AIFailureDetail {
    private String transactionId;
    private String batchId;
    private String failureCategory;
    private String failureReason;
    private String npciResponseCode;
    private BigDecimal amount;
    private String senderIfsc;
    private String receiverIfsc;
    private LocalDateTime timestamp;
    private int retryCount;
    private boolean isRetryable;
}
