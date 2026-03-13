package com.insurance.newbusiness.integration.client;

import com.insurance.newbusiness.exception.ApiCallException;
import com.insurance.newbusiness.exception.JourneyStageException;
import com.insurance.newbusiness.integration.model.scoring.*;
import com.insurance.newbusiness.journey.JourneyContext;
import com.insurance.newbusiness.tracking.JourneyTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * ScoringApiClient — calls EDC_API, PASA_API, and TASA_API.
 *
 * ── WHY ONE CLIENT FOR THREE APIS ────────────────────────────────────────────
 * EDC, PASA, TASA all receive the same ScoringRequest and run in parallel.
 * Grouping them in one client avoids three near-identical classes.
 * Each gets its own call method (callEdc, callPasa, callTasa) with its own URL.
 *
 * ── PARALLEL EXECUTION ───────────────────────────────────────────────────────
 * JourneyOrchestrator calls all three using CompletableFuture.runAsync():
 *
 *   CompletableFuture<Void> edcFuture  = CompletableFuture.runAsync(
 *       () -> context.setEdcResult(scoringClient.callEdc(req, context)), executor);
 *   CompletableFuture<Void> pasaFuture = CompletableFuture.runAsync(
 *       () -> context.setPasaResult(scoringClient.callPasa(req, context)), executor);
 *   CompletableFuture<Void> tasaFuture = CompletableFuture.runAsync(
 *       () -> context.setTasaResult(scoringClient.callTasa(req, context)), executor);
 *   CompletableFuture.allOf(edcFuture, pasaFuture, tasaFuture).join();
 *   // All three must complete before MEDICAL_STAGE begins
 *
 * ── RETRY BEHAVIOUR ───────────────────────────────────────────────────────────
 * Each API (EDC/PASA/TASA) has its own @Retryable method, so retries are
 * independent. If EDC fails after 3 retries but PASA/TASA succeed, only EDC
 * will re-execute on the next journey retry (not PASA or TASA again).
 *
 * ── SPLITTING THIS CLASS ──────────────────────────────────────────────────────
 * If EDC/PASA/TASA contracts diverge significantly in future:
 *   1. Create EdcApiClient, PasaApiClient, TasaApiClient separately
 *   2. Update JourneyOrchestrator to inject three clients instead of one
 *   3. Each gets its own request POJO if needed
 */
@Service
public class ScoringApiClient {

    private static final Logger log      = LoggerFactory.getLogger(ScoringApiClient.class);
    private static final String STAGE    = "SCORING_STAGE";

    @Value("${api.endpoints.edc}")  private String edcUrl;
    @Value("${api.endpoints.pasa}") private String pasaUrl;
    @Value("${api.endpoints.tasa}") private String tasaUrl;

    @Autowired private RestTemplate           restTemplate;
    @Autowired private JourneyTrackingService trackingService;

    // ── EDC ───────────────────────────────────────────────────────────────────
    @Retryable(value = {ApiCallException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public EdcResponse callEdc(ScoringRequest request, JourneyContext context) {
        return doCall(edcUrl, "EDC_API", request, EdcResponse.class, context);
    }

    @Recover
    public EdcResponse recoverEdc(ApiCallException ex, ScoringRequest req, JourneyContext ctx) {
        log.error("[{}] EDC_API — all retries exhausted: {}", ctx.getCorrelationId(), ex.getMessage());
        throw new JourneyStageException(STAGE, "EDC_API failed after all retries: " + ex.getMessage());
    }

    // ── PASA ──────────────────────────────────────────────────────────────────
    @Retryable(value = {ApiCallException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public PasaResponse callPasa(ScoringRequest request, JourneyContext context) {
        return doCall(pasaUrl, "PASA_API", request, PasaResponse.class, context);
    }

    @Recover
    public PasaResponse recoverPasa(ApiCallException ex, ScoringRequest req, JourneyContext ctx) {
        log.error("[{}] PASA_API — all retries exhausted: {}", ctx.getCorrelationId(), ex.getMessage());
        throw new JourneyStageException(STAGE, "PASA_API failed after all retries: " + ex.getMessage());
    }

    // ── TASA ──────────────────────────────────────────────────────────────────
    @Retryable(value = {ApiCallException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public TasaResponse callTasa(ScoringRequest request, JourneyContext context) {
        return doCall(tasaUrl, "TASA_API", request, TasaResponse.class, context);
    }

    @Recover
    public TasaResponse recoverTasa(ApiCallException ex, ScoringRequest req, JourneyContext ctx) {
        log.error("[{}] TASA_API — all retries exhausted: {}", ctx.getCorrelationId(), ex.getMessage());
        throw new JourneyStageException(STAGE, "TASA_API failed after all retries: " + ex.getMessage());
    }

    // ── Shared call logic ─────────────────────────────────────────────────────
    // All three APIs follow identical call/log pattern — no duplication.
    private <R> R doCall(String url, String apiName,
                          ScoringRequest request,
                          Class<R> responseType,
                          JourneyContext context) {
        long start = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), apiName, url);
        try {
            R response = restTemplate.postForObject(url, request, responseType);
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, apiName,
                    request, response, "SUCCESS", null, null, duration);
            log.info("[{}] {} SUCCESS | {}ms", context.getCorrelationId(), apiName, duration);
            return response;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, apiName,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), duration);
            log.error("[{}] {} FAILED | {}ms | {}", context.getCorrelationId(), apiName, duration, ex.getMessage());
            throw new ApiCallException(apiName, ex.getMessage(), ex);
        }
    }
}
