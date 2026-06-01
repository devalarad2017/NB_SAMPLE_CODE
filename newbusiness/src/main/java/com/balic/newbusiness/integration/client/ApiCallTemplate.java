package com.balic.newbusiness.integration.client;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.balic.newbusiness.domain.enums.ApiName;
import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * ApiCallTemplate — the shared "execute-around" wrapper for every standard ApiClient.
 *
 * <p>Each ApiClient keeps its OWN {@code @Retryable}/{@code @Recover} (Spring Retry
 * semantics are deliberately left untouched on the concrete methods) and passes ONLY
 * its actual HTTP call in as a {@link Supplier}. This template owns the boilerplate
 * that was previously copy-pasted into all 13 clients:
 * <ul>
 *   <li>timing</li>
 *   <li>request/response JSON logging (isolated so a logging error can never fail the call)</li>
 *   <li>journey_stage_log SUCCESS / FAILED tracking</li>
 *   <li>wrapping any failure as {@link ApiCallException} so the caller's {@code @Retryable} fires</li>
 * </ul>
 *
 * <p>The {@code httpCall} is supplied lazily and only invoked inside the try block,
 * so timing and exception handling correctly wrap the real call. The verb (GET/POST),
 * URL and binding type all live in the caller's lambda, so this template is
 * verb-agnostic and type-transparent — {@code Resp} may be a bean, an array, or a
 * parameterized type; it is decided entirely by the lambda.
 *
 * <p>Excluded by design: {@code PasApiClient} (two-step push + NGIN auth headers) and
 * {@code NGINTokenAPIClient} (swallows errors, returns null, has no JourneyContext).
 */
@Component
@RequiredArgsConstructor
public class ApiCallTemplate {

    private static final Logger log = LoggerFactory.getLogger(ApiCallTemplate.class);

    private final JourneyTrackingService trackingService;
    private final ObjectMapper objectMapper;

    /**
     * Runs {@code httpCall} with full timing, logging, tracking and error-wrapping.
     *
     * @param api      API identity (supplies api_name + stage_name)
     * @param request  the request object — used only for logging and stage-log tracking
     * @param context  the journey context (correlation id, application number)
     * @param httpCall the actual RestClient invocation, executed lazily inside the try
     * @param <Resp>   response type — bean, array, or parameterized; decided by the caller
     * @return whatever {@code httpCall} returns
     * @throws ApiCallException if {@code httpCall} throws — so the caller's {@code @Retryable} triggers
     */
    public <Resp> Resp execute(ApiName api,
                               Object request,
                               JourneyContext context,
                               Supplier<Resp> httpCall) {
        long start = System.currentTimeMillis();
        log.info("[{}] Calling {}", context.getCorrelationId(), api.apiName());
        logJson(api.apiName() + " request", request);

        try {
            Resp response = httpCall.get();
            logJson(api.apiName() + " response", response);

            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, api.stageName(), api.apiName(),
                    request, response, "SUCCESS", null, null, duration);
            log.info("[{}] {} SUCCESS | {}ms", context.getCorrelationId(), api.apiName(), duration);
            return response;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, api.stageName(), api.apiName(),
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), duration);
            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), api.apiName(), duration, ex.getMessage());
            throw new ApiCallException(api.apiName(), ex.getMessage(), ex);
        }
    }

    /** Serialising for logs must never affect the call — swallow and note instead. */
    private void logJson(String label, Object value) {
        try {
            log.info("{} : {}", label, objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            log.warn("{} : <unserializable: {}>", label, ex.getMessage());
        }
    }
}
