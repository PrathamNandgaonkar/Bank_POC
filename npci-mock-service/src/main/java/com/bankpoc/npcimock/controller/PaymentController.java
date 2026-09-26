package com.bankpoc.npcimock.controller;

import com.bankpoc.npcimock.model.PaymentRequest;
import com.bankpoc.npcimock.model.PaymentResponse;
import com.bankpoc.npcimock.service.FailureInjectionService;
import com.bankpoc.npcimock.service.PaymentSimulatorService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/npci/v1")
public class PaymentController {

    private final PaymentSimulatorService simulatorService;
    private final FailureInjectionService failureInjectionService;

    public PaymentController(PaymentSimulatorService simulatorService, FailureInjectionService failureInjectionService) {
        this.simulatorService = simulatorService;
        this.failureInjectionService = failureInjectionService;
    }

    @PostMapping("/payments/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        String traceId = UUID.randomUUID().toString();
        try {
            MDC.put("traceId", traceId);
            MDC.put("transactionId", request.getTransactionId());
            if (request.getBatchId() != null) {
                MDC.put("batchId", request.getBatchId());
            }
            MDC.put("simulationMode", failureInjectionService.getConfig().isUnavailableMode() ? "UNAVAILABLE" : 
                    (failureInjectionService.getConfig().isEnabled() ? "CHAOS" : "NORMAL"));
            
            PaymentResponse response = simulatorService.processPayment(request);
            return ResponseEntity.ok(response);
            
        } finally {
            MDC.clear();
        }
    }
}
