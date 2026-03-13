package com.insurance.newbusiness.pas;

import com.insurance.newbusiness.integration.model.proposal.ProposalResponse;
import com.insurance.newbusiness.journey.JourneyContext;
import com.insurance.newbusiness.tracking.JourneyTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PasApiClient — dedicated class for Policy Administration System submission.
 *
 * ── WHY SEPARATE FROM OTHER API CLIENTS ──────────────────────────────────────
 * PAS is the only API where:
 *   1. The request is assembled from ALL prior stage results (not just stringvalN)
 *   2. The response contains applicationNumber which must be stored in DB
 *      and passed to the reverse feed
 *
 * All other APIs use a generic typed client pattern.
 * PAS is special enough to justify its own class with buildPasRequest().
 *
 * ── PAS REQUEST ASSEMBLY ─────────────────────────────────────────────────────
 * PAS request is built in buildPasRequest() which pulls from:
 *   - context.getRawParams()             — partner's original stringvalN
 *   - context.getEligibilityResult()     — eligibility details
 *   - context.getProposalResult()        — proposal number from PROPOSAL_SUBMIT_API
 *   - context.getPremiumResult()         — final premium and frequency
 *   - context.getUnderwritingResult()    — UW decision and conditions
 *
 * TODO: Implement buildPasRequest() fully once PAS API contract is received.
 * Currently it contains placeholder field mappings.
 */
@Service
public class PasApiClient {

    private static final Logger log      = LoggerFactory.getLogger(PasApiClient.class);
    private static final String STAGE    = "PAS_SUBMISSION_STAGE";
    private static final String API_NAME = "PAS_API";

    @Value("${api.endpoints.pas}")
    private String pasUrl;

    @Autowired private RestTemplate           restTemplate;
    @Autowired private JourneyTrackingService trackingService;

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 3000, multiplier = 2))
    public String submitAndGetApplicationNumber(JourneyContext context) {
        long start = System.currentTimeMillis();
        log.info("[{}] Submitting to PAS | url={}", context.getCorrelationId(), pasUrl);

        try {
            Map<String, Object> pasRequest = buildPasRequest(context);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    pasUrl, pasRequest, Map.class);

            long duration = System.currentTimeMillis() - start;

            // TODO: replace "applicationNumber" with actual PAS response field name
            String applicationNumber = response != null
                    ? (String) response.get("applicationNumber") : null;

            if (applicationNumber == null || applicationNumber.trim().isEmpty()) {
                throw new RuntimeException("PAS returned empty or null applicationNumber");
            }

            trackingService.logApiCall(context, STAGE, API_NAME,
                    pasRequest, response, "SUCCESS", null, null, duration);

            log.info("[{}] PAS SUCCESS | applicationNumber={} | {}ms",
                    context.getCorrelationId(), applicationNumber, duration);

            return applicationNumber;

        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    null, null, "FAILED", "PAS_ERROR", ex.getMessage(), duration);
            log.error("[{}] PAS FAILED | {}ms | {}",
                    context.getCorrelationId(), duration, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Assembles PAS request from all prior journey results.
     *
     * Sources:
     *   context.getRawParams()            — original partner params
     *   context.getXxxResult()            — typed results from each prior stage
     *
     * TODO: Replace placeholder assignments with actual PAS API contract fields.
     * PAS likely needs a hierarchical JSON structure — consider creating a
     * PasRequest POJO with nested objects if PAS contract is complex.
     */
    private Map<String, Object> buildPasRequest(JourneyContext context) {
        Map<String, Object> request = new LinkedHashMap<>();

        // From partner raw params
        request.put("correlationId", context.getCorrelationId());
        request.put("partnerCode",   context.getPartnerCode());
        request.put("applicantName", context.getRawParams().get("stringval1")); // TODO: map correctly

        // From proposal stage
        if (context.getProposalResult() != null) {
            request.put("proposalNumber", context.getProposalResult().getProposalNumber());
        }

        // From premium calculation
        if (context.getPremiumResult() != null) {
            request.put("calculatedPremium", context.getPremiumResult().getCalculatedPremium());
            request.put("premiumFrequency",  context.getPremiumResult().getFrequency());
        }

        // From underwriting
        if (context.getUnderwritingResult() != null) {
            request.put("underwritingDecision", context.getUnderwritingResult().getDecision());
            request.put("underwritingConditions", context.getUnderwritingResult().getConditions());
        }

        // From medical
        if (context.getMedicalResult() != null) {
            request.put("medicalStatus",   context.getMedicalResult().getStatus());
            request.put("riskCategory",    context.getMedicalResult().getRiskCategory());
            request.put("loadingFactor",   context.getMedicalResult().getLoadingFactor());
        }

        // TODO: add all remaining fields required by PAS API contract
        return request;
    }
}
