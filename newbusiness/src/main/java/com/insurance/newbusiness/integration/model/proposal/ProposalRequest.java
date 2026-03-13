package com.insurance.newbusiness.integration.model.proposal;

import lombok.Data;
import java.math.BigDecimal;

/**
 * ProposalRequest — sent to PROPOSAL_SUBMIT_API (final proposal before PAS).
 *
 * Aggregates data from all prior stages. This is the most field-rich request.
 *
 * In JourneyOrchestrator after resolveAs():
 *   req.setCalculatedPremium(context.getPremiumResult().getCalculatedPremium());
 *   req.setFrequency(context.getPremiumResult().getFrequency());
 *   req.setUnderwritingDecision(context.getUnderwritingResult().getDecision());
 *   req.setDocumentId(context.getDocumentResult().getDocumentId());
 *
 * TODO: Add all fields from PROPOSAL_SUBMIT_API contract.
 */
@Data
public class ProposalRequest {
    // From partner stringvalN
    private String     productCode;
    private BigDecimal sumAssured;
    private Integer    policyTerm;
    private String     applicantName;

    // From prior API responses
    private BigDecimal calculatedPremium;   // from PremiumResponse
    private String     frequency;           // from PremiumResponse
    private String     underwritingDecision;// from UnderwritingResponse
    private String     documentId;          // from DocumentResponse
}
