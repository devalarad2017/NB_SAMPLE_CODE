package com.balic.newbusiness.integration.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.receipting.ReceiptingRequest;
import com.balic.newbusiness.integration.model.receipting.ReceiptingResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReceiptingApiClient {

    private static final Logger log       = LoggerFactory.getLogger(ReceiptingApiClient.class);
    private static final String STAGE     = "RECEIPTING_STAGE";
    private static final String RECEIPTING_NAME  = "RECEIPTING_API";

    @Value("${api.endpoints.receipting}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;
    private final NGINTokenAPIClient    nginToken;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public ReceiptingResponse call(ReceiptingRequest request, JourneyContext context) {
        long receiptingStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), RECEIPTING_NAME, url);

        try {
            log.info("RECEIPTING_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            String token = nginToken.GenerateNginTokenOSB();

            ReceiptingResponse response = restClient.post()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (token != null) {
                            headers.add("Authorization", token);
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(ReceiptingResponse.class);

            log.info("RECEIPTING_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long receitingDuration = System.currentTimeMillis() - receiptingStart;
            trackingService.logApiCall(context, STAGE, RECEIPTING_NAME,
                    request, response, "SUCCESS", null, null, receitingDuration);

            log.info("[{}] {} SUCCESS | receiptingScore={} | {}ms",
                    context.getCorrelationId(), RECEIPTING_NAME,
                    response != null ? response.getStatus() : "null", receitingDuration);

            return response;

        } catch (Exception ex) {
            long receitingDuration = System.currentTimeMillis() - receiptingStart;
            trackingService.logApiCall(context, STAGE, RECEIPTING_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), receitingDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), RECEIPTING_NAME, receitingDuration, ex.getMessage());

            throw new ApiCallException(RECEIPTING_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public ReceiptingResponse recover(ApiCallException ex, ReceiptingRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), RECEIPTING_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                RECEIPTING_NAME + " failed after all retries: " + ex.getMessage());
    }
}
