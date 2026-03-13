package com.insurance.newbusiness.integration.model.premium;

import lombok.Data;
import java.math.BigDecimal;

/**
 * PremiumRequest — sent to PREMIUM_CALC_API.
 *
 * This request is enriched with medical assessment results.
 *
 * Fields from stringvalN → partner_field_mapping with target_api = PREMIUM_CALC_API
 * Fields from prior API results → set in JourneyOrchestrator:
 *   req.setRiskCategory(context.getMedicalResult().getRiskCategory());
 *   req.setLoadingFactor(context.getMedicalResult().getLoadingFactor());
 *   req.setAgeAtEntry(context.getEligibilityResult().getAgeAtEntry());
 *
 * TODO: Add all fields from PREMIUM_CALC_API contract.
 */
@Data
public class PremiumRequest {
    // From partner stringvalN
    private BigDecimal sumAssured;
    private Integer    policyTerm;
    private String     productCode;
    private String     smokingStatus;

    // From prior API responses (set manually in JourneyOrchestrator)
    private Integer    ageAtEntry;      // from EligibilityResponse
    private String     riskCategory;    // from MedicalResponse
    private BigDecimal loadingFactor;   // from MedicalResponse
}
