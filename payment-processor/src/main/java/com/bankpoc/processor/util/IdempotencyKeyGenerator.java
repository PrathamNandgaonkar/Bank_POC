package com.bankpoc.processor.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class IdempotencyKeyGenerator {
    
    public String generateKey(String transactionId, String senderAccount, String receiverAccount, BigDecimal amount) {
        String data = transactionId + "|" + senderAccount + "|" + receiverAccount + "|" + amount.toPlainString();
        return DigestUtils.sha256Hex(data);
    }
}
