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
import com.balic.newbusiness.integration.model.mrs.MrsRequest;
import com.balic.newbusiness.integration.model.mrs.MrsResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MrsApiClient {

    private static final Logger log       = LoggerFactory.getLogger(MrsApiClient.class);
    private static final String STAGE     = "MRS_STAGE";
    private static final String API_NAME  = "MRS_API";

    @Value("${api.endpoints.mrs}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public MrsResponse call(MrsRequest request, JourneyContext context) {
        long mrsStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("MRS_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            MrsResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(MrsResponse.class);

            log.info("MRS_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long mrsDuration = System.currentTimeMillis() - mrsStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, mrsDuration);

            log.info("[{}] {} SUCCESS | mrsScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null", mrsDuration);

            return response;

        } catch (Exception ex) {
            long mrsDuration = System.currentTimeMillis() - mrsStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), mrsDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, mrsDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public MrsResponse recover(ApiCallException ex, MrsRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
