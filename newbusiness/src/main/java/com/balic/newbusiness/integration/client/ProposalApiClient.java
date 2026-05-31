package com.balic.newbusiness.integration.client;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.integration.model.proposal.ProposalRequest;
import com.balic.newbusiness.integration.model.proposal.ProposalResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final Logger log       = LoggerFactory.getLogger(ProposalApiClient.class);
    private static final String STAGE     = "PROPOSAL_STAGE";
    private static final String API_NAME  = "PROPOSAL_API";

    @Value("${api.endpoints.proposal}")
    private final String url;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public ProposalResponse call(ProposalRequest request, JourneyContext context) {
        long proposalStart = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);

        try {
            log.info("PROPOSAL_API request : {}",  String.valueOf(objectMapper.writeValueAsString(request)));

            ProposalResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(ProposalResponse.class);

            log.info("PROPOSAL_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));

            long proposalDuration = System.currentTimeMillis() - proposalStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, proposalDuration);

            log.info("[{}] {} SUCCESS | proposalScore={} | {}ms",
                    context.getCorrelationId(), API_NAME,
                    response != null ? response.getStatus() : "null", proposalDuration);

            return response;

        } catch (Exception ex) {
            long proposalDuration = System.currentTimeMillis() - proposalStart;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), proposalDuration);

            log.error("[{}] {} FAILED | {}ms | {}",
                    context.getCorrelationId(), API_NAME, proposalDuration, ex.getMessage());

            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public ProposalResponse recover(ApiCallException ex, ProposalRequest request, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
