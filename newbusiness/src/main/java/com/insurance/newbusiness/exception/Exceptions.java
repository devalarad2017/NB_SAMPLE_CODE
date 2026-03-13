package com.insurance.newbusiness.exception;

import com.insurance.newbusiness.domain.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// Exception hierarchy — all in one file to reduce class count.
//
//   NewBusinessException              base, extends RuntimeException
//     MappingValidationException      mandatory fields missing — collects ALL missing at once
//     ApiCallException                downstream API failed — triggers @Retryable
//     JourneyStageException           all retries exhausted — stops the journey
//     PartnerConfigException          partner not in DB or no notifier registered
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────

public class MappingValidationException extends NewBusinessException {
    private final List<String> missingFields;
    public MappingValidationException(String targetApi, List<String> missingFields) {
        super("MAPPING_ERROR",
              "Mandatory fields missing for [" + targetApi + "]: " + missingFields);
        this.missingFields = missingFields;
    }
    public List<String> getMissingFields() { return missingFields; }
}

// ─────────────────────────────────────────────────────────────────────────────

public class ApiCallException extends NewBusinessException {
    private final String apiName;
    public ApiCallException(String apiName, String message, Throwable cause) {
        super("API_CALL_ERROR", "[" + apiName + "] " + message, cause);
        this.apiName = apiName;
    }
    public String getApiName() { return apiName; }
}

// ─────────────────────────────────────────────────────────────────────────────

public class JourneyStageException extends NewBusinessException {
    private final String stageName;
    public JourneyStageException(String stageName, String message) {
        super("JOURNEY_STAGE_ERROR", "Stage [" + stageName + "] failed: " + message);
        this.stageName = stageName;
    }
    public String getStageName() { return stageName; }
}

// ─────────────────────────────────────────────────────────────────────────────

public class PartnerConfigException extends NewBusinessException {
    public PartnerConfigException(String message) {
        super("PARTNER_CONFIG_ERROR", message);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GlobalExceptionHandler — catches all exceptions before they reach the partner.
// NOTE: Journey async exceptions do NOT reach here — caught in NewBusinessService.
// ─────────────────────────────────────────────────────────────────────────────

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MappingValidationException.class)
    public ResponseEntity<ErrorResponse> handleMapping(MappingValidationException ex) {
        log.error("Mapping validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(JourneyStageException.class)
    public ResponseEntity<ErrorResponse> handleJourneyStage(JourneyStageException ex) {
        log.error("Journey stage failed | stage={} | {}", ex.getStageName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(PartnerConfigException.class)
    public ResponseEntity<ErrorResponse> handlePartnerConfig(PartnerConfigException ex) {
        log.error("Partner config error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
