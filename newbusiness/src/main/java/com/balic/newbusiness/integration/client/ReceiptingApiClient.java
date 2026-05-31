package com.balic.newbusiness.integration.client;

import com.balic.newbusiness.integration.model.pas.PasInitialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.receipting.ReceiptingRequest;
import com.balic.newbusiness.integration.model.receipting.ReceiptingResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;

@Service
public class ReceiptingApiClient {
	
	private static final Logger log       = LoggerFactory.getLogger(ReceiptingApiClient.class);
    private static final String STAGE     = "RECEIPTING_STAGE";
    private static final String RECEIPTING_NAME  = "RECEIPTING_API";

    @Value("${api.endpoints.receipting}")
    private String url;

    @Autowired private RestTemplate          restTemplate;
    @Autowired private JourneyTrackingService trackingService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NGINTokenAPIClient nginToken;

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
    public ReceiptingResponse call(ReceiptingRequest request, JourneyContext context) {
        long receiptingStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), RECEIPTING_NAME, url);

        try {
            log.info("RECEIPTING_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            String token = nginToken.GenerateNginTokenOSB();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            if(token != null) {
                headers.add("Authorization", token);
            }
            HttpEntity<?> entityGlobal = new HttpEntity<>(request, headers);

            ResponseEntity<ReceiptingResponse> res= restTemplate.exchange(url, HttpMethod.POST, entityGlobal, ReceiptingResponse.class);
            ReceiptingResponse response = res.getBody();

            log.info("RECEIPTING_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long receitingDuration = System.currentTimeMillis() - receiptingStart;
            trackingService.logApiCall(context, STAGE, RECEIPTING_NAME,
                    request, response, "SUCCESS", null, null, receitingDuration);

            log.info("[{}] {} SUCCESS | receiptingScore={} | {}ms",
                    context.getCorrelationId(), RECEIPTING_NAME,
                    response != null ? response.getStatus() : "null", receitingDuration);

            return response;

        } catch (Exception ex) {
            long receitingDuration = System.currentTimeMillis() - receiptingStart;
            trackingService.logApiCall(context, STAGE, RECEIPTING_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), receitingDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), RECEIPTING_NAME, receitingDuration, ex.getMessage());

            throw new ApiCallException(RECEIPTING_NAME, ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // @Recover: called by Spring Retry after ALL 3 attempts fail.
    // Signature rule: first param = exception type from @Retryable,
    //                 remaining params = same as call() method.
    // ─────────────────────────────────────────────────────────────────────────
    @Recover
    public ReceiptingResponse recover(ApiCallException ex, ReceiptingRequest request, JourneyContext context) {
    	
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), RECEIPTING_NAME, ex.getMessage());
        
        // JourneyStageException stops the journey.
        // JourneyOrchestrator catches this, marks journey as FAILED.
        // On next retry run, this API will re-execute (not in SUCCESS set).
        throw new JourneyStageException(STAGE,
        		RECEIPTING_NAME + " failed after all retries: " + ex.getMessage());
    }

}
