package com.insurance.newbusiness.integration.model.kyc;

import lombok.Data;
import java.time.LocalDate;

/**
 * KycRequest — sent to KYC_API.
 *
 * TODO: Add fields matching KYC_API contract.
 * Fields from partner stringvalN → add rows to partner_field_mapping with target_api = KYC_API
 * Fields from prior API responses → set manually in JourneyOrchestrator after resolveAs()
 *
 * Pattern (see MedicalRequest for full example):
 *   KycRequest req = mappingService.resolveAs(partnerCode, "KYC_API", rawParams, KycRequest.class);
 *   req.setEligibilityId(context.getEligibilityResult().getEligibilityId()); // from prior stage
 */
@Data
public class KycRequest {
    // TODO: Add fields from KYC_API contract
    private String panNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String aadhaarNumber;   // if applicable
    private String addressLine1;
    private String city;
    private String pincode;
}
