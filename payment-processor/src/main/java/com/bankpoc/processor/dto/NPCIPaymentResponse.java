package com.bankpoc.processor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NPCIPaymentResponse {
    private String npciReferenceId;
    private String transactionId;
    private String status;
    private String responseCode;
    private String responseMessage;
    private long processingDurationMs;
    private LocalDateTime processedAt;
}
