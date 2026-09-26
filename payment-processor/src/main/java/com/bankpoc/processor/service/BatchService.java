package com.bankpoc.processor.service;

import com.bankpoc.processor.dto.BatchIngestRequest;
import com.bankpoc.processor.dto.BatchStatusResponse;
import com.bankpoc.processor.model.entity.Batch;
import com.bankpoc.processor.model.entity.BatchPart;
import com.bankpoc.processor.model.enums.BatchStatus;
import com.bankpoc.processor.repository.BatchPartRepository;
import com.bankpoc.processor.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final BatchPartRepository batchPartRepository;
    private final PaymentOrchestrator paymentOrchestrator;
    private final AuditService auditService;

    public BatchStatusResponse ingestBatch(BatchIngestRequest request) {
        log.info("Ingesting batch {}", request.getBatchId());
        
        Batch batch = Batch.builder()
                .batchId(request.getBatchId())
                .status(BatchStatus.ASSEMBLING)
                .totalTransactions(0)
                .successfulTransactions(0)
                .failedTransactions(0)
                .totalAmount(BigDecimal.ZERO)
                .build();
        
        batch = batchRepository.save(batch);

        int partNumber = 1;
        for (String filePath : request.getFilePaths()) {
            BatchPart part = BatchPart.builder()
                    .batchId(request.getBatchId())
                    .partNumber(partNumber++)
                    .filePath(filePath)
                    .recordCount(0) // Will be updated during parsing
                    .build();
            batchPartRepository.save(part);
        }

        auditService.logStateChange("BATCH", request.getBatchId(), "INGEST", null, BatchStatus.ASSEMBLING.name(), null);
        
        // Trigger async processing
        paymentOrchestrator.processBatch(request.getBatchId());
        
        return mapToResponse(batch);
    }

    public BatchStatusResponse getBatchStatus(String batchId) {
        Batch batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));
        return mapToResponse(batch);
    }

    public List<BatchStatusResponse> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBatchStatus(String batchId, BatchStatus newStatus, String errorMessage) {
        Batch batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));
        
        String oldStatus = batch.getStatus().name();
        batch.setStatus(newStatus);
        
        if (errorMessage != null) {
            batch.setErrorMessage(errorMessage);
        }
        
        if (newStatus == BatchStatus.COMPLETED || newStatus == BatchStatus.PARTIAL_SUCCESS || newStatus == BatchStatus.FAILED) {
            batch.setCompletedAt(java.time.LocalDateTime.now());
        }
        
        batchRepository.save(batch);
        auditService.logStateChange("BATCH", batchId, "STATUS_UPDATE", oldStatus, newStatus.name(), null);
    }

    private BatchStatusResponse mapToResponse(Batch batch) {
        return BatchStatusResponse.builder()
                .batchId(batch.getBatchId())
                .status(batch.getStatus())
                .totalTransactions(batch.getTotalTransactions())
                .successfulTransactions(batch.getSuccessfulTransactions())
                .failedTransactions(batch.getFailedTransactions())
                .pendingTransactions(batch.getTotalTransactions() - batch.getSuccessfulTransactions() - batch.getFailedTransactions())
                .totalAmount(batch.getTotalAmount())
                .successfulAmount(BigDecimal.ZERO) // Simplified for POC
                .failedAmount(BigDecimal.ZERO)
                .successRate(batch.getSuccessRate())
                .createdAt(batch.getCreatedAt())
                .completedAt(batch.getCompletedAt())
                .errorMessage(batch.getErrorMessage())
                .build();
    }
}
