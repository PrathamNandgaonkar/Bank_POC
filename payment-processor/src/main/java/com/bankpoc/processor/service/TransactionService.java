package com.bankpoc.processor.service;

import com.bankpoc.processor.dto.CSVTransactionRecord;
import com.bankpoc.processor.dto.TransactionStatusResponse;
import com.bankpoc.processor.exception.DuplicateTransactionException;
import com.bankpoc.processor.model.entity.Transaction;
import com.bankpoc.processor.model.enums.FailureCategory;
import com.bankpoc.processor.model.enums.PaymentMode;
import com.bankpoc.processor.model.enums.TransactionStatus;
import com.bankpoc.processor.repository.TransactionRepository;
import com.bankpoc.processor.util.IdempotencyKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;
    private final AuditService auditService;

    @Transactional
    public int createTransactions(String batchId, List<CSVTransactionRecord> records, int partNumber) {
        List<Transaction> transactions = new ArrayList<>();
        
        for (CSVTransactionRecord record : records) {
            String idemKey = idempotencyKeyGenerator.generateKey(
                    record.getTransactionId(),
                    record.getSenderAccount(),
                    record.getReceiverAccount(),
                    record.getAmount()
            );

            if (isDuplicateTransaction(record.getTransactionId(), idemKey)) {
                log.warn("Duplicate transaction skipped: {}", record.getTransactionId());
                continue;
            }

            Transaction txn = Transaction.builder()
                    .transactionId(record.getTransactionId())
                    .batchId(batchId)
                    .idempotencyKey(idemKey)
                    .senderAccount(record.getSenderAccount())
                    .senderIfsc(record.getSenderIfsc())
                    .senderName(record.getSenderName())
                    .receiverAccount(record.getReceiverAccount())
                    .receiverIfsc(record.getReceiverIfsc())
                    .receiverName(record.getReceiverName())
                    .amount(record.getAmount())
                    .paymentMode(PaymentMode.valueOf(record.getPaymentMode().toUpperCase()))
                    .narration(record.getNarration())
                    .status(TransactionStatus.PENDING)
                    .build();
            transactions.add(txn);
        }
        
        transactionRepository.saveAll(transactions);
        return transactions.size();
    }

    public ValidationResult validateTransaction(CSVTransactionRecord record) {
        List<String> errors = new ArrayList<>();
        if (record.getSenderAccount() == null || !record.getSenderAccount().matches("\\d{10,18}")) {
            errors.add("Invalid sender account format");
        }
        if (record.getSenderIfsc() == null || !record.getSenderIfsc().matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            errors.add("Invalid sender IFSC format");
        }
        if (record.getAmount() == null || record.getAmount().doubleValue() <= 0) {
            errors.add("Amount must be greater than zero");
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public TransactionStatusResponse getTransactionStatus(String txnId) {
        Transaction txn = transactionRepository.findByTransactionId(txnId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return mapToResponse(txn);
    }

    public List<TransactionStatusResponse> getTransactionsByBatch(String batchId) {
        return transactionRepository.findByBatchId(batchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateTransactionStatus(String txnId, TransactionStatus status, String npciRef, String responseCode, String responseMsg, FailureCategory failCat, String failReason) {
        Transaction txn = transactionRepository.findByTransactionId(txnId).orElseThrow();
        String oldStatus = txn.getStatus().name();
        
        txn.setStatus(status);
        txn.setNpciReference(npciRef);
        txn.setNpciResponseCode(responseCode);
        txn.setFailureCategory(failCat);
        txn.setFailureReason(failReason);
        if (status == TransactionStatus.SUCCESS || status == TransactionStatus.FAILED) {
            txn.setProcessedAt(LocalDateTime.now());
        }
        
        transactionRepository.save(txn);
        
        auditService.logStateChange("TRANSACTION", txnId, "STATUS_UPDATE", oldStatus, status.name(), null);
    }

    public boolean isDuplicateTransaction(String txnId, String idempotencyKey) {
        return transactionRepository.existsByTransactionId(txnId) || transactionRepository.existsByIdempotencyKey(idempotencyKey);
    }

    private TransactionStatusResponse mapToResponse(Transaction txn) {
        return TransactionStatusResponse.builder()
                .transactionId(txn.getTransactionId())
                .batchId(txn.getBatchId())
                .senderAccount(txn.getSenderAccount())
                .receiverAccount(txn.getReceiverAccount())
                .amount(txn.getAmount())
                .status(txn.getStatus())
                .npciReference(txn.getNpciReference())
                .failureCategory(txn.getFailureCategory())
                .failureReason(txn.getFailureReason())
                .retryCount(txn.getRetryCount())
                .processedAt(txn.getProcessedAt())
                .build();
    }

    public record ValidationResult(boolean isValid, List<String> errors) {}
}
