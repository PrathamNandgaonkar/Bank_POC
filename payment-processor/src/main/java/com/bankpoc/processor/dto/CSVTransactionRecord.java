package com.bankpoc.processor.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CSVTransactionRecord {
    
    @CsvBindByName(column = "transaction_id")
    private String transactionId;
    
    @CsvBindByName(column = "sender_account")
    private String senderAccount;
    
    @CsvBindByName(column = "sender_ifsc")
    private String senderIfsc;
    
    @CsvBindByName(column = "sender_name")
    private String senderName;
    
    @CsvBindByName(column = "receiver_account")
    private String receiverAccount;
    
    @CsvBindByName(column = "receiver_ifsc")
    private String receiverIfsc;
    
    @CsvBindByName(column = "receiver_name")
    private String receiverName;
    
    @CsvBindByName(column = "amount")
    private BigDecimal amount;
    
    @CsvBindByName(column = "payment_mode")
    private String paymentMode;
    
    @CsvBindByName(column = "narration")
    private String narration;
}
