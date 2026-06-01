package com.balic.newbusiness.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immediate acknowledgement response sent to the partner.
 * JDK 21 record — immutable; Jackson serializes it via its components.
 */
@Schema(description = "Immediate acknowledgement response sent to partner")
public record NotificationResponse(
        @Schema(description = "Unique ID to track this request end-to-end",
                example = "550e8400-e29b-41d4-a716-446655440000") String correlationId,
        @Schema(description = "status message",
                example = "Request received successfully. Application number will be sent via reverse feed.") String message) {
}
