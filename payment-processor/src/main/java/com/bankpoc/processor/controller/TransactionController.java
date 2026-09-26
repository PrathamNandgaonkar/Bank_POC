package com.bankpoc.processor.controller;

import com.bankpoc.processor.dto.TransactionStatusResponse;
import com.bankpoc.processor.model.entity.Transaction;
import com.bankpoc.processor.model.enums.FailureCategory;
import com.bankpoc.processor.repository.TransactionRepository;
import com.bankpoc.processor.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionStatusResponse> getTransactionStatus(@PathVariable String transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionStatus(transactionId));
    }

    @GetMapping("/failures")
    public ResponseEntity<List<Transaction>> getFailures(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) FailureCategory category) {
        
        List<Transaction> failures;
        if (category != null) {
            failures = transactionRepository.findByFailureCategory(category);
        } else if (batchId != null) {
            failures = transactionRepository.findByBatchId(batchId).stream()
                    .filter(t -> t.getFailureCategory() != null)
                    .collect(Collectors.toList());
        } else {
            failures = transactionRepository.findAll().stream()
                    .filter(t -> t.getFailureCategory() != null)
                    .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(failures);
    }
}
