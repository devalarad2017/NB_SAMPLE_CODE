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
import com.balic.newbusiness.integration.model.pan.PanRequest;
import com.balic.newbusiness.integration.model.pan.PanResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PanApiClient {
	
	private static final Logger log       = LoggerFactory.getLogger(PanApiClient.class);
    private static final String STAGE     = "PAN_STAGE";
    private static final String PAN_NAME  = "PAN_API";

    @Value("${api.endpoints.pan}")
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
    public PanResponse call(PanRequest request, JourneyContext context) {
        long panStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), PAN_NAME, url);

        try {
            log.info("PAN_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            PanResponse response = restTemplate.postForObject(
                    url, request, PanResponse.class);

            log.info("PAN_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long panDuration = System.currentTimeMillis() - panStart;
            trackingService.logApiCall(context, STAGE, PAN_NAME,
                    request, response, "SUCCESS", null, null, panDuration);

            log.info("[{}] {} SUCCESS | panScore={} | {}ms",
                    context.getCorrelationId(), PAN_NAME,
                    response != null ? response.getStatus() : "null", panDuration);

            return response;

        } catch (Exception ex) {
            long panDuration = System.currentTimeMillis() - panStart;
            trackingService.logApiCall(context, STAGE, PAN_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), panDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), PAN_NAME, panDuration, ex.getMessage());

            throw new ApiCallException(PAN_NAME, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Recover: called by Spring Retry after ALL 3 attempts fail.
    // Signature rule: first param = exception type from @Retryable,
    //                 remaining params = same as call() method.
    // ─────────────────────────────────────────────────────────────────────────
    @Recover
    public PanResponse recover(ApiCallException ex, PanRequest request, JourneyContext context) {
    	
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), PAN_NAME, ex.getMessage());
        
        // JourneyStageException stops the journey.
        // JourneyOrchestrator catches this, marks journey as FAILED.
        // On next retry run, this API will re-execute (not in SUCCESS set).
        throw new JourneyStageException(STAGE,
        		PAN_NAME + " failed after all retries: " + ex.getMessage());
    }

}
