package com.bankpoc.npcimock.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotBlank(message = "Sender account is required")
    private String senderAccount;
    
    @NotBlank(message = "Sender IFSC is required")
    private String senderIfsc;
    
    @NotBlank(message = "Receiver account is required")
    private String receiverAccount;
    
    @NotBlank(message = "Receiver IFSC is required")
    private String receiverIfsc;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String currency = "INR";
    
    @NotBlank(message = "Payment mode is required")
    private String paymentMode; // NEFT, RTGS, IMPS
    
    private String narration;
    
    private String batchId;
    
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
