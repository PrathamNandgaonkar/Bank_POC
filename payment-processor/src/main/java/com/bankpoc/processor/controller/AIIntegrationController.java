package com.bankpoc.processor.controller;

import com.bankpoc.processor.dto.AIBatchSummary;
import com.bankpoc.processor.dto.AIFailureDetail;
import com.bankpoc.processor.dto.AISystemContext;
import com.bankpoc.processor.service.AIDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIIntegrationController {

    private final AIDataService aiDataService;

    @GetMapping("/batches/summary")
    public ResponseEntity<Map<String, Object>> getBatchSummaries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        
        List<AIBatchSummary> summaries = aiDataService.getBatchSummaries(from, to);
        return ResponseEntity.ok(wrapWithMetadata(summaries));
    }

    @GetMapping("/batches/{batchId}/analysis")
    public ResponseEntity<Map<String, Object>> getBatchAnalysis(@PathVariable String batchId) {
        return ResponseEntity.ok(wrapWithMetadata(aiDataService.getBatchAnalysis(batchId)));
    }

    @GetMapping("/failures/patterns")
    public ResponseEntity<Map<String, Object>> getFailurePatterns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(wrapWithMetadata(aiDataService.getFailurePatterns(from, to)));
    }

    @GetMapping("/failures/details")
    public ResponseEntity<Map<String, Object>> getFailureDetails(
            @RequestParam String batchId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<AIFailureDetail> details = aiDataService.getTransactionFailures(batchId);
        return ResponseEntity.ok(wrapWithMetadata(details));
    }

    @GetMapping("/system/context")
    public ResponseEntity<Map<String, Object>> getSystemContext() {
        return ResponseEntity.ok(wrapWithMetadata(aiDataService.getSystemContext()));
    }

    @GetMapping("/export/batch/{batchId}")
    public ResponseEntity<Map<String, Object>> exportBatchForLLM(@PathVariable String batchId) {
        Map<String, Object> export = new HashMap<>();
        export.put("summary", aiDataService.getBatchAnalysis(batchId));
        export.put("failures", aiDataService.getTransactionFailures(batchId));
        return ResponseEntity.ok(wrapWithMetadata(export));
    }

    private Map<String, Object> wrapWithMetadata(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("requestTimestamp", LocalDateTime.now());
        response.put("dataFreshness", "REALTIME");
        
        if (data instanceof List<?>) {
            response.put("recordCount", ((List<?>) data).size());
        } else {
            response.put("recordCount", 1);
        }
        
        response.put("data", data);
        return response;
    }
}
