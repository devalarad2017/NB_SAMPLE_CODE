package com.balic.newbusiness.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Error response structure returned on all failures.
 * JDK 21 record — immutable; Jackson serializes it via its components.
 */
@Schema(description = "Error response structure returned on all failures")
public record ErrorResponse(
        @Schema(description = "Error code", example = "MAPPING_ERROR") String errorCode,
        @Schema(description = "Error description") String message,
        @Schema(description = "Timestamp when the error occurred") LocalDateTime timestamp) {

    /** Convenience constructor — stamps the current time, matching previous behaviour. */
    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, LocalDateTime.now());
    }
}
