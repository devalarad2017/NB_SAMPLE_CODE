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
import com.balic.newbusiness.integration.model.fes.FesRequest;
import com.balic.newbusiness.integration.model.fes.FesResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class FesApiClient {

    private static final Logger log       = LoggerFactory.getLogger(FesApiClient.class);
    private static final String STAGE     = "FES_STAGE";
    private static final String API_NAME  = "FES_API";

    @Value("${api.endpoints.fes}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public FesResponse call(FesRequest request, JourneyContext context) {
        long fesStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("FES_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            FesResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(FesResponse.class);

            log.info("FES_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long fesDuration = System.currentTimeMillis() - fesStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, fesDuration);

            log.info("[{}] {} SUCCESS | fesScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatusCode() : "null", fesDuration);

            return response;

        } catch (Exception ex) {
            long fesDuration = System.currentTimeMillis() - fesStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), fesDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, fesDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public FesResponse recover(ApiCallException ex, FesRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
