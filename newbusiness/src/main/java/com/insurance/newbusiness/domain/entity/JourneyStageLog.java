package com.insurance.newbusiness.domain.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable audit log — one row per API call attempt.
 * Never updated. Only inserted.
 * attemptNumber increments on retry so you can see all attempts for one correlationId + apiName.
 */
@Data
@Entity
@Table(name = "journey_stage_log")
public class JourneyStageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;
    @Column(name = "stage_name", length = 50)
    private String stageName;
    @Column(name = "api_name", length = 50)
    private String apiName;
    @Column(name = "attempt_number")
    private int attemptNumber = 1;
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;
    @Column(name = "status", length = 20)
    private String status;          // SUCCESS | FAILED | RETRYING
    @Column(name = "error_code", length = 50)
    private String errorCode;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
