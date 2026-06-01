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
import com.balic.newbusiness.integration.model.ekyc.EkycRequest;
import com.balic.newbusiness.integration.model.ekyc.EkycResponse;
import com.balic.newbusiness.journey.JourneyContext;

@Service
@RequiredArgsConstructor
public class EkycApiClient {

    private static final Logger  log = LoggerFactory.getLogger(EkycApiClient.class);
    private static final ApiName API = ApiName.EKYC_API;

    @Value("${api.endpoints.ekyc}")
    private final String url;

    private final RestClient      restClient;
    private final ApiCallTemplate apiCallTemplate;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public EkycResponse call(EkycRequest request, JourneyContext context) {
        // eKYC is a GET — the application number is appended to the URL, no request body.
        return apiCallTemplate.execute(API, request, context, () ->
                restClient.get()
                        .uri(url + request.getApplicationNo())
                        .retrieve()
                        .body(EkycResponse.class));
    }

    @Recover
    public EkycResponse recover(ApiCallException ex, EkycRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API.apiName(), ex.getMessage());
        throw new JourneyStageException(API.stageName(),
                API.apiName() + " failed after all retries: " + ex.getMessage());
    }
}
