package com.bankpoc.processor.controller;

import com.bankpoc.processor.dto.BatchIngestRequest;
import com.bankpoc.processor.dto.BatchStatusResponse;
import com.bankpoc.processor.dto.TransactionStatusResponse;
import com.bankpoc.processor.service.BatchService;
import com.bankpoc.processor.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final TransactionService transactionService;

    @PostMapping("/ingest")
    public ResponseEntity<BatchStatusResponse> ingestBatch(@RequestBody BatchIngestRequest request) {
        return ResponseEntity.accepted().body(batchService.ingestBatch(request));
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<BatchStatusResponse> getBatchStatus(@PathVariable String batchId) {
        return ResponseEntity.ok(batchService.getBatchStatus(batchId));
    }

    @GetMapping
    public ResponseEntity<List<BatchStatusResponse>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @GetMapping("/{batchId}/transactions")
    public ResponseEntity<List<TransactionStatusResponse>> getTransactionsForBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(transactionService.getTransactionsByBatch(batchId));
    }
}
