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
import com.balic.newbusiness.integration.model.bi.BiRequest;
import com.balic.newbusiness.integration.model.bi.BiResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class BiApiClient {

    private static final Logger log       = LoggerFactory.getLogger(BiApiClient.class);
    private static final String STAGE     = "BI_STAGE";
    private static final String API_NAME  = "BI_API";

    @Value("${api.endpoints.bi}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public BiResponse call(BiRequest request, JourneyContext context) {
        long biStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("BI_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            BiResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(BiResponse.class);

            log.info("BI_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long biDuration = System.currentTimeMillis() - biStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, biDuration);

            log.info("[{}] {} SUCCESS | biScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null", biDuration);

            return response;

        } catch (Exception ex) {
            long biDuration = System.currentTimeMillis() - biStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), biDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, biDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public BiResponse recover(ApiCallException ex, BiRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
