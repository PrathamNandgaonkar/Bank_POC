package com.bankpoc.npcimock.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String npciReferenceId;
    private String transactionId;
    private String status; // SUCCESS, FAILED, TIMEOUT, PENDING
    private String responseCode;
    private String responseMessage;
    private LocalDateTime processedAt;
    private long processingDurationMs;
}
