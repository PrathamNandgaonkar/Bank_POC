package com.bankpoc.processor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AIBatchSummary {
    private String batchId;
    private String status;
    private int totalTransactions;
    private int successCount;
    private int failureCount;
    private int pendingCount;
    private BigDecimal totalAmount;
    private Map<String, Integer> failureBreakdown;
    private long avgProcessingTimeMs;
    private List<String> commonFailureReasons;
    private List<AITimelineEvent> processingTimeline;
}
