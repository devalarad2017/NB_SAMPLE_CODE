package com.balic.newbusiness.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Error response structure returned on all failures")
public class ErrorResponse {

    @Schema(description = "Error code", example = "MAPPING_ERROR")
    private final String errorCode;

    @Schema(description = "Error description")
    private final String message;

    @Schema(description = "Timestamp when the error occurred")
    private final LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() { 
    	return errorCode; 
    }
    public String getMessage() { 
    	return message; 
    }
    public LocalDateTime getTimestamp() {
    	return timestamp; 
    }
}