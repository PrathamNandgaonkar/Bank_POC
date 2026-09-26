package com.bankpoc.npcimock.service;

import com.bankpoc.npcimock.exception.ServiceUnavailableException;
import com.bankpoc.npcimock.model.FailureConfig;
import com.bankpoc.npcimock.model.NPCIResponseCode;
import com.bankpoc.npcimock.model.PaymentRequest;
import com.bankpoc.npcimock.model.PaymentResponse;
import com.bankpoc.npcimock.model.SimulationStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class PaymentSimulatorService {

    private final FailureInjectionService failureInjectionService;
    private final Random random = new Random();
    
    // LRU Cache for idempotency keys
    private final Map<String, Boolean> processedTransactions = Collections.synchronizedMap(
        new LinkedHashMap<String, Boolean>(10000, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > 10000;
            }
        });

    public PaymentSimulatorService(FailureInjectionService failureInjectionService) {
        this.failureInjectionService = failureInjectionService;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        long startTime = System.currentTimeMillis();
        FailureConfig config = failureInjectionService.getConfig();
        SimulationStats stats = failureInjectionService.getStats();
        
        stats.getTotalRequests().incrementAndGet();
        stats.setLastRequestAt(LocalDateTime.now());
        
        log.info("Processing payment request: {}", request.getTransactionId());

        try {
            if (config.isUnavailableMode()) {
                throw new ServiceUnavailableException("NPCI Service is currently unavailable");
            }

            if (config.isDuplicateDetection() && processedTransactions.putIfAbsent(request.getIdempotencyKey(), true) != null) {
                return createResponse(request, NPCIResponseCode.DUPLICATE_TRANSACTION, "FAILED", startTime, stats);
            }

            simulateProcessingDelay(config, request.getPaymentMode());

            NPCIResponseCode specificFailure = checkSpecificFailures(request, config);
            if (specificFailure != null) {
                return createResponse(request, specificFailure, "FAILED", startTime, stats);
            }

            if (config.isEnabled()) {
                double rand = random.nextDouble();
                if (rand < config.getTimeoutRate()) {
                    simulateTimeout(config);
                    return createResponse(request, NPCIResponseCode.TIMEOUT, "TIMEOUT", startTime, stats);
                } else if (rand < config.getTimeoutRate() + config.getFailureRate()) {
                    return createResponse(request, getRandomFailureCode(), "FAILED", startTime, stats);
                }
            }

            return createResponse(request, NPCIResponseCode.SUCCESS, "SUCCESS", startTime, stats);
        } catch (ServiceUnavailableException e) {
            stats.getFailedRequests().incrementAndGet();
            stats.recordResponseCode(NPCIResponseCode.SERVICE_UNAVAILABLE.getCode());
            throw e;
        } catch (Exception e) {
            log.error("Internal processing error for transaction {}", request.getTransactionId(), e);
            return createResponse(request, NPCIResponseCode.PROCESSING_ERROR, "FAILED", startTime, stats);
        }
    }

    private void simulateProcessingDelay(FailureConfig config, String paymentMode) throws InterruptedException {
        long min = config.getMinProcessingDelayMs();
        long max = config.getMaxProcessingDelayMs();
        
        if ("RTGS".equalsIgnoreCase(paymentMode)) {
            min = Math.max(50, min);
            max = Math.min(500, max);
        } else if ("IMPS".equalsIgnoreCase(paymentMode)) {
            min = Math.max(50, min);
            max = Math.min(300, max);
        } else if ("NEFT".equalsIgnoreCase(paymentMode)) {
            min = Math.max(200, min);
            max = Math.min(1500, max);
        }
        
        if (min >= max) max = min + 100;
        
        long delay = ThreadLocalRandom.current().nextLong(min, max);
        Thread.sleep(delay);
    }

    private void simulateTimeout(FailureConfig config) throws InterruptedException {
        Thread.sleep(config.getTimeoutDelayMs());
    }

    private NPCIResponseCode checkSpecificFailures(PaymentRequest request, FailureConfig config) throws InterruptedException {
        String receiverAccount = request.getReceiverAccount();
        if (receiverAccount != null) {
            if (receiverAccount.startsWith("999")) return NPCIResponseCode.ACCOUNT_NOT_FOUND;
            if (receiverAccount.startsWith("888")) return NPCIResponseCode.ACCOUNT_CLOSED;
            if (receiverAccount.startsWith("777")) return NPCIResponseCode.INSUFFICIENT_FUNDS;
            
            if (config.getSpecificAccountFailures().containsKey(receiverAccount)) {
                String reason = config.getSpecificAccountFailures().get(receiverAccount);
                return getCodeFromName(reason, NPCIResponseCode.PROCESSING_ERROR);
            }
        }

        if ("FAIL0000001".equals(request.getSenderIfsc())) return NPCIResponseCode.RECEIVER_BANK_UNAVAILABLE;
        if ("FAIL0000001".equals(request.getReceiverIfsc())) return NPCIResponseCode.INVALID_IFSC;

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(new BigDecimal("99999.99")) == 0) {
                simulateTimeout(config);
                return NPCIResponseCode.TIMEOUT;
            }
            if (request.getAmount().compareTo(new BigDecimal("1000000")) > 0) return NPCIResponseCode.AMOUNT_LIMIT_EXCEEDED;
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) return NPCIResponseCode.PROCESSING_ERROR;
        }

        return null;
    }

    private NPCIResponseCode getRandomFailureCode() {
        NPCIResponseCode[] codes = {
            NPCIResponseCode.NETWORK_ERROR, 
            NPCIResponseCode.SERVICE_UNAVAILABLE, 
            NPCIResponseCode.RECEIVER_BANK_UNAVAILABLE
        };
        return codes[random.nextInt(codes.length)];
    }

    private NPCIResponseCode getCodeFromName(String name, NPCIResponseCode def) {
        try {
            return NPCIResponseCode.valueOf(name);
        } catch (Exception e) {
            return def;
        }
    }

    private PaymentResponse createResponse(PaymentRequest request, NPCIResponseCode code, String status, long startTime, SimulationStats stats) {
        long duration = System.currentTimeMillis() - startTime;
        
        if ("SUCCESS".equals(status)) {
            stats.getSuccessfulRequests().incrementAndGet();
        } else if ("TIMEOUT".equals(status)) {
            stats.getTimedOutRequests().incrementAndGet();
        } else {
            stats.getFailedRequests().incrementAndGet();
        }
        
        stats.recordResponseCode(code.getCode());
        stats.updateAverageProcessingTime(duration);

        String npciRefId = "NPCI" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%06d", random.nextInt(1000000));
        
        PaymentResponse response = PaymentResponse.builder()
                .transactionId(request.getTransactionId())
                .npciReferenceId(npciRefId)
                .status(status)
                .responseCode(code.getCode())
                .responseMessage(code.getMessage())
                .processedAt(LocalDateTime.now())
                .processingDurationMs(duration)
                .build();
                
        log.info("Payment response generated: txId={}, status={}, code={}, duration={}ms", 
                request.getTransactionId(), status, code.getCode(), duration);
                
        return response;
    }
}
