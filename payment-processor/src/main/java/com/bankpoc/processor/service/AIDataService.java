package com.bankpoc.processor.service;

import com.bankpoc.processor.dto.AIBatchSummary;
import com.bankpoc.processor.dto.AIFailureDetail;
import com.bankpoc.processor.dto.AISystemContext;
import com.bankpoc.processor.model.entity.Batch;
import com.bankpoc.processor.model.entity.Transaction;
import com.bankpoc.processor.model.enums.BatchStatus;
import com.bankpoc.processor.repository.BatchRepository;
import com.bankpoc.processor.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIDataService {

    private final BatchRepository batchRepository;
    private final TransactionRepository transactionRepository;

    public List<AIBatchSummary> getBatchSummaries(LocalDateTime from, LocalDateTime to) {
        return batchRepository.findByCreatedAtBetween(from, to).stream()
                .map(this::buildBatchSummary)
                .collect(Collectors.toList());
    }

    public AIBatchSummary getBatchAnalysis(String batchId) {
        Batch batch = batchRepository.findByBatchId(batchId).orElseThrow();
        return buildBatchSummary(batch);
    }

    public Map<String, Object> getFailurePatterns(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> patterns = new HashMap<>();
        List<Object[]> categoryCounts = transactionRepository.countByFailureCategoryToday();
        
        Map<String, Integer> categoryMap = new HashMap<>();
        for (Object[] row : categoryCounts) {
            categoryMap.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        patterns.put("categoryCounts", categoryMap);
        
        return patterns;
    }

    public List<AIFailureDetail> getTransactionFailures(String batchId) {
        List<Transaction> failures = transactionRepository.findByBatchId(batchId).stream()
                .filter(t -> t.getStatus().name().equals("FAILED") || t.getStatus().name().equals("TIMEOUT"))
                .collect(Collectors.toList());
                
        return failures.stream().map(t -> AIFailureDetail.builder()
                .transactionId(t.getTransactionId())
                .batchId(t.getBatchId())
                .failureCategory(t.getFailureCategory() != null ? t.getFailureCategory().name() : null)
                .failureReason(t.getFailureReason())
                .npciResponseCode(t.getNpciResponseCode())
                .amount(t.getAmount())
                .senderIfsc(t.getSenderIfsc())
                .receiverIfsc(t.getReceiverIfsc())
                .timestamp(t.getProcessedAt() != null ? t.getProcessedAt() : t.getCreatedAt())
                .retryCount(t.getRetryCount())
                .isRetryable(t.canRetry())
                .build()).collect(Collectors.toList());
    }

    public AISystemContext getSystemContext() {
        long totalToday = batchRepository.countBatchesToday();
        long successToday = batchRepository.countByStatusToday(BatchStatus.COMPLETED);
        
        return AISystemContext.builder()
                .systemHealth("UP")
                .activeProcessingBatches((int) batchRepository.countByStatusToday(BatchStatus.PROCESSING))
                .totalBatchesToday((int) totalToday)
                .successRateToday(totalToday > 0 ? (double) successToday / totalToday * 100 : 0)
                .databaseStatus("CONNECTED")
                .npciServiceStatus("UNKNOWN")
                .systemUptime("UP")
                .lastProcessedAt(LocalDateTime.now())
                .build();
    }

    private AIBatchSummary buildBatchSummary(Batch batch) {
        List<Transaction> transactions = transactionRepository.findByBatchId(batch.getBatchId());
        
        Map<String, Integer> failureBreakdown = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getFailureCategory() != null) {
                failureBreakdown.merge(t.getFailureCategory().name(), 1, Integer::sum);
            }
        }
        
        return AIBatchSummary.builder()
                .batchId(batch.getBatchId())
                .status(batch.getStatus().name())
                .totalTransactions(batch.getTotalTransactions())
                .successCount(batch.getSuccessfulTransactions())
                .failureCount(batch.getFailedTransactions())
                .totalAmount(batch.getTotalAmount())
                .failureBreakdown(failureBreakdown)
                .build();
    }
}
