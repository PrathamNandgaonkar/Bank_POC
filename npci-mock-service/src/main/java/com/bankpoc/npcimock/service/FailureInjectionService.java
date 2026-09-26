package com.bankpoc.npcimock.service;

import com.bankpoc.npcimock.config.SimulationConfig;
import com.bankpoc.npcimock.model.FailureConfig;
import com.bankpoc.npcimock.model.SimulationStats;
import org.springframework.stereotype.Service;

@Service
public class FailureInjectionService {

    private final SimulationConfig simulationConfig;
    private FailureConfig currentConfig;
    private SimulationStats stats;

    public FailureInjectionService(SimulationConfig simulationConfig) {
        this.simulationConfig = simulationConfig;
        this.currentConfig = simulationConfig.defaultFailureConfig();
        this.stats = new SimulationStats();
    }

    public FailureConfig getConfig() {
        return currentConfig;
    }

    public void updateConfig(FailureConfig config) {
        this.currentConfig = config;
    }

    public void resetConfig() {
        this.currentConfig = simulationConfig.defaultFailureConfig();
    }

    public void enableChaosMode() {
        currentConfig.setEnabled(true);
        currentConfig.setFailureRate(0.3);
        currentConfig.setTimeoutRate(0.1);
    }

    public void enableUnavailableMode() {
        currentConfig.setEnabled(true);
        currentConfig.setUnavailableMode(true);
    }

    public void disableAllFailures() {
        currentConfig.setEnabled(false);
        currentConfig.setUnavailableMode(false);
        currentConfig.setFailureRate(0.0);
        currentConfig.setTimeoutRate(0.0);
        currentConfig.getSpecificAccountFailures().clear();
        currentConfig.getSpecificIfscFailures().clear();
    }

    public void addAccountFailure(String account, String reason) {
        currentConfig.getSpecificAccountFailures().put(account, reason);
        currentConfig.setEnabled(true);
    }

    public void removeAccountFailure(String account) {
        currentConfig.getSpecificAccountFailures().remove(account);
    }

    public SimulationStats getStats() {
        return stats;
    }

    public void resetStats() {
        this.stats = new SimulationStats();
    }
}
