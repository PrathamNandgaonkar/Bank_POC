package com.bankpoc.processor.model.entity;

import com.bankpoc.processor.model.enums.FailureCategory;
import com.bankpoc.processor.model.enums.PaymentMode;
import com.bankpoc.processor.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "sender_account", nullable = false)
    private String senderAccount;

    @Column(name = "sender_ifsc", nullable = false)
    private String senderIfsc;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "receiver_account", nullable = false)
    private String receiverAccount;

    @Column(name = "receiver_ifsc", nullable = false)
    private String receiverIfsc;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Column
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "npci_reference")
    private String npciReference;

    @Column(name = "npci_response_code")
    private String npciResponseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category")
    private FailureCategory failureCategory;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public boolean canRetry() {
        return this.retryCount < 3 && this.status == TransactionStatus.TIMEOUT;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
