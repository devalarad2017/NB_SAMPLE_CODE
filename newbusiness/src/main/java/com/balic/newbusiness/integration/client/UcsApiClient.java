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
import com.balic.newbusiness.integration.model.ucs.UcsApiRequest;
import com.balic.newbusiness.integration.model.ucs.UcsResponse;
import com.balic.newbusiness.journey.JourneyContext;

/**
 * UcsApiClient — calls UCS API.
 *
 * ── PATTERN — ALL STANDARD API CLIENTS FOLLOW THIS SAME STRUCTURE ────────────
 *   1. @Value reads URL from application.properties (OCP ConfigMap)
 *   2. @Retryable retries 3 times with exponential backoff on ApiCallException
 *   3. call() delegates the timing/logging/tracking/error-wrapping to ApiCallTemplate
 *      and supplies only the actual RestClient invocation as a lambda
 *   4. @Recover fires after all 3 retries fail — throws JourneyStageException
 *   5. JourneyOrchestrator catches JourneyStageException and stops the journey
 *   6. On the next resume run, JourneyOrchestrator skips this API if already SUCCESS
 *
 * ── RETRY AOP NOTE ────────────────────────────────────────────────────────────
 * @Retryable works via Spring AOP proxy. call() must be invoked from OUTSIDE this
 * bean (from JourneyOrchestrator); a self-call bypasses the proxy and retry won't fire.
 * @EnableRetry on NewBusinessApplication activates this globally.
 */
@Service
@RequiredArgsConstructor
public class UcsApiClient {

    private static final Logger  log = LoggerFactory.getLogger(UcsApiClient.class);
    private static final ApiName API = ApiName.UCS_API;

    @Value("${api.endpoints.ucs}")
    private final String url;

    private final RestClient      restClient;
    private final ApiCallTemplate apiCallTemplate;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public UcsResponse call(UcsApiRequest request, JourneyContext context) {
        return apiCallTemplate.execute(API, request, context, () ->
                restClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(UcsResponse.class));
    }

    @Recover
    public UcsResponse recover(ApiCallException ex, UcsApiRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API.apiName(), ex.getMessage());
        throw new JourneyStageException(API.stageName(),
                API.apiName() + " failed after all retries: " + ex.getMessage());
    }
}
