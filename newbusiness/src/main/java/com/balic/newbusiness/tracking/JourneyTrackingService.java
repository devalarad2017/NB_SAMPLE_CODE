package com.balic.newbusiness.tracking;

import com.balic.newbusiness.domain.entity.JourneyExecution;
import com.balic.newbusiness.domain.entity.JourneyStageLog;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.repository.JourneyExecutionRepository;
import com.balic.newbusiness.repository.JourneyStageLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

/**
 * JourneyTrackingService — all writes to journey_execution and journey_stage_log.
 *
 * ── CRITICAL RULE ─────────────────────────────────────────────────────────────
 * Every public method catches its own exceptions internally.
 * Tracking must NEVER stop or affect the journey processing.
 * If a DB write fails here, it logs the error to file and returns silently.
 * The journey continues regardless.
 *
 * ── RETRY RESUME ─────────────────────────────────────────────────────────────
 * getSucceededApiNames() is the foundation of the retry resume feature.
 * JourneyOrchestrator calls this at the start of every execute() call.
 * Any apiName in the returned set will be skipped in that run.
 *
 * ── journey_stage_log IS IMMUTABLE ────────────────────────────────────────────
 * logApiCall() only inserts — never updates.
 * Every retry attempt creates a new row with a fresh status.
 * This means you can see the full retry history for any API call.
 *
 * ── LOGGING FORMAT ───────────────────────────────────────────────────────────
 * Both request and response objects are serialised to JSON string for storage.

 */
@Service
@RequiredArgsConstructor
public class JourneyTrackingService {

    private static final Logger log = LoggerFactory.getLogger(JourneyTrackingService.class);

    private final JourneyExecutionRepository executionRepository;
    private final JourneyStageLogRepository  stageLogRepository;
    private final ObjectMapper               objectMapper;

    /**
     * Creates the initial journey_execution record when a new request arrives.
     * Called once from NewBusinessService after raw request is stored.
     */
    public void initJourney(JourneyContext context, Long rawRequestId) {
        try {
            JourneyExecution execution = new JourneyExecution();
            execution.setCorrelationId(context.getCorrelationId());
            execution.setPartnerCode(context.getPartnerCode());
            execution.setRawRequestId(rawRequestId);
            execution.setOverallStatus("IN_PROGRESS");
            executionRepository.save(execution);
        } catch (Exception ex) {
            log.error("[{}] Failed to init journey_execution: {}",
                    context.getCorrelationId(), ex.getMessage());
        }
    }

    /**
     * Returns set of api_names that already have status=SUCCESS in journey_stage_log.
     * JourneyOrchestrator uses this to skip already-completed stages on retry.
     * Returns empty set on first run or if DB query fails.
     */
    public Set<String> getSucceededApiNames(String correlationId) {
        try {
            Set<String> names = stageLogRepository.findSucceededApiNames(correlationId);
            return names != null ? names : Collections.emptySet();
        } catch (Exception ex) {
            log.error("[{}] Failed to load succeeded api names — treating as fresh start: {}",
                    correlationId, ex.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Marks the full journey as COMPLETED and stores the application number.
     * Called from NewBusinessService after PAS returns applicationNumber.
     */
    public void markJourneyCompleted(JourneyContext context) {
        try {
            executionRepository.findByCorrelationId(context.getCorrelationId())
                    .ifPresent(execution -> {
                        execution.setOverallStatus("COMPLETED");
                        execution.setApplicationNumber(context.getApplicationNumber());
                        // Clear any earlier failure markers — the journey has now succeeded.
                        execution.setFailedStageName(null);
                        execution.setFailedApiName(null);
                        execution.setFailureReason(null);
                        executionRepository.save(execution);
                    });
        } catch (Exception ex) {
            log.error("[{}] Failed to mark journey completed: {}",
                    context.getCorrelationId(), ex.getMessage());
        }
    }

    /**
     * Marks the journey as FAILED and records WHERE it stopped.
     * The failing stage/API/error are read from the most recent FAILED row in
     * journey_stage_log so the UI can show "application X failed at stage <Y> (<API>)".
     * Called from NewBusinessService catch block in processJourney().
     */
    public void markJourneyFailed(JourneyContext context, String errorMessage) {
        try {
            executionRepository.findByCorrelationId(context.getCorrelationId())
                    .ifPresent(execution -> {
                        execution.setOverallStatus("FAILED");

                        // Identify the API/stage that last failed for this correlationId.
                        JourneyStageLog lastFailure = stageLogRepository
                                .findTopByCorrelationIdAndStatusOrderByIdDesc(
                                        context.getCorrelationId(), "FAILED");
                        if (lastFailure != null) {
                            execution.setFailedStageName(lastFailure.getStageName());
                            execution.setFailedApiName(lastFailure.getApiName());
                            execution.setFailureReason(lastFailure.getErrorMessage() != null
                                    ? lastFailure.getErrorMessage() : errorMessage);
                        } else {
                            // No stage-level failure row (e.g. failure before any API call).
                            execution.setFailureReason(errorMessage);
                        }
                        executionRepository.save(execution);
                    });
        } catch (Exception ex) {
            log.error("[{}] Failed to mark journey failed: {}",
                    context.getCorrelationId(), ex.getMessage());
        }
    }

    /**
     * Resets a FAILED journey back to IN_PROGRESS and clears the failure markers
     * when a manual retry is triggered from the UI.
     */
    public void markJourneyResumed(String correlationId) {
        try {
            executionRepository.findByCorrelationId(correlationId)
                    .ifPresent(execution -> {
                        execution.setOverallStatus("IN_PROGRESS");
                        execution.setFailedStageName(null);
                        execution.setFailedApiName(null);
                        execution.setFailureReason(null);
                        executionRepository.save(execution);
                    });
        } catch (Exception ex) {
            log.error("[{}] Failed to mark journey resumed: {}", correlationId, ex.getMessage());
        }
    }

    /**
     * Logs one API call attempt to journey_stage_log.
     * Called by every ApiClient for EVERY call — both success and failure.
     * On retry, a new row is inserted — previous rows remain as audit history.
     *
     * Request and response objects are serialised to JSON for storage.
     * journey_stage_log is NEVER updated — only inserted.
     */
    public void logApiCall(JourneyContext context,
                            String stageName,
                            String apiName,
                            Object request,
                            Object response,
                            String status,
                            String errorCode,
                            String errorMessage,
                            long durationMs) {
        try {
            JourneyStageLog entry = new JourneyStageLog();
            entry.setCorrelationId(context.getCorrelationId());
            // Business tracking key — lets the UI search the journey by application number.
            entry.setApplicationNumber(context.getApplicationNumber());
            entry.setStageName(stageName);
            entry.setApiName(apiName);
            entry.setRequestPayload(toJson(request));
            entry.setResponsePayload(toJson(response));
            entry.setStatus(status);
            entry.setErrorCode(errorCode);
            entry.setErrorMessage(errorMessage);
            entry.setDurationMs(durationMs);
            entry.setExecutedAt(LocalDateTime.now());
            stageLogRepository.save(entry);
        } catch (Exception ex) {
            // Tracking failure must never stop the journey
            log.error("[{}] Failed to write stage log [{}/{}]: {}",
                    context.getCorrelationId(), stageName, apiName, ex.getMessage());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return "[serialization-error: " + ex.getMessage() + "]";
        }
    }
}
