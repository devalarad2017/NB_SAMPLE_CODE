package com.balic.newbusiness.exception;

public class ApiCallException extends NewBusinessException {
    private final String apiName;
    public ApiCallException(String apiName, String message, Throwable cause) {
        super("API_CALL_ERROR", "[" + apiName + "] " + message, cause);
        this.apiName = apiName;
    }
    public String getApiName() { return apiName; }
}
