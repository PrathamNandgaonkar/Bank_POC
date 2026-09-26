package com.bankpoc.npcimock.controller;

import com.bankpoc.npcimock.model.FailureConfig;
import com.bankpoc.npcimock.model.SimulationStats;
import com.bankpoc.npcimock.service.FailureInjectionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/npci/v1/simulation")
public class SimulationController {

    private final FailureInjectionService failureInjectionService;

    public SimulationController(FailureInjectionService failureInjectionService) {
        this.failureInjectionService = failureInjectionService;
    }

    @GetMapping("/config")
    public FailureConfig getConfig() {
        return failureInjectionService.getConfig();
    }

    @PutMapping("/config")
    public FailureConfig updateConfig(@RequestBody FailureConfig config) {
        failureInjectionService.updateConfig(config);
        return failureInjectionService.getConfig();
    }

    @PostMapping("/reset")
    public String reset() {
        failureInjectionService.resetConfig();
        failureInjectionService.resetStats();
        return "Reset successful";
    }

    @PostMapping("/chaos")
    public FailureConfig enableChaosMode() {
        failureInjectionService.enableChaosMode();
        return failureInjectionService.getConfig();
    }

    @PostMapping("/unavailable")
    public FailureConfig enableUnavailableMode() {
        failureInjectionService.enableUnavailableMode();
        return failureInjectionService.getConfig();
    }

    @PostMapping("/recover")
    public FailureConfig disableAllFailures() {
        failureInjectionService.disableAllFailures();
        return failureInjectionService.getConfig();
    }

    @GetMapping("/stats")
    public SimulationStats getStats() {
        return failureInjectionService.getStats();
    }

    @PostMapping("/account-failure")
    public FailureConfig addAccountFailure(@RequestBody Map<String, String> body) {
        failureInjectionService.addAccountFailure(body.get("account"), body.get("reason"));
        return failureInjectionService.getConfig();
    }

    @DeleteMapping("/account-failure/{account}")
    public FailureConfig removeAccountFailure(@PathVariable String account) {
        failureInjectionService.removeAccountFailure(account);
        return failureInjectionService.getConfig();
    }
}
