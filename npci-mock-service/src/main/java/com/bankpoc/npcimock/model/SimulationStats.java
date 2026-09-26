package com.bankpoc.npcimock.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class SimulationStats {
    private AtomicLong totalRequests = new AtomicLong(0);
    private AtomicLong successfulRequests = new AtomicLong(0);
    private AtomicLong failedRequests = new AtomicLong(0);
    private AtomicLong timedOutRequests = new AtomicLong(0);
    private volatile double averageProcessingTimeMs = 0.0;
    private LocalDateTime upSince = LocalDateTime.now();
    private volatile LocalDateTime lastRequestAt;
    private Map<String, Long> responseCodeDistribution = new ConcurrentHashMap<>();

    public synchronized void updateAverageProcessingTime(long newDuration) {
        long currentTotal = totalRequests.get();
        if (currentTotal == 1) {
            averageProcessingTimeMs = newDuration;
        } else {
            averageProcessingTimeMs = ((averageProcessingTimeMs * (currentTotal - 1)) + newDuration) / currentTotal;
        }
    }

    public void recordResponseCode(String code) {
        responseCodeDistribution.merge(code, 1L, Long::sum);
    }
}
