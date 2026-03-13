package com.insurance.newbusiness.integration.model.medical;

import lombok.Data;
import java.math.BigDecimal;

/**
 * MedicalResponse — received from MEDICAL_API.
 *
 * ── HOW RESPONSE FEEDS NEXT STAGES ───────────────────────────────────────────
 * After the medical call, JourneyOrchestrator does:
 *   context.setMedicalResult(response);
 *
 * When building UnderwritingRequest later:
 *   uwReq.setMedicalScore(context.getMedicalResult().getMedicalScore());
 *   uwReq.setRiskCategory(context.getMedicalResult().getRiskCategory());
 *   uwReq.setMedicalStatus(context.getMedicalResult().getStatus());
 *
 * ── KEY FIELDS THAT DOWNSTREAM APIS USE ──────────────────────────────────────
 * medicalScore  → UnderwritingRequest
 * riskCategory  → UnderwritingRequest, PremiumRequest
 * loadingFactor → PremiumRequest (extra premium loading due to health risk)
 *
 * TODO: Replace with actual MEDICAL_API response contract field names.
 */
@Data
public class MedicalResponse {
    private String     status;          // APPROVED / DECLINED / LOADED / PENDING
    private String     medicalRefId;    // medical underwriting reference
    private Integer    medicalScore;    // health score (higher = healthier)
    private String     riskCategory;    // STANDARD / SUBSTANDARD / DECLINED
    private BigDecimal loadingFactor;   // extra premium % due to health (0.0 if standard)
    private String     remarks;         // free text from medical underwriting
    private Boolean    requiresMedical; // true if physical medical exam is required
}
