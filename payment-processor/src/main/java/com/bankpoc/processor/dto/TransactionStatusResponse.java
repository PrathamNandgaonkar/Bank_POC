package com.bankpoc.processor.dto;

import com.bankpoc.processor.model.enums.FailureCategory;
import com.bankpoc.processor.model.enums.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionStatusResponse {
    private String transactionId;
    private String batchId;
    private String senderAccount;
    private String receiverAccount;
    private BigDecimal amount;
    private TransactionStatus status;
    private String npciReference;
    private FailureCategory failureCategory;
    private String failureReason;
    private int retryCount;
    private LocalDateTime processedAt;
}
