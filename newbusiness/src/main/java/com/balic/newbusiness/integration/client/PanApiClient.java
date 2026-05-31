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
import com.balic.newbusiness.integration.model.pan.PanRequest;
import com.balic.newbusiness.integration.model.pan.PanResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PanApiClient {

    private static final Logger log       = LoggerFactory.getLogger(PanApiClient.class);
    private static final String STAGE     = "PAN_STAGE";
    private static final String PAN_NAME  = "PAN_API";

    @Value("${api.endpoints.pan}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

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

            PanResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(PanResponse.class);

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

    @Recover
    public PanResponse recover(ApiCallException ex, PanRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), PAN_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                PAN_NAME + " failed after all retries: " + ex.getMessage());
    }
}
