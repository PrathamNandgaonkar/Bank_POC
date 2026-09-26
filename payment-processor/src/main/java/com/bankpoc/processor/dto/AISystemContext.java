package com.bankpoc.processor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AISystemContext {
    private String systemHealth;
    private int activeProcessingBatches;
    private int totalBatchesToday;
    private double successRateToday;
    private double failureRateToday;
    private Map<String, Integer> topFailureCategories;
    private String npciServiceStatus;
    private String databaseStatus;
    private LocalDateTime lastProcessedAt;
    private String systemUptime;
}
