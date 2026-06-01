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

import com.balic.newbusiness.domain.enums.ApiName;
import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.cibil.CibilRequest;
import com.balic.newbusiness.integration.model.cibil.CibilResponse;
import com.balic.newbusiness.journey.JourneyContext;

/**
 * CibilApiClient — calls CIBIL API. First stage in the journey.
 *
 * Timing, logging, tracking and error-wrapping live in {@link ApiCallTemplate};
 * this class owns only the URL, the actual HTTP call, and the retry/recover policy.
 */
@Service
@RequiredArgsConstructor
public class CibilApiClient {

    private static final Logger  log = LoggerFactory.getLogger(CibilApiClient.class);
    private static final ApiName API = ApiName.CIBIL_API;

    @Value("${api.endpoints.cibil}")
    private final String url;

    private final RestClient      restClient;
    private final ApiCallTemplate apiCallTemplate;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public CibilResponse[] call(CibilRequest request, JourneyContext context) {
        return apiCallTemplate.execute(API, request, context, () ->
                restClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(CibilResponse[].class));
    }

    // Return type MUST match the @Retryable method (CibilResponse[]) for Spring Retry
    // to wire this recovery method; a mismatched type is silently ignored at runtime.
    @Recover
    public CibilResponse[] recover(ApiCallException ex, CibilRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API.apiName(), ex.getMessage());
        throw new JourneyStageException(API.stageName(),
                API.apiName() + " failed after all retries: " + ex.getMessage());
    }
}
