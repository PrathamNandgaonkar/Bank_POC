package com.bankpoc.npcimock.model;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class FailureConfig {
    private boolean enabled = false;
    private double failureRate = 0.0;
    private double timeoutRate = 0.0;
    private long timeoutDelayMs = 35000;
    private boolean unavailableMode = false;
    private Map<String, String> specificAccountFailures = new ConcurrentHashMap<>();
    private Map<String, String> specificIfscFailures = new ConcurrentHashMap<>();
    private long minProcessingDelayMs = 100;
    private long maxProcessingDelayMs = 2000;
    private boolean duplicateDetection = true;
}
