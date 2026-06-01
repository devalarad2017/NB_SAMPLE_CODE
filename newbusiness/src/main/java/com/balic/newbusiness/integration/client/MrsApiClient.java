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
import com.balic.newbusiness.integration.model.mrs.MrsRequest;
import com.balic.newbusiness.integration.model.mrs.MrsResponse;
import com.balic.newbusiness.journey.JourneyContext;

@Service
@RequiredArgsConstructor
public class MrsApiClient {

    private static final Logger  log = LoggerFactory.getLogger(MrsApiClient.class);
    private static final ApiName API = ApiName.MRS_API;

    @Value("${api.endpoints.mrs}")
    private final String url;

    private final RestClient      restClient;
    private final ApiCallTemplate apiCallTemplate;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public MrsResponse call(MrsRequest request, JourneyContext context) {
        return apiCallTemplate.execute(API, request, context, () ->
                restClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(MrsResponse.class));
    }

    @Recover
    public MrsResponse recover(ApiCallException ex, MrsRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API.apiName(), ex.getMessage());
        throw new JourneyStageException(API.stageName(),
                API.apiName() + " failed after all retries: " + ex.getMessage());
    }
}
