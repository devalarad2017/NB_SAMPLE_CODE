package com.insurance.newbusiness.integration.model.scoring;

import lombok.Data;
import java.math.BigDecimal;

/**
 * ScoringRequest — sent to EDC_API, PASA_API, and TASA_API.
 *
 * ── WHY ONE REQUEST CLASS FOR THREE APIS ─────────────────────────────────────
 * EDC (credit check), PASA (financial score), TASA (risk score) all need
 * broadly the same applicant and financial data. Using one shared request
 * class avoids three near-identical POJOs.
 *
 * If one of these APIs diverges significantly in future, create a separate
 * request class for that API and update ScoringApiClient accordingly.
 *
 * ── PARALLEL EXECUTION ───────────────────────────────────────────────────────
 * All three scoring APIs run concurrently in SCORING_STAGE using
 * CompletableFuture.allOf(). All must complete before MEDICAL_STAGE begins.
 * See JourneyOrchestrator.executeScoringStage() for the parallel flow.
 *
 * ── DB MAPPING ───────────────────────────────────────────────────────────────
 * target_api in partner_field_mapping = "SCORING_API" (shared).
 * MappingService resolves once and the same request is sent to all three.
 *
 * If EDC needs a different field that others don't:
 *   Option A: Add it here (EDC sends it, others ignore it)
 *   Option B: Create ScoringRequest + EdcRequest separately
 */
@Data
public class ScoringRequest {

    // ── Applicant identity ────────────────────────────────────────────────────
    private String panNumber;       // stringval45 → UPPERCASE | mandatory
    private String firstName;       // stringval1 → TRIM
    private String lastName;        // stringval2 → TRIM
    private java.time.LocalDate dateOfBirth; // stringval4 → DATE

    // ── Financial data ────────────────────────────────────────────────────────
    private BigDecimal annualIncome;    // stringval60 → DECIMAL
    private BigDecimal existingLoans;   // stringval61 → DECIMAL | default_value = 0
    private String     employmentType;  // stringval62 → UPPERCASE (SALARIED/SELF_EMPLOYED)

    // ── Policy details ────────────────────────────────────────────────────────
    private BigDecimal sumAssured;  // stringval52 → DECIMAL | mandatory
    private Integer    policyTerm;  // stringval51 → INTEGER | mandatory
}
