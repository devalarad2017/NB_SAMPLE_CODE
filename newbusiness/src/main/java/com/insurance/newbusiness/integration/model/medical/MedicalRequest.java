package com.insurance.newbusiness.integration.model.medical;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MedicalRequest — sent to MEDICAL_API.
 *
 * ── FIELDS FROM TWO SOURCES ───────────────────────────────────────────────────
 * This request is built in two steps in JourneyOrchestrator:
 *
 * Step 1 — resolveAs() fills fields from stringvalN via DB mapping:
 *   MedicalRequest req = mappingService.resolveAs(
 *       partnerCode, "MEDICAL_API", rawParams, MedicalRequest.class);
 *   → fills: firstName, lastName, dateOfBirth, panNumber, sumAssured, smokingStatus etc.
 *
 * Step 2 — Orchestrator enriches from prior API results:
 *   req.setEligibilityId(context.getEligibilityResult().getEligibilityId());
 *   req.setAgeAtEntry(context.getEligibilityResult().getAgeAtEntry());
 *   → fields like eligibilityId and ageAtEntry come from ELIGIBILITY_API response
 *   → they are NOT in partner_field_mapping (partner doesn't send them)
 *
 * ── WHICH FIELDS NEED DB MAPPING ROWS ────────────────────────────────────────
 * Fields that come from partner's stringvalN → need a row in partner_field_mapping
 * Fields that come from prior API responses  → set manually in JourneyOrchestrator
 *
 * This is documented per field in the comments below.
 */
@Data
public class MedicalRequest {

    // ── From partner stringvalN (needs DB mapping rows) ───────────────────────
    private String firstName;           // stringval1  → TRIM  | mandatory
    private String lastName;            // stringval2  → TRIM  | mandatory

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;      // stringval4  → DATE  | mandatory

    private String panNumber;           // stringval45 → UPPERCASE | mandatory
    private BigDecimal sumAssured;      // stringval52 → DECIMAL | mandatory
    private Integer policyTerm;         // stringval51 → INTEGER | mandatory
    private String smokingStatus;       // stringval70 → UPPERCASE | default_value = NON_SMOKER
    private String alcoholConsumption;  // stringval71 → UPPERCASE | default_value = NONE
    private String existingConditions;  // stringval72 (not mandatory — partner may not know)
    private BigDecimal height;          // stringval73 → DECIMAL (cm) | not mandatory
    private BigDecimal weight;          // stringval74 → DECIMAL (kg) | not mandatory

    // ── From prior API responses (set manually in JourneyOrchestrator) ────────
    // These are NOT in partner_field_mapping — no DB row needed for these.

    // From EligibilityResponse — set by orchestrator after ELIGIBILITY_STAGE
    private String  eligibilityId;  // traceability — link medical check to eligibility check
    private Integer ageAtEntry;     // calculated by eligibility service, reused here

    // From EdcResponse — set by orchestrator after SCORING_STAGE
    private Integer creditScore;    // some medical APIs use credit score for premium loading
}
