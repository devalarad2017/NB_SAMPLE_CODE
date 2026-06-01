package com.balic.newbusiness.integration.client;

import com.balic.newbusiness.domain.enums.ApiName;
import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.proposal.ProposalRequest;
import com.balic.newbusiness.integration.model.proposal.ProposalResponse;
import com.balic.newbusiness.journey.JourneyContext;
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
public class ProposalApiClient {

    private static final Logger  log = LoggerFactory.getLogger(ProposalApiClient.class);
    private static final ApiName API = ApiName.PROPOSAL_API;

    @Value("${api.endpoints.proposal}")
    private final String url;

    private final RestClient      restClient;
    private final ApiCallTemplate apiCallTemplate;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public ProposalResponse call(ProposalRequest request, JourneyContext context) {
        return apiCallTemplate.execute(API, request, context, () ->
                restClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(ProposalResponse.class));
    }

    @Recover
    public ProposalResponse recover(ApiCallException ex, ProposalRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}",
                context.getCorrelationId(), API.apiName(), ex.getMessage());
        throw new JourneyStageException(API.stageName(),
                API.apiName() + " failed after all retries: " + ex.getMessage());
    }
}
