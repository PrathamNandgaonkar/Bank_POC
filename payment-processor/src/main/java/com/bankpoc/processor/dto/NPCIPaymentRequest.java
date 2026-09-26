package com.bankpoc.processor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NPCIPaymentRequest {
    private String transactionId;
    private String senderAccount;
    private String senderIfsc;
    private String receiverAccount;
    private String receiverIfsc;
    private BigDecimal amount;
    private String currency;
    private String paymentMode;
    private String narration;
    private String batchId;
    private String idempotencyKey;
}
