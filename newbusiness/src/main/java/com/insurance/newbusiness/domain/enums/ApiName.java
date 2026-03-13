package com.insurance.newbusiness.domain.enums;

/**
 * Canonical list of all internal API names.
 *
 * Each name here must have a matching entry in application.properties under api.endpoints
 * and matching rows in the partner_field_mapping table.
 *
 * WHY AN ENUM: Prevents typos when referencing api names in JourneyOrchestrator
 * and partner_field_mapping. If you add a new API, add it here first.
 */
public enum ApiName {

    // --- Scoring APIs (run in parallel) ---
    EDC_API,
    PASA_API,
    TASA_API,

    // --- Sequential journey APIs ---
    ELIGIBILITY_API,
    MEDICAL_API,
    KYC_API,
    PREMIUM_CALC_API,
    PROPOSAL_SUBMIT_API,
    UNDERWRITING_API,
    DOCUMENT_API,

    // --- PAS and reverse feed ---
    PAS_API,
    REVERSE_FEED_API
}
