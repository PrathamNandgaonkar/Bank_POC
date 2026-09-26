package com.bankpoc.processor.service;

import com.bankpoc.processor.dto.CSVTransactionRecord;
import com.bankpoc.processor.dto.NPCIPaymentResponse;
import com.bankpoc.processor.model.entity.Batch;
import com.bankpoc.processor.model.entity.BatchPart;
import com.bankpoc.processor.model.entity.Transaction;
import com.bankpoc.processor.model.enums.BatchStatus;
import com.bankpoc.processor.model.enums.FailureCategory;
import com.bankpoc.processor.model.enums.TransactionStatus;
import com.bankpoc.processor.repository.BatchPartRepository;
import com.bankpoc.processor.repository.BatchRepository;
import com.bankpoc.processor.repository.TransactionRepository;
import com.bankpoc.processor.util.TraceIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final BatchRepository batchRepository;
    private final BatchPartRepository batchPartRepository;
    private final TransactionRepository transactionRepository;
    private final FileProcessingService fileProcessingService;
    private final TransactionService transactionService;
    private final NPCIClientService npciClientService;
    private final AuditService auditService;

    @Async("paymentProcessingExecutor")
    public void processBatch(String batchId) {
        MDC.put("batchId", batchId);
        MDC.put("traceId", TraceIdGenerator.generate());
        try {
            log.info("Starting processing for batch {}", batchId);
            
            Batch batch = batchRepository.findByBatchId(batchId).orElseThrow();
            updateBatchStatus(batch, BatchStatus.VALIDATING);

            List<BatchPart> parts = batchPartRepository.findByBatchId(batchId);
            int totalRecords = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (BatchPart part : parts) {
                List<CSVTransactionRecord> records = fileProcessingService.parseCSVFile(part.getFilePath());
                totalRecords += records.size();
                for (CSVTransactionRecord r : records) {
                    if (r.getAmount() != null) {
                        totalAmount = totalAmount.add(r.getAmount());
                    }
                }
                transactionService.createTransactions(batchId, records, part.getPartNumber());
            }

            batch.setTotalTransactions(totalRecords);
            batch.setTotalAmount(totalAmount);
            batchRepository.save(batch);

            updateBatchStatus(batch, BatchStatus.PROCESSING);
            
            List<Transaction> transactions = transactionRepository.findByBatchId(batchId);
            for (Transaction txn : transactions) {
                processTransaction(txn);
            }

            finalizeBatch(batch);

        } catch (Exception e) {
            log.error("Fatal error processing batch {}", batchId, e);
            Batch batch = batchRepository.findByBatchId(batchId).orElse(null);
            if (batch != null) {
                batch.setErrorMessage(e.getMessage());
                updateBatchStatus(batch, BatchStatus.FAILED);
            }
        } finally {
            MDC.clear();
        }
    }

    private void processTransaction(Transaction txn) {
        MDC.put("transactionId", txn.getTransactionId());
        try {
            TransactionService.ValidationResult validationResult = transactionService.validateTransaction(mapToCsvRecord(txn));
            if (!validationResult.isValid()) {
                transactionService.updateTransactionStatus(txn.getTransactionId(), TransactionStatus.FAILED, null, null, String.join(", ", validationResult.errors()), FailureCategory.VALIDATION, "Invalid data");
                incrementBatchCounters(txn.getBatchId(), false);
                return;
            }

            transactionService.updateTransactionStatus(txn.getTransactionId(), TransactionStatus.PROCESSING, null, null, null, null, null);

            try {
                NPCIPaymentResponse response = npciClientService.processPayment(txn);
                if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                    transactionService.updateTransactionStatus(txn.getTransactionId(), TransactionStatus.SUCCESS, response.getNpciReferenceId(), response.getResponseCode(), response.getResponseMessage(), null, null);
                    incrementBatchCounters(txn.getBatchId(), true);
                } else {
                    FailureCategory category = FailureCategory.BUSINESS;
                    TransactionStatus newStatus = TransactionStatus.FAILED;
                    
                    if ("TIMEOUT".equalsIgnoreCase(response.getStatus())) {
                        newStatus = TransactionStatus.TIMEOUT;
                        category = FailureCategory.TIMEOUT;
                    } else if (response.getResponseCode() != null) {
                        if (response.getResponseCode().startsWith("T")) {
                            category = FailureCategory.INFRASTRUCTURE;
                        } else if (response.getResponseCode().startsWith("E")) {
                            category = FailureCategory.BUSINESS;
                        }
                    }
                    transactionService.updateTransactionStatus(txn.getTransactionId(), newStatus, response.getNpciReferenceId(), response.getResponseCode(), response.getResponseMessage(), category, response.getResponseMessage());
                    incrementBatchCounters(txn.getBatchId(), false);
                }
            } catch (Exception e) {
                log.error("NPCI call failed for transaction {}", txn.getTransactionId(), e);
                transactionService.updateTransactionStatus(txn.getTransactionId(), TransactionStatus.TIMEOUT, null, null, null, FailureCategory.TIMEOUT, e.getMessage());
                incrementBatchCounters(txn.getBatchId(), false);
            }

        } finally {
            MDC.remove("transactionId");
        }
    }

    private synchronized void incrementBatchCounters(String batchId, boolean isSuccess) {
        Batch batch = batchRepository.findByBatchId(batchId).orElseThrow();
        if (isSuccess) {
            batch.incrementSuccessful();
        } else {
            batch.incrementFailed();
        }
        batchRepository.save(batch);
    }

    private void finalizeBatch(Batch batch) {
        batch = batchRepository.findById(batch.getId()).orElseThrow();
        if (batch.getSuccessfulTransactions() == batch.getTotalTransactions()) {
            updateBatchStatus(batch, BatchStatus.COMPLETED);
        } else if (batch.getFailedTransactions() == batch.getTotalTransactions()) {
            updateBatchStatus(batch, BatchStatus.FAILED);
        } else {
            updateBatchStatus(batch, BatchStatus.PARTIAL_SUCCESS);
        }
    }

    private void updateBatchStatus(Batch batch, BatchStatus status) {
        String oldStatus = batch.getStatus().name();
        batch.setStatus(status);
        if (status == BatchStatus.COMPLETED || status == BatchStatus.PARTIAL_SUCCESS || status == BatchStatus.FAILED) {
            batch.setCompletedAt(LocalDateTime.now());
        }
        batchRepository.save(batch);
        auditService.logStateChange("BATCH", batch.getBatchId(), "STATUS_UPDATE", oldStatus, status.name(), null);
    }

    private CSVTransactionRecord mapToCsvRecord(Transaction txn) {
        CSVTransactionRecord r = new CSVTransactionRecord();
        r.setTransactionId(txn.getTransactionId());
        r.setSenderAccount(txn.getSenderAccount());
        r.setSenderIfsc(txn.getSenderIfsc());
        r.setAmount(txn.getAmount());
        return r;
    }
}
