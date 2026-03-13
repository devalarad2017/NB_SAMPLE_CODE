package com.insurance.newbusiness.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Immediate acknowledgement response sent to partner")
public class NotificationResponse {

    @Schema(description = "Unique ID to track this request end-to-end",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;

    @Schema(description = "Human-readable status message",
            example = "Request received successfully. Application number will be sent via reverse feed.")
    private String message;

    public NotificationResponse(String correlationId, String message) {
        this.correlationId = correlationId;
        this.message = message;
    }

    public String getCorrelationId() { return correlationId; }
    public String getMessage() { return message; }
}
