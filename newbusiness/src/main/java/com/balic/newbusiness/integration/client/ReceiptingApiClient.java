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

import com.balic.newbusiness.domain.enums.ApiName;
import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.receipting.ReceiptingRequest;
import com.balic.newbusiness.integration.model.receipting.ReceiptingResponse;
import com.balic.newbusiness.journey.JourneyContext;

@Service
@RequiredArgsConstructor
public class ReceiptingApiClient {

    private static final Logger  log = LoggerFactory.getLogger(ReceiptingApiClient.class);
    private static final ApiName API = ApiName.RECEIPTING_API;

    @Value("${api.endpoints.receipting}")
    private final String url;

    private final RestClient         restClient;
    private final ApiCallTemplate    apiCallTemplate;
    private final NGINTokenAPIClient nginToken;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public ReceiptingResponse call(ReceiptingRequest request, JourneyContext context) {
        // Token generation stays inside the lambda so a token failure is timed,
        // tracked and retried exactly like an HTTP failure (same as before).
        return apiCallTemplate.execute(API, request, context, () -> {
            String token = nginToken.GenerateNginTokenOSB();
            return restClient.post()
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
        });
    }

    @Recover
    public ReceiptingResponse recover(ApiCallException ex, ReceiptingRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API.apiName(), ex.getMessage());
        throw new JourneyStageException(API.stageName(),
                API.apiName() + " failed after all retries: " + ex.getMessage());
    }
}
