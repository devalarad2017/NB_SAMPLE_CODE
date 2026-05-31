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
import com.balic.newbusiness.integration.model.edc.EdcRequest;
import com.balic.newbusiness.integration.model.edc.EdcResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EdcApiClient {

    private static final Logger log       = LoggerFactory.getLogger(EdcApiClient.class);
    private static final String STAGE     = "EDC_STAGE";
    private static final String API_NAME  = "EDC_API";

    @Value("${api.endpoints.edc}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public EdcResponse call(EdcRequest request, JourneyContext context) {
        long edcStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("EDC_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            EdcResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(EdcResponse.class);

            log.info("EDC_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long edcDuration = System.currentTimeMillis() - edcStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, edcDuration);

            log.info("[{}] {} SUCCESS | edcScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getErrorCode() : "null", edcDuration);

            return response;

        } catch (Exception ex) {
            long edcDuration = System.currentTimeMillis() - edcStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), edcDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, edcDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public EdcResponse recover(ApiCallException ex, EdcRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
