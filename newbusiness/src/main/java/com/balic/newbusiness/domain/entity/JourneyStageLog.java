package com.balic.newbusiness.domain.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit log — one row per API call attempt.
 * Never updated. Only inserted.
 * attemptNumber increments on retry so you can see all attempts for one correlationId + apiName. (Appno+apiname)
 */
@Data
@Entity
@Table(name = "journey_stage_log")
public class JourneyStageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "correlation_id")
    private String correlationId;  // technical/code-tracking id
    // Business tracking key (partner-supplied application number). Stamped on every
    // row so the whole journey can be searched/tracked by application number from the UI.
    @Column(name = "application_number")
    private String applicationNumber;
    @Column(name = "stage_name")
    private String stageName;
    @Column(name = "api_name")
    private String apiName;
    @Column(name = "attempt_number")
    private int attemptNumber = 1;
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;
    @Column(name = "status" )
    private String status;          // SUCCESS | FAILED | RETRYING
    @Column(name = "error_code")
    private String errorCode;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
