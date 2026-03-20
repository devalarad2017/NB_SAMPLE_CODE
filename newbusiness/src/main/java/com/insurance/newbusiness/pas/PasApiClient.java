package com.insurance.newbusiness.pas;

import com.insurance.newbusiness.integration.model.pas.*;
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

import java.util.ArrayList;
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
            PasRequest pasRequest = buildPasRequest(context);

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
     * Assembles PAS request from all prior journey results using typed PasRequest POJO.
     *
     * Sources:
     *   context.getRawParams()            — original partner params
     *   context.getXxxResult()            — typed results from each prior stage
     */
    private PasRequest buildPasRequest(JourneyContext context) {
        PasRequest pasRequest = new PasRequest();
        Map<String, String> raw = context.getRawParams();

        // Build header
        PasHeader header = new PasHeader();
        header.setCorrelationId(context.getCorrelationId());
        ProcessVars processVars = new ProcessVars();
        header.setProcessVars(processVars);
        pasRequest.setHeader(header);

        // Build request body
        PasRequestBody body = new PasRequestBody();

        // Set proposal number from proposal stage
        if (context.getProposalResult() != null) {
            body.setProposalNumber(context.getProposalResult().getProposalNumber());
        }

        // Build policyCheckIn
        PolicyCheckIn policyCheckIn = new PolicyCheckIn();

        // Basic policy insured from raw params
        BasicPolicyInsured insured = new BasicPolicyInsured();
        insured.setPolicyInsuredFirstName(raw.get("stringval1"));
        insured.setPolicyInsuredLastName(raw.get("stringval2"));
        insured.setPolicyInsuredMiddleName(raw.get("stringval3"));
        insured.setGender(raw.get("stringval5"));
        insured.setSalutation(raw.get("stringval6"));
        insured.setPolicyInsuredDateOfBirth(raw.get("stringval4"));
        ArrayList<BasicPolicyInsured> insuredList = new ArrayList<>();
        insuredList.add(insured);
        policyCheckIn.setBasicPolicyInsured(insuredList);

        // Product selection
        ProductSelectionDetails productSelection = new ProductSelectionDetails();
        productSelection.setBaseCoverageCode(raw.get("stringval50"));
        policyCheckIn.setProductSelection(productSelection);

        // Bank details
        BankDetailsDTO bankDetails = new BankDetailsDTO();
        policyCheckIn.setBankDetailsDTO(bankDetails);

        // Integration results from prior stages
        ArrayList<IntegrationDetail> integrations = new ArrayList<>();

        // From underwriting
        if (context.getUnderwritingResult() != null) {
            IntegrationDetail uwIntegration = new IntegrationDetail();
            uwIntegration.setIntegrationName("AWS");
            uwIntegration.setIntegrationStatus(true);
            integrations.add(uwIntegration);
        }

        policyCheckIn.setIntegrations(integrations);

        // Journey details
        JourneyDetails journeyDetails = new JourneyDetails();
        policyCheckIn.setJourneyDetails(journeyDetails);

        // Product details from premium calculation
        ProductDetailsDTO productDetails = new ProductDetailsDTO();
        if (context.getPremiumResult() != null) {
            productDetails.setPremiumAmount(
                    context.getPremiumResult().getCalculatedPremium() != null
                            ? context.getPremiumResult().getCalculatedPremium().toPlainString() : null);
            productDetails.setPremFrequency(context.getPremiumResult().getFrequency());
        }
        policyCheckIn.setProductDetailsDTO(productDetails);

        // Medical results
        if (context.getMedicalResult() != null) {
            IntegrationDetail mrsIntegration = new IntegrationDetail();
            mrsIntegration.setIntegrationName("MRS");
            mrsIntegration.setIntegrationStatus(true);
            integrations.add(mrsIntegration);
        }

        body.setPolicyCheckIn(policyCheckIn);
        body.setStatus("DRAFT");
        pasRequest.setRequest(body);

        return pasRequest;
    }
}
