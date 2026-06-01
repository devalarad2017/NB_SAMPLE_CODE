package com.balic.newbusiness.service;

import com.balic.newbusiness.domain.dto.InboundRequest;
import com.balic.newbusiness.domain.dto.JourneyStatusResponse;
import com.balic.newbusiness.domain.entity.JourneyExecution;
import com.balic.newbusiness.domain.entity.JourneyStageLog;
import com.balic.newbusiness.domain.entity.RawRequest;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.journey.JourneyOrchestrator;
import com.balic.newbusiness.pas.PasApiClient;
import com.balic.newbusiness.repository.JourneyExecutionRepository;
import com.balic.newbusiness.repository.JourneyStageLogRepository;
import com.balic.newbusiness.repository.RawRequestRepository;
import com.balic.newbusiness.reversefeed.PartnerNotifierFactory;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * NewBusinessService — top-level coordinator.
 *
 * ── TWO METHODS, TWO THREADS ─────────────────────────────────────────────────
 *
 * receiveAndAcknowledge() — runs on HTTP request thread
 *   1. Generate correlationId — added to MDC for all log lines
 *   2. Persist raw request to DB — immutable record before any processing
 *   3. Create journey_execution record in DB
 *   4. Trigger processJourney() async — immediately returns correlationId
 *   Controller returns 202 Accepted + correlationId to partner. Done.
 *
 * processJourney() — runs on journeyTaskExecutor thread (@Async)
 *   Partner has already received 202. This runs in background.
 *   1. Execute all 8 stages via JourneyOrchestrator
 *   2. Submit to PAS — get applicationNumber
 *   3. Store applicationNumber in journey_execution
 *   4. Send reverse feed to partner callback URL
 *
 * ── WHY PAS AND REVERSE FEED ARE HERE NOT IN ORCHESTRATOR ─────────────────────
 * PAS is the only API that returns a value (applicationNumber) that must be
 * stored in journey_execution AND passed to the reverse feed.
 * Keeping these two steps here makes the data flow explicit and easy to follow.
 *
 * ── ASYNC EXCEPTION HANDLING ─────────────────────────────────────────────────
 * Exceptions thrown inside processJourney() do NOT reach GlobalExceptionHandler.
 * They are caught in the try/catch here. Journey is marked FAILED in DB.
 * To resume: call processJourney() again with the same correlationId.
 * JourneyOrchestrator will skip already-succeeded APIs automatically.
 *
 * NOTE — "retry" vs "resume": the per-API @Retryable in each ApiClient is the
 * technical retry of a single call (transient blips). The journey-level
 * resumeJourney() below is a human-triggered RESUME of a FAILED journey that
 * continues from the stage that failed. Different concepts — kept named apart.
 */

@Service
@RequiredArgsConstructor
public class NewBusinessService {

    private static final Logger log = LoggerFactory.getLogger(NewBusinessService.class);

    private final RawRequestRepository       rawRequestRepository;
    private final JourneyExecutionRepository executionRepository;
    private final JourneyStageLogRepository  stageLogRepository;
    private final JourneyTrackingService     trackingService;
    private final JourneyOrchestrator        journeyOrchestrator;
    private final PasApiClient               pasApiClient;
    private final PartnerNotifierFactory     notifierFactory;
    private final ObjectMapper               objectMapper;

    // Inbound param that carries the partner-supplied application number (business
    // tracking key). Configurable so it can change without code edits.
    // (@Value is copied onto the generated constructor param via lombok.config.)
    @Value("${journey.application-number-param:obj1.stringval6}")
    private final String applicationNumberParam;

    // Self-reference to the Spring-managed proxy. A direct processJourney() call would be
    // a self-invocation that bypasses the @Async proxy (running the journey synchronously
    // on the HTTP thread, inside the @Transactional boundary, holding a DB connection for
    // the whole journey). Going through the injected proxy ensures @Async takes effect.
    // @Lazy (copied onto the constructor param via lombok.config) breaks the self-
    // referential bean-creation cycle by injecting a lazy proxy.
    @Lazy
    private final NewBusinessService self;

    // ==========================================================================
    // SYNC — runs on HTTP thread. Must be fast. Returns immediately.
    // ==========================================================================
    @Transactional
    public String receiveAndAcknowledge(InboundRequest inboundRequest) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        log.info("[{}] Inbound request received | partner={}",
                correlationId, inboundRequest.getPartnerCode());
        long journeyStart = System.currentTimeMillis();
        // Persist raw request — immutable snapshot of everything partner sent
        // Stores full payload (all pInObj + pInList objects) for audit trail
        RawRequest rawRequest = new RawRequest();
        rawRequest.setCorrelationId(correlationId);
        rawRequest.setPartnerCode(inboundRequest.getPartnerCode());
        rawRequest.setRawPayload(toJson(inboundRequest));
        rawRequestRepository.save(rawRequest);

        // Build context — getParams() merges pInObj1 + pInObj2 + pInObj3 into one Map
        JourneyContext context = new JourneyContext(
                correlationId,
                inboundRequest.getPartnerCode(),
                inboundRequest.getParams()
        );
        // Stamp the inbound application number so every stage log can be searched by it.
        context.setApplicationNumber(context.getRawParams().get(applicationNumberParam));

