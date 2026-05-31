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
import com.balic.newbusiness.integration.model.ekyc.EkycRequest;
import com.balic.newbusiness.integration.model.ekyc.EkycResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EkycApiClient {

	private static final Logger log       = LoggerFactory.getLogger(EkycApiClient.class);
    private static final String STAGE     = "EKYC_STAGE";
    private static final String API_NAME  = "EKYC_API";

    @Value("${api.endpoints.ekyc}")
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
    public EkycResponse call(EkycRequest request, JourneyContext context) {
        long ekycStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url + request.getApplicationNo());

        try {
            log.info("EKYC_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            EkycResponse response = restTemplate.getForObject(
                    url + request.getApplicationNo(), EkycResponse.class);

            log.info("EKYC_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long ekycDuration = System.currentTimeMillis() - ekycStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, ekycDuration);

            log.info("[{}] {} SUCCESS | auwScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getErrorCode() : "null", ekycDuration);

            return response;

        } catch (Exception ex) {
            long ekycDuration = System.currentTimeMillis() - ekycStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), ekycDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, ekycDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Recover: called by Spring Retry after ALL 3 attempts fail.
    // Signature rule: first param = exception type from @Retryable,
    //                 remaining params = same as call() method.
    // ─────────────────────────────────────────────────────────────────────────
    @Recover
    public EkycResponse recover(ApiCallException ex, EkycRequest request, JourneyContext context) {
    	
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        
        // JourneyStageException stops the journey.
        // JourneyOrchestrator catches this, marks journey as FAILED.
        // On next retry run, this API will re-execute (not in SUCCESS set).
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
