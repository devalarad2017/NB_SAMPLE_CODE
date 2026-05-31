package com.balic.newbusiness.integration.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.dcs.DcsRequest;
import com.balic.newbusiness.integration.model.dcs.DcsResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DcsApiClient {
	
	private static final Logger log       = LoggerFactory.getLogger(DcsApiClient.class);
    private static final String STAGE     = "DCS_STAGE";
    private static final String API_NAME  = "DCS_API";

    @Value("${api.endpoints.dcs}")
    private String url;

    @Autowired private RestTemplate          restTemplate;
    @Autowired private JourneyTrackingService trackingService;
    @Autowired
    private ObjectMapper objectMapper;

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
    public DcsResponse call(DcsRequest request, JourneyContext context) {
        long dcsStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("DCS_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            DcsResponse response = restTemplate.postForObject(
                    url, request, DcsResponse.class);

            log.info("DCS_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long dcsDuration = System.currentTimeMillis() - dcsStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, dcsDuration);

            log.info("[{}] {} SUCCESS | dcsScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null", dcsDuration);

            return response;

        } catch (Exception ex) {
            long dcsDuration = System.currentTimeMillis() - dcsStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), dcsDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, dcsDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Recover: called by Spring Retry after ALL 3 attempts fail.
    // Signature rule: first param = exception type from @Retryable,
    //                 remaining params = same as call() method.
    // ─────────────────────────────────────────────────────────────────────────
    @Recover
    public DcsResponse recover(ApiCallException ex, DcsRequest request, JourneyContext context) {
    	
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        
        // JourneyStageException stops the journey.
        // JourneyOrchestrator catches this, marks journey as FAILED.
        // On next retry run, this API will re-execute (not in SUCCESS set).
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }

}