        // Create journey_execution row — status = IN_PROGRESS
        trackingService.initJourney(context, rawRequest.getId());

        // Fire async — HTTP thread returns here immediately with correlationId.
        // Must go through the injected proxy (self) so @Async is honoured; a direct
        // processJourney(...) call would run synchronously on this thread.
        self.processJourney(context, journeyStart);

        return correlationId;
    }

    // ==========================================================================
    // ASYNC — runs on journeyTaskExecutor. Partner already got 202.
    // @Async requires @EnableAsync on NewBusinessApplication (already set).
    // ==========================================================================
    @Async("journeyTaskExecutor")
    public void processJourney(JourneyContext context, long journeyStart) {
        try {
            MDC.put("correlationId", context.getCorrelationId());

            // Stage 1-8: eligibility → scoring → medical → kyc → premium
            //             → underwriting → document → proposal
            journeyOrchestrator.execute(context);

            // Stage 9: PAS submission — separate because it returns applicationNumber
            String applicationNumber = pasApiClient.submitAndGetApplicationNumber(context);
            context.setApplicationNumber(applicationNumber);

            // Store applicationNumber on journey_execution and mark COMPLETED
            trackingService.markJourneyCompleted(context);

            // Stage 10: Reverse feed — push applicationNumber to partner callback URL
//            notifierFactory
//                    .getNotifier(context.getPartnerCode())
//                    .notify(context, applicationNumber);
            long journeyDuration = System.currentTimeMillis() - journeyStart;
            log.info("[{}] Journey COMPLETED | applicationNumber={} | {}ms",
                    context.getCorrelationId(), applicationNumber, journeyDuration);

        } catch (Exception ex) {
            // Mark FAILED so it can be resumed later
            trackingService.markJourneyFailed(context, ex.getMessage());
            log.error("[{}] Journey FAILED: {}",
                    context.getCorrelationId(), ex.getMessage(), ex);
            // Do NOT rethrow — @Async methods that throw cause unhandled exception warnings
        } finally {
            MDC.clear();
        }
    }

    // ==========================================================================
    // RESUME — triggered manually from the UI for a FAILED journey.
    //
    // Rebuilds the EXACT same context from the stored raw request, resets the
    // execution status, and re-runs the journey async. JourneyOrchestrator skips
    // already-succeeded APIs and JourneyResultRestorer restores their results, so
    // processing resumes from the stage that previously failed, with the same data.
    // ==========================================================================
    @Transactional
    public String resumeJourney(String correlationId) {
        RawRequest rawRequest = rawRequestRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No raw request found for correlationId: " + correlationId));

        log.info("[{}] Resume requested — resuming journey", correlationId);

        // Rebuild the original partner request, then the same JourneyContext.
        InboundRequest inboundRequest = fromJson(rawRequest.getRawPayload(), InboundRequest.class);
        JourneyContext context = new JourneyContext(
                correlationId,
                inboundRequest.getPartnerCode(),
                inboundRequest.getParams());
        // Same business application number as the original run.
        context.setApplicationNumber(context.getRawParams().get(applicationNumberParam));

        // Reset status to IN_PROGRESS and clear previous failure markers.
        trackingService.markJourneyResumed(correlationId);

        long journeyStart = System.currentTimeMillis();
        // Through the proxy so @Async actually runs it on the journey executor.
        self.processJourney(context, journeyStart);

        return correlationId;
    }

    // ==========================================================================
    // STATUS — read model for the UI: overall status, where it failed, and the
    // full per-attempt stage history for one application/journey.
    // ==========================================================================
    @Transactional(readOnly = true)
    public JourneyStatusResponse getStatus(String correlationId) {
        JourneyExecution execution = executionRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No journey found for correlationId: " + correlationId));

        List<JourneyStageLog> logs =
                stageLogRepository.findByCorrelationIdOrderByIdDesc(correlationId);

        return JourneyStatusResponse.from(execution, logs);
    }

    // ==========================================================================
    // BUSINESS-KEY ACCESS — the UI tracks journeys by application number.
    // application_number maps 1:1 to a correlationId via journey_stage_log; these
    // resolve the application number to its journey, then reuse the existing logic.
    // ==========================================================================
    @Transactional(readOnly = true)
    public JourneyStatusResponse getStatusByApplicationNumber(String applicationNumber) {
        return getStatus(resolveCorrelationId(applicationNumber));
    }

    @Transactional
    public String resumeByApplicationNumber(String applicationNumber) {
        return resumeJourney(resolveCorrelationId(applicationNumber));
    }

    /** Finds the correlationId backing an application number, or 400 if none exists. */
    private String resolveCorrelationId(String applicationNumber) {
        JourneyStageLog latest = stageLogRepository
                .findFirstByApplicationNumberOrderByIdDesc(applicationNumber);
        if (latest == null) {
            throw new IllegalArgumentException(
                    "No journey found for application number: " + applicationNumber);
        }
        return latest.getCorrelationId();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[serialization-error]";
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            // A stored raw payload that won't parse is unrecoverable for resume — surface it.
            throw new IllegalStateException(
                    "Could not parse stored raw request payload: " + e.getMessage(), e);
        }
    }
}
