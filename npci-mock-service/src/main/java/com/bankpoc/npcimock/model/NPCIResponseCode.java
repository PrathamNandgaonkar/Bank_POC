package com.bankpoc.npcimock.model;

public enum NPCIResponseCode {
    SUCCESS("00", "Transaction successful"),
    INSUFFICIENT_FUNDS("E01", "Insufficient funds in sender account"),
    ACCOUNT_NOT_FOUND("E02", "Beneficiary account not found"),
    ACCOUNT_CLOSED("E03", "Account closed or blocked"),
    INVALID_IFSC("E04", "Invalid IFSC code"),
    AMOUNT_LIMIT_EXCEEDED("E05", "Transaction amount exceeds allowed limit"),
    DUPLICATE_TRANSACTION("E06", "Duplicate transaction detected"),
    SENDER_ACCOUNT_BLOCKED("E07", "Sender account is blocked"),
    RECEIVER_BANK_UNAVAILABLE("E08", "Receiver bank system unavailable"),
    TIMEOUT("T01", "Transaction processing timed out"),
    SERVICE_UNAVAILABLE("T02", "NPCI service temporarily unavailable"),
    PROCESSING_ERROR("T03", "Internal processing error"),
    NETWORK_ERROR("T04", "Network communication error");

    private final String code;
    private final String message;

    NPCIResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
