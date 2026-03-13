package com.insurance.newbusiness.integration.client;

import com.insurance.newbusiness.exception.ApiCallException;
import com.insurance.newbusiness.exception.JourneyStageException;
import com.insurance.newbusiness.integration.model.medical.MedicalRequest;
import com.insurance.newbusiness.integration.model.medical.MedicalResponse;
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

/** MedicalApiClient — calls MEDICAL_API. Same pattern as EligibilityApiClient. */
@Service
public class MedicalApiClient {

    private static final Logger log      = LoggerFactory.getLogger(MedicalApiClient.class);
    private static final String STAGE    = "MEDICAL_STAGE";
    private static final String API_NAME = "MEDICAL_API";

    @Value("${api.endpoints.medical}")
    private String url;

    @Autowired private RestTemplate           restTemplate;
    @Autowired private JourneyTrackingService trackingService;

    @Retryable(value = {ApiCallException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public MedicalResponse call(MedicalRequest request, JourneyContext context) {
        long start = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);
        try {
            MedicalResponse response = restTemplate.postForObject(url, request, MedicalResponse.class);
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, duration);
            log.info("[{}] {} SUCCESS | status={} score={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null",
                    response != null ? response.getMedicalScore() : "null",
                    duration);
            return response;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), duration);
            log.error("[{}] {} FAILED | {}ms | {}", context.getCorrelationId(), API_NAME, duration, ex.getMessage());
            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public MedicalResponse recover(ApiCallException ex, MedicalRequest req, JourneyContext ctx) {
        log.error("[{}] {} — all retries exhausted: {}", ctx.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE, API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
