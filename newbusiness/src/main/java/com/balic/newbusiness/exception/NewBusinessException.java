package com.balic.newbusiness.exception;

public class NewBusinessException extends RuntimeException {
    private final String errorCode;
    public NewBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public NewBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
