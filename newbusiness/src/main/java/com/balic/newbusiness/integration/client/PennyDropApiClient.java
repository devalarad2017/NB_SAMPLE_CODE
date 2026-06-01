package com.balic.newbusiness.integration.client;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.pennydrop.PennyDropRequest;
import com.balic.newbusiness.integration.model.pennydrop.PennyDropResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class PennyDropApiClient {

    private static final Logger log       = LoggerFactory.getLogger(PennyDropApiClient.class);
    private static final String STAGE     = "PENNYDROP_STAGE";
    private static final String API_NAME  = "PENNYDROP_API";

    @Value("${api.endpoints.pennydrop}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public PennyDropResponse call(PennyDropRequest request, JourneyContext context) {
        long pennyDropStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("PENNYDROP_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            PennyDropResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(PennyDropResponse.class);

            log.info("PENNYDROP_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long pennyDropDuration = System.currentTimeMillis() - pennyDropStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, pennyDropDuration);

            log.info("[{}] {} SUCCESS | pennyDropScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null", pennyDropDuration);

            return response;

        } catch (Exception ex) {
            long pennyDropDuration = System.currentTimeMillis() - pennyDropStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), pennyDropDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, pennyDropDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public PennyDropResponse recover(ApiCallException ex, PennyDropRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
