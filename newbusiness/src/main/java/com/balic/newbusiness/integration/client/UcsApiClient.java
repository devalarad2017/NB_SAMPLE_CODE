package com.balic.newbusiness.integration.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.ucs.UcsApiRequest;
import com.balic.newbusiness.integration.model.ucs.UcsResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * UcsApiClient — calls UCS API.
 *
 * ── PATTERN — ALL API CLIENTS FOLLOW THIS SAME STRUCTURE ─────────────────────
 *   1. @Value reads URL from application.properties (OCP ConfigMap)
 *   2. @Retryable retries 3 times with exponential backoff on ApiCallException
 *   3. call() times the call, invokes RestClient, logs to journey_stage_log
 *   4. @Recover fires after all 3 retries fail — throws JourneyStageException
 *   5. JourneyOrchestrator catches JourneyStageException and stops the journey
 *   6. On next retry run, JourneyOrchestrator skips this API if already SUCCESS
 *
 * ── RETRY AOP NOTE ────────────────────────────────────────────────────────────
 * @Retryable works via Spring AOP proxy. call() must be invoked from OUTSIDE this
 * bean (from JourneyOrchestrator); a self-call bypasses the proxy and retry won't fire.
 * @EnableRetry on NewBusinessApplication activates this globally.
 */
@Service
@RequiredArgsConstructor
public class UcsApiClient {

    private static final Logger log       = LoggerFactory.getLogger(UcsApiClient.class);
    private static final String STAGE     = "UCS_STAGE";
    private static final String API_NAME  = "UCS_API";

    @Value("${api.endpoints.ucs}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public UcsResponse call(UcsApiRequest request, JourneyContext context) {
        long ucsStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("UCS_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            UcsResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(UcsResponse.class);

            log.info("UCS_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long ucsDuration = System.currentTimeMillis() - ucsStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, ucsDuration);

            log.info("[{}] {} SUCCESS | ucsScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getTranxStatus().getStatus() : "null", ucsDuration);

            return response;

        } catch (Exception ex) {
            long ucsDuration = System.currentTimeMillis() - ucsStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), ucsDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, ucsDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public UcsResponse recover(ApiCallException ex, UcsApiRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
