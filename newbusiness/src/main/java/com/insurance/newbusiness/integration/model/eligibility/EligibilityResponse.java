package com.insurance.newbusiness.integration.model.eligibility;

import lombok.Data;

/**
 * EligibilityResponse — received from ELIGIBILITY_API.
 *
 * ── HOW THIS POJO IS POPULATED ───────────────────────────────────────────────
 * RestTemplate.postForObject(url, request, EligibilityResponse.class) deserialises
 * the JSON response body into this POJO automatically.
 *
 * Field names here must match the JSON field names returned by ELIGIBILITY_API.
 * Use @JsonProperty if the API returns snake_case and you want camelCase here.
 *
 * ── HOW RESPONSE IS USED ─────────────────────────────────────────────────────
 * After the call:
 *   context.setEligibilityResult(response);
 *
 * Later in JourneyOrchestrator when building downstream requests:
 *   String status = context.getEligibilityResult().getStatus();
 *
 * ── ADDING A NEW RESPONSE FIELD ──────────────────────────────────────────────
 * 1. Add field here
 * 2. No DB or mapping change needed — response fields don't go through mapping table
 * 3. Use in JourneyOrchestrator if the field feeds a later API request
 */
@Data
public class EligibilityResponse {

    // TODO: Replace with actual field names from ELIGIBILITY_API contract
    private String  status;         // ELIGIBLE / NOT_ELIGIBLE / PENDING_REVIEW
    private String  eligibilityId;  // unique ID assigned by eligibility service
    private String  reason;         // populated when status = NOT_ELIGIBLE
    private Integer ageAtEntry;     // calculated by eligibility service
    private Boolean smokingFlag;    // detected from application data
}
