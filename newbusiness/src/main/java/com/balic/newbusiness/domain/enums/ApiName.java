package com.balic.newbusiness.domain.enums;

/**
 * Single source of truth for every internal API's identity.
 *
 * <p>The enum constant {@link #name()} IS the {@code api_name} written to
 * journey_stage_log — and is what the skip-check ({@code succeeded.contains(...)})
 * and the JourneyResultRestorer handlers match on. These strings are PERSISTED, so
 * renaming a constant requires a data migration. {@link #stageName()} is the stage
 * label stored alongside it.
 *
 * <p>Each journey API here should have a matching entry in application.properties
 * under {@code api.endpoints}. If you add a new API, add it here first.
 */
public enum ApiName {

    // --- Sequential journey APIs (execution order) ---
    CIBIL_API("CIBIL_STAGE"),
    UCS_API("UCS_STAGE"),
    PAN_API("PAN_STAGE"),
    BI_API("BI_STAGE"),
    EKYC_API("EKYC_STAGE"),
    EDC_API("EDC_STAGE"),
    DCS_API("DCS_STAGE"),
    AUW_API("AUW_STAGE"),
    MRS_API("MRS_STAGE"),
    FES_API("FES_STAGE"),
    PROPOSAL_API("PROPOSAL_STAGE"),
    PENNYDROP_API("PENNYDROP_STAGE"),
    RECEIPTING_API("RECEIPTING_STAGE"),

    // --- Not routed through ApiCallTemplate (kept here for a single registry) ---
    NGIN_API("NGIN_STAGE"),
    PAS_API("PAS_SUBMISSION_STAGE"),
    REVERSE_FEED_API("REVERSE_FEED_STAGE");

    private final String stageName;

    ApiName(String stageName) {
        this.stageName = stageName;
    }

    /** The {@code api_name} string persisted to journey_stage_log. */
    public String apiName() {
        return name();
    }

    /** The {@code stage_name} string persisted alongside the api_name. */
    public String stageName() {
        return stageName;
    }
}
