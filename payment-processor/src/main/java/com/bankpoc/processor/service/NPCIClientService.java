package com.bankpoc.processor.service;

import com.bankpoc.processor.dto.NPCIPaymentRequest;
import com.bankpoc.processor.dto.NPCIPaymentResponse;
import com.bankpoc.processor.exception.NPCIServiceException;
import com.bankpoc.processor.model.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NPCIClientService {

    private final RestTemplate restTemplate;

    @Value("${app.npci.base-url}")
    private String npciBaseUrl;

    public NPCIPaymentResponse processPayment(Transaction txn) {
        NPCIPaymentRequest request = NPCIPaymentRequest.builder()
                .transactionId(txn.getTransactionId())
                .senderAccount(txn.getSenderAccount())
                .senderIfsc(txn.getSenderIfsc())
                .receiverAccount(txn.getReceiverAccount())
                .receiverIfsc(txn.getReceiverIfsc())
                .amount(txn.getAmount())
                .currency("INR")
                .paymentMode(txn.getPaymentMode().name())
                .narration(txn.getNarration())
                .batchId(txn.getBatchId())
                .idempotencyKey(txn.getIdempotencyKey())
                .build();

        try {
            return restTemplate.postForObject(npciBaseUrl + "/api/npci/v1/payments/process", request, NPCIPaymentResponse.class);
        } catch (Exception e) {
            log.error("Error communicating with NPCI for transaction {}", txn.getTransactionId(), e);
            throw new NPCIServiceException("Failed to communicate with NPCI", e);
        }
    }
}
