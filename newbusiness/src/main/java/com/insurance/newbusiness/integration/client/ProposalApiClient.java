package com.insurance.newbusiness.integration.client;

import com.insurance.newbusiness.exception.ApiCallException;
import com.insurance.newbusiness.exception.JourneyStageException;
import com.insurance.newbusiness.integration.model.proposal.ProposalRequest;
import com.insurance.newbusiness.integration.model.proposal.ProposalResponse;
import com.insurance.newbusiness.journey.JourneyContext;
import com.insurance.newbusiness.tracking.JourneyTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * ProposalApiClient — calls PROPOSAL_API.
 *
 * TODO: Skeleton — follow the same pattern as EligibilityApiClient (full example).
 * Step 1: Confirm API contract → fill ProposalRequest and ProposalResponse POJOs
 * Step 2: Add partner_field_mapping rows with target_api = PROPOSAL_API
 * Step 3: Wire into JourneyOrchestrator in the correct stage position
 */
@Service
public class ProposalApiClient {

    private static final Logger log      = LoggerFactory.getLogger(ProposalApiClient.class);
    private static final String STAGE    = "PROPOSAL_STAGE";
    private static final String API_NAME = "PROPOSAL_API";

    @Value("${api.endpoints.proposal}")
    private String url;

    @Autowired private RestTemplate           restTemplate;
    @Autowired private JourneyTrackingService trackingService;

    @Retryable(value = {ApiCallException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public ProposalResponse call(ProposalRequest request, JourneyContext context) {
        long start = System.currentTimeMillis();
        log.info("[{}] Calling {} | url={}", context.getCorrelationId(), API_NAME, url);
        try {
            ProposalResponse response = restTemplate.postForObject(url, request, ProposalResponse.class);
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, response, "SUCCESS", null, null, duration);
            log.info("[{}] {} SUCCESS | {}ms", context.getCorrelationId(), API_NAME, duration);
            return response;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    request, null, "FAILED", "HTTP_ERROR", ex.getMessage(), duration);
            log.error("[{}] {} FAILED | {}ms | {}", context.getCorrelationId(), API_NAME, duration, ex.getMessage());
            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
        }
    }

    @Recover
    public ProposalResponse recover(ApiCallException ex, ProposalRequest req, JourneyContext ctx) {
        log.error("[{}] {} — all retries exhausted: {}", ctx.getCorrelationId(), API_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE, API_NAME + " failed after all retries: " + ex.getMessage());
    }
}
