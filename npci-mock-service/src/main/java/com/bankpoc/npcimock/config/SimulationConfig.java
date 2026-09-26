package com.bankpoc.npcimock.config;

import com.bankpoc.npcimock.model.FailureConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "npci.simulation")
public class SimulationConfig {
    
    private long minProcessingDelayMs = 100;
    private long maxProcessingDelayMs = 2000;
    private double defaultFailureRate = 0.0;
    private double defaultTimeoutRate = 0.0;
    private boolean duplicateDetection = true;
    private long maxAmountLimit = 1000000;

    @Bean
    public FailureConfig defaultFailureConfig() {
        FailureConfig config = new FailureConfig();
        config.setMinProcessingDelayMs(minProcessingDelayMs);
        config.setMaxProcessingDelayMs(maxProcessingDelayMs);
        config.setFailureRate(defaultFailureRate);
        config.setTimeoutRate(defaultTimeoutRate);
        config.setDuplicateDetection(duplicateDetection);
        return config;
    }

    // Getters and Setters
    public long getMinProcessingDelayMs() { return minProcessingDelayMs; }
    public void setMinProcessingDelayMs(long minProcessingDelayMs) { this.minProcessingDelayMs = minProcessingDelayMs; }
    public long getMaxProcessingDelayMs() { return maxProcessingDelayMs; }
    public void setMaxProcessingDelayMs(long maxProcessingDelayMs) { this.maxProcessingDelayMs = maxProcessingDelayMs; }
    public double getDefaultFailureRate() { return defaultFailureRate; }
    public void setDefaultFailureRate(double defaultFailureRate) { this.defaultFailureRate = defaultFailureRate; }
    public double getDefaultTimeoutRate() { return defaultTimeoutRate; }
    public void setDefaultTimeoutRate(double defaultTimeoutRate) { this.defaultTimeoutRate = defaultTimeoutRate; }
    public boolean isDuplicateDetection() { return duplicateDetection; }
    public void setDuplicateDetection(boolean duplicateDetection) { this.duplicateDetection = duplicateDetection; }
    public long getMaxAmountLimit() { return maxAmountLimit; }
    public void setMaxAmountLimit(long maxAmountLimit) { this.maxAmountLimit = maxAmountLimit; }
}
