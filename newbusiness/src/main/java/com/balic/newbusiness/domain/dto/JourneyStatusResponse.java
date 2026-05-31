package com.balic.newbusiness.domain.dto;

import com.balic.newbusiness.domain.entity.JourneyExecution;
import com.balic.newbusiness.domain.entity.JourneyStageLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JourneyStatusResponse — read model returned to the UI for one application/journey.
 *
 * Tells the operator the overall status, exactly which stage/API the journey stopped
 * at (and why), the generated application number once available, and the full
 * per-attempt stage history so a retry decision can be made.
 */
public record JourneyStatusResponse(
        String correlationId,
        String partnerCode,
        String overallStatus,
        String failedStageName,
        String failedApiName,
        String failureReason,
        String applicationNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<StageEntry> stages) {

    /** One stage-log attempt, flattened for display. */
    public record StageEntry(
            String stageName,
            String apiName,
            int attemptNumber,
            String status,
            String errorCode,
            String errorMessage,
            Long durationMs,
            LocalDateTime executedAt) {
    }

    /** Builds the response from the persisted execution row and its stage logs. */
    public static JourneyStatusResponse from(JourneyExecution execution, List<JourneyStageLog> logs) {
        List<StageEntry> stageEntries = logs.stream()
                .map(l -> new StageEntry(
                        l.getStageName(),
                        l.getApiName(),
                        l.getAttemptNumber(),
                        l.getStatus(),
                        l.getErrorCode(),
                        l.getErrorMessage(),
                        l.getDurationMs(),
                        l.getExecutedAt()))
                .toList();

        return new JourneyStatusResponse(
                execution.getCorrelationId(),
                execution.getPartnerCode(),
                execution.getOverallStatus(),
                execution.getFailedStageName(),
                execution.getFailedApiName(),
                execution.getFailureReason(),
                execution.getApplicationNumber(),
                execution.getCreatedAt(),
                execution.getUpdatedAt(),
                stageEntries);
    }
}
