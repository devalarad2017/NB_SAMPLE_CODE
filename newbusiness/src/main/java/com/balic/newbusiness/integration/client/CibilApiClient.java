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
import com.balic.newbusiness.integration.model.cibil.CibilRequest;
import com.balic.newbusiness.integration.model.cibil.CibilResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CibilApiClient — calls CIBIL API.
 * First stage in the journey.
 */
@Service
@RequiredArgsConstructor
public class CibilApiClient {

    private static final Logger log       = LoggerFactory.getLogger(CibilApiClient.class);
    private static final String STAGE     = "CIBIL_STAGE";
    private static final String API_NAME  = "CIBIL_API";

    @Value("${api.endpoints.cibil}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public CibilResponse[] call(CibilRequest request, JourneyContext context) {
        long cibilStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("CIBIL_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            CibilResponse[] response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(CibilResponse[].class);

            log.info("CIBIL_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long cibilDuration = System.currentTimeMillis() - cibilStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, cibilDuration);

            log.info("[{}] {} SUCCESS | cibilScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response[0].getCibilStatus() : "null", cibilDuration);

            return response;

        } catch (Exception ex) {
            long cibilDuration = System.currentTimeMillis() - cibilStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), cibilDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, cibilDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    // Return type MUST match the @Retryable method (CibilResponse[]) for Spring Retry
    // to wire this recovery method; a mismatched type is silently ignored at runtime.
    @Recover
    public CibilResponse[] recover(ApiCallException ex,
                                  CibilRequest request,
                                  JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
