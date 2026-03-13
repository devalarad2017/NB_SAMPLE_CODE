package com.insurance.newbusiness.integration.client;

import com.insurance.newbusiness.exception.ApiCallException;
import com.insurance.newbusiness.exception.JourneyStageException;
import com.insurance.newbusiness.integration.model.eligibility.EligibilityRequest;
import com.insurance.newbusiness.integration.model.eligibility.EligibilityResponse;
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
 * EligibilityApiClient — calls ELIGIBILITY_API.
 *
 * ── PATTERN — ALL API CLIENTS FOLLOW THIS SAME STRUCTURE ─────────────────────
 * Every downstream API client in this application follows this exact pattern:
 *
 *   1. @Value reads URL from application.properties (OCP ConfigMap)
 *   2. @Retryable retries 3 times with exponential backoff on ApiCallException
 *   3. call() resolves timing, calls RestTemplate, logs to journey_stage_log
 *   4. @Recover fires after all 3 retries fail — throws JourneyStageException
 *   5. JourneyOrchestrator catches JourneyStageException and stops the journey
 *   6. On next retry run, JourneyOrchestrator skips this API if already SUCCESS
 *
 * ── CREATING A NEW API CLIENT ─────────────────────────────────────────────────
 * 1. Copy this file
 * 2. Change class name, STAGE_NAME, API_NAME
 * 3. Change @Value property key (add matching entry to application.properties)
 * 4. Change call() method signature to the new request/response types
 * 5. Inject in JourneyOrchestrator
 *
 * ── RETRY AOP NOTE ────────────────────────────────────────────────────────────
 * @Retryable works via Spring AOP proxy. The call() method must be invoked from
 * OUTSIDE this bean (from JourneyOrchestrator). Calling it from within this
 * class bypasses the proxy and retry will NOT work.
 * @EnableRetry on NewBusinessApplication activates this globally.
 */
@Service
public class EligibilityApiClient {

    private static final Logger log       = LoggerFactory.getLogger(EligibilityApiClient.class);
    private static final String STAGE     = "ELIGIBILITY_STAGE";
    private static final String API_NAME  = "ELIGIBILITY_API";

    // URL loaded from application.properties → populated by OCP ConfigMap at deployment
    @Value("${api.endpoints.eligibility}")
    private String url;

    @Autowired private RestTemplate          restTemplate;
    @Autowired private JourneyTrackingService trackingService;

    // ─────────────────────────────────────────────────────────────────────────
    // @Retryable: 3 attempts, 2s → 4s backoff on ApiCallException.
    // Each attempt is logged separately in journey_stage_log.
    // maxAttempts=3 means: attempt 1 (immediate) + attempt 2 (2s) + attempt 3 (4s).
    // ─────────────────────────────────────────────────────────────────────────
    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public EligibilityResponse call(EligibilityRequest request, JourneyContext context) {
        long start = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            EligibilityResponse response = restTemplate.postForObject(
                    url, request, EligibilityResponse.class);

            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, duration);

            log.info("[{}] {} SUCCESS | status={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null", duration);

            return response;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), duration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, duration, ex.getMessage());

            // Throw ApiCallException — this triggers @Retryable to retry
            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Recover: called by Spring Retry after ALL 3 attempts fail.
    // Signature rule: first param = exception type from @Retryable,
    //                 remaining params = same as call() method.
    // ─────────────────────────────────────────────────────────────────────────
    @Recover
    public EligibilityResponse recover(ApiCallException ex,
                                        EligibilityRequest request,
                                        JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API_NAME, ex.getMessage());
        // JourneyStageException stops the journey.
        // JourneyOrchestrator catches this, marks journey as FAILED.
        // On next retry run, this API will re-execute (not in SUCCESS set).
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
