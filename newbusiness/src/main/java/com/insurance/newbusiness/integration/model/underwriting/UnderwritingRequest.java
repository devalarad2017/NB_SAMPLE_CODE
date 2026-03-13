package com.insurance.newbusiness.integration.model.underwriting;

import lombok.Data;
import java.math.BigDecimal;

/**
 * UnderwritingRequest — sent to UNDERWRITING_API.
 *
 * Heavily enriched from prior API results. Most fields here come from
 * previous stages, not from partner's stringvalN params.
 *
 * In JourneyOrchestrator:
 *   UnderwritingRequest req = mappingService.resolveAs(..., UnderwritingRequest.class);
 *   // Enrich with scoring results
 *   req.setCreditScore(context.getEdcResult().getCreditScore());
 *   req.setPasaScore(context.getPasaResult().getPasaScore());
 *   req.setTasaRiskCategory(context.getTasaResult().getRiskCategory());
 *   // Enrich with medical results
 *   req.setMedicalScore(context.getMedicalResult().getMedicalScore());
 *   req.setMedicalRiskCategory(context.getMedicalResult().getRiskCategory());
 *
 * TODO: Add all fields from UNDERWRITING_API contract.
 */
@Data
public class UnderwritingRequest {
    // From partner stringvalN
    private BigDecimal sumAssured;
    private Integer    policyTerm;
    private String     productCode;

    // From prior API responses
    private Integer creditScore;          // from EdcResponse
    private Integer pasaScore;            // from PasaResponse
    private String  tasaRiskCategory;     // from TasaResponse
    private Integer medicalScore;         // from MedicalResponse
    private String  medicalRiskCategory;  // from MedicalResponse
    private BigDecimal loadingFactor;     // from MedicalResponse
}
