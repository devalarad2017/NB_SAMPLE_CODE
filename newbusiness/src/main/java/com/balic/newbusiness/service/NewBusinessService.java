package com.balic.newbusiness.service;

import com.balic.newbusiness.domain.dto.InboundRequest;
import com.balic.newbusiness.domain.entity.RawRequest;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.journey.JourneyOrchestrator;
import com.balic.newbusiness.pas.PasApiClient;
import com.balic.newbusiness.repository.RawRequestRepository;
import com.balic.newbusiness.reversefeed.PartnerNotifierFactory;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
 * To retry: call processJourney() again with the same correlationId.
 * JourneyOrchestrator will skip already-succeeded APIs automatically.
 */

@Service
public class NewBusinessService {

    private static final Logger log = LoggerFactory.getLogger(NewBusinessService.class);

    @Autowired private RawRequestRepository   rawRequestRepository;
    @Autowired private JourneyTrackingService  trackingService;
    @Autowired private JourneyOrchestrator     journeyOrchestrator;
    @Autowired private PasApiClient            pasApiClient;
    @Autowired private PartnerNotifierFactory  notifierFactory;
    @Autowired private ObjectMapper            objectMapper;

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

        // Create journey_execution row — status = IN_PROGRESS
        trackingService.initJourney(context, rawRequest.getId());

        // Fire async — HTTP thread returns here immediately with correlationId
        // processJourney() picks up on journeyTaskExecutor thread pool
        processJourney(context, journeyStart);

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
            // Mark FAILED so a retry job can pick it up later
            trackingService.markJourneyFailed(context, ex.getMessage());
            log.error("[{}] Journey FAILED: {}",
                    context.getCorrelationId(), ex.getMessage(), ex);
            // Do NOT rethrow — @Async methods that throw cause unhandled exception warnings
        } finally {
            MDC.clear();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[serialization-error]";
        }
    }
}
