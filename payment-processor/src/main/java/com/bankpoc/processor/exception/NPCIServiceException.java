package com.bankpoc.processor.exception;

public class NPCIServiceException extends RuntimeException {
    public NPCIServiceException(String message) {
        super(message);
    }
    public NPCIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
