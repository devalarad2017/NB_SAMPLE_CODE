package com.insurance.newbusiness.pas;

import com.insurance.newbusiness.integration.model.pas.*;
import com.insurance.newbusiness.journey.JourneyContext;
import com.insurance.newbusiness.tracking.JourneyTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;

/**
 * PasApiClient — dedicated class for Policy Administration System submission.
 *
 * ── WHY SEPARATE FROM OTHER API CLIENTS ──────────────────────────────────────
 * PAS is the only API where:
 *   1. The request is assembled from ALL prior stage results (not just stringvalN)
 *   2. The response contains applicationNumber which must be stored in DB
 *      and passed to the reverse feed
 *
 * All other APIs use a generic typed client pattern.
 * PAS is special enough to justify its own class with buildPasRequest().
 *
 * ── PAS REQUEST ASSEMBLY ─────────────────────────────────────────────────────
 * PAS request is built in buildPasRequest() which pulls from:
 *   - context.getRawParams()             — partner's original stringvalN
 *   - context.getEligibilityResult()     — eligibility details
 *   - context.getProposalResult()        — proposal number from PROPOSAL_SUBMIT_API
 *   - context.getPremiumResult()         — final premium and frequency
 *   - context.getUnderwritingResult()    — UW decision and conditions
 *
 * ── RAW PARAM (stringvalN) MAPPING ───────────────────────────────────────────
 * pInObj1_inout:
 *   stringval12  — salutation
 *   stringval13  — IP first name
 *   stringval14  — IP middle name
 *   stringval15  — IP last name
 *   stringval16  — IP date of birth
 *   stringval18  — IP gender            (M / F  →  mapped to MALE / FEMALE)
 *   stringval19  — mobile number
 *   stringval20  — email address
 *   stringval31  — base coverage code
 *   stringval46  — PH name
 *   stringval47  — PH relationship to IP
 *   stringval48  — PH date of birth
 *   stringval49  — PH gender            (M / F  →  mapped to MALE / FEMALE)
 *   stringval99  — marital status       (M  →  mapped to MARRIED)
 *   stringval117 — Aadhar number        (preferred ID proof; empty = use PAN)
 *   stringval122 — PAN number           (fallback ID proof when Aadhar absent)
 *   stringval130 — annual income
 *   stringval133 — occupation
 */
@Service
public class PasApiClient {

    private static final Logger log      = LoggerFactory.getLogger(PasApiClient.class);
    private static final String STAGE    = "PAS_SUBMISSION_STAGE";
    private static final String API_NAME = "PAS_API";

    private static final String AADHAR_CODE = "AADHAR_REFERENCE_CODE";
    private static final String PAN_CODE    = "PAN";

    @Value("${api.endpoints.pas}")
    private String pasUrl;

    @Autowired private RestTemplate           restTemplate;
    @Autowired private JourneyTrackingService trackingService;

    @Retryable(value = {RuntimeException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 3000, multiplier = 2))
    public String submitAndGetApplicationNumber(JourneyContext context) {
        long start = System.currentTimeMillis();
        log.info("[{}] Submitting to PAS | url={}", context.getCorrelationId(), pasUrl);

        try {
            PasRequest pasRequest = buildPasRequest(context);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    pasUrl, pasRequest, Map.class);

            long duration = System.currentTimeMillis() - start;

            String applicationNumber = response != null
                    ? (String) response.get("applicationNumber") : null;

            if (applicationNumber == null || applicationNumber.trim().isEmpty()) {
                throw new RuntimeException("PAS returned empty or null applicationNumber");
            }

            trackingService.logApiCall(context, STAGE, API_NAME,
                    pasRequest, response, "SUCCESS", null, null, duration);

            log.info("[{}] PAS SUCCESS | applicationNumber={} | {}ms",
                    context.getCorrelationId(), applicationNumber, duration);

            return applicationNumber;

        } catch (RuntimeException ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, STAGE, API_NAME,
                    null, null, "FAILED", "PAS_ERROR", ex.getMessage(), duration);
            log.error("[{}] PAS FAILED | {}ms | {}",
                    context.getCorrelationId(), duration, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Assembles the full PAS request from all prior journey results.
     */
    private PasRequest buildPasRequest(JourneyContext context) {

        PasRequest pasRequest = new PasRequest();
        Map<String, String> raw = context.getRawParams();

        // ── Header ────────────────────────────────────────────────────────────
        PasHeader header = new PasHeader();
        header.setCorrelationId(context.getCorrelationId());
        header.setProcessVars(new ProcessVars());
        pasRequest.setHeader(header);

        // ── Request body ──────────────────────────────────────────────────────
        PasRequestBody body = new PasRequestBody();

        if (context.getProposalResult() != null) {
            body.setProposalNumber(context.getProposalResult().getProposalNumber());
        }

        PolicyCheckIn policyCheckIn = new PolicyCheckIn();

        // ── basicPolicyInsured ────────────────────────────────────────────────
        BasicPolicyInsured insured = buildBasicPolicyInsured(raw);
        ArrayList<BasicPolicyInsured> insuredList = new ArrayList<>();
        insuredList.add(insured);
        policyCheckIn.setBasicPolicyInsured(insuredList);

        // ── productSelection ──────────────────────────────────────────────────
        ProductSelectionDetails productSelection = new ProductSelectionDetails();
        productSelection.setBaseCoverageCode(raw.get("stringval31"));
        policyCheckIn.setProductSelection(productSelection);

        // ── bankDetailsDTO ────────────────────────────────────────────────────
        policyCheckIn.setBankDetailsDTO(new BankDetailsDTO());

        // ── integration results from prior stages ─────────────────────────────
        ArrayList<IntegrationDetail> integrations = new ArrayList<>();
        if (context.getUnderwritingResult() != null) {
            IntegrationDetail uwIntegration = new IntegrationDetail();
            uwIntegration.setIntegrationName("AWS");
            uwIntegration.setIntegrationStatus(true);
            integrations.add(uwIntegration);
        }
        if (context.getMedicalResult() != null) {
            IntegrationDetail mrsIntegration = new IntegrationDetail();
            mrsIntegration.setIntegrationName("MRS");
            mrsIntegration.setIntegrationStatus(true);
            integrations.add(mrsIntegration);
        }
        policyCheckIn.setIntegrations(integrations);

        // ── journeyDetails ────────────────────────────────────────────────────
        policyCheckIn.setJourneyDetails(new JourneyDetails());

        // ── habbitDetailsDTO — fix 5: default all lifestyle flags to false ────
        policyCheckIn.setHabbitDetailsDTO(buildHabbitDetails());

        // ── IP details ────────────────────────────────────────────────────────
        policyCheckIn.setIpDetails(buildIpDetails(raw));

        // ── PH details ────────────────────────────────────────────────────────
        policyCheckIn.setPhDetails(buildPhDetails(raw));

        // ── Payer details ─────────────────────────────────────────────────────
        policyCheckIn.setPayerDetails(buildPayerDetails(raw));

        // ── productDetailsDTO — fix 9: ppt=0, riderDetails=empty list ─────────
        policyCheckIn.setProductDetailsDTO(buildProductDetails(context, raw));

        body.setPolicyCheckIn(policyCheckIn);
        body.setStatus("DRAFT");
        pasRequest.setRequest(body);

        return pasRequest;
    }

    // ── Section builders ──────────────────────────────────────────────────────

    /**
     * Builds basicPolicyInsured.
     *
     * Fix 2: policyInsuredLegalIdentifierCode — use AADHAR_REFERENCE_CODE when Aadhar
     *         is present; fall back to PAN when Aadhar is absent.
     * Fix 3: gender — map "M"/"F" to full strings "MALE"/"FEMALE".
     */
    private BasicPolicyInsured buildBasicPolicyInsured(Map<String, String> raw) {
        BasicPolicyInsured insured = new BasicPolicyInsured();
        insured.setSalutation(raw.get("stringval12"));
        insured.setPolicyInsuredFirstName(raw.get("stringval13"));
        insured.setPolicyInsuredMiddleName(raw.get("stringval14"));
        insured.setPolicyInsuredLastName(raw.get("stringval15"));
        insured.setPolicyInsuredDateOfBirth(raw.get("stringval16"));
        insured.setGender(mapGender(raw.get("stringval18")));   // fix 3

        String aadhar = raw.get("stringval117");
        if (aadhar != null && !aadhar.trim().isEmpty()) {       // fix 2
            insured.setPolicyInsuredLegalIdentifierCode(AADHAR_CODE);
            insured.setPolicyInsuredLegalIdentifierValue(aadhar);
        } else {
            insured.setPolicyInsuredLegalIdentifierCode(PAN_CODE);
            insured.setPolicyInsuredLegalIdentifierValue(raw.get("stringval122"));
        }

        return insured;
    }

    /**
     * Builds habbitDetailsDTO.
     *
     * Fix 5: all lifestyle boolean flags must be false (not null) for PAS to accept the request.
     */
    private HabbitDetailsDTO buildHabbitDetails() {
        HabbitDetailsDTO habbitDetails = new HabbitDetailsDTO();
        habbitDetails.setIsAlcohol(false);
        habbitDetails.setIsChangeInWeight(false);
        habbitDetails.setIsDGH(false);
        habbitDetails.setIsPEP(false);
        habbitDetails.setIsSmoker(false);
        habbitDetails.setIsTobacco(false);
        return habbitDetails;
    }

    /**
     * Builds IP (insured person) details.
     *
     * Fix 1: email in ContactDetails must be wrapped in an EmailAddress object.
     * Fix 3: gender as full string.
     * Fix 4: maritalStatus — map "M" to "MARRIED".
     * Fix 2: ID proof — prefer Aadhar; fall back to PAN.
     */
    private IpDetails buildIpDetails(Map<String, String> raw) {
        PersonDetails ipBasicDetails = new PersonDetails();
        ipBasicDetails.setSalutation(raw.get("stringval12"));
        ipBasicDetails.setFirstName(raw.get("stringval13"));
        ipBasicDetails.setMiddleName(raw.get("stringval14"));
        ipBasicDetails.setLastName(raw.get("stringval15"));
        ipBasicDetails.setDateOfBirth(raw.get("stringval16"));
        ipBasicDetails.setGender(mapGender(raw.get("stringval18")));             // fix 3
        ipBasicDetails.setMaritalStatus(mapMaritalStatus(raw.get("stringval99"))); // fix 4

        String aadhar = raw.get("stringval117");
        if (aadhar != null && !aadhar.trim().isEmpty()) {   // fix 2
            ipBasicDetails.setIdProofDoc(AADHAR_CODE);
            ipBasicDetails.setIdProofValue(aadhar);
        } else {
            ipBasicDetails.setIdProofDoc(PAN_CODE);
            ipBasicDetails.setIdProofValue(raw.get("stringval122"));
        }

        IpPersonalDetailsWrapper ipPersonalDetails = new IpPersonalDetailsWrapper();
        ipPersonalDetails.setIpBasicDetails(ipBasicDetails);
        ipPersonalDetails.setContactDetails(
                buildContactDetails(raw.get("stringval20"), raw.get("stringval19"))); // fix 1

        IpDetails ipDetails = new IpDetails();
        ipDetails.setIppersonalDetails(ipPersonalDetails);
        return ipDetails;
    }

    /**
     * Builds PH (policyholder) details.
     *
     * Fix 1: email wrapped in EmailAddress object.
     * Fix 7: phEducationAndOccupationDetails.occupationDetails — annualIncome must be set.
     * Fix 8: basicPersonDetails — gender as full string, ID proof with Aadhar-or-PAN logic,
     *        relationshipToIP as empty string instead of null.
     */
    private PhDetails buildPhDetails(Map<String, String> raw) {
        PersonDetails phBasicDetails = new PersonDetails();
        phBasicDetails.setFirstName(raw.get("stringval46"));
        phBasicDetails.setDateOfBirth(raw.get("stringval48"));
        phBasicDetails.setGender(mapGender(raw.get("stringval49")));  // fix 8: full gender

        String aadhar = raw.get("stringval117");
        if (aadhar != null && !aadhar.trim().isEmpty()) {   // fix 8: Aadhar-or-PAN
            phBasicDetails.setIdProofDoc(AADHAR_CODE);
            phBasicDetails.setIdProofValue(aadhar);
        } else {
            phBasicDetails.setIdProofDoc(PAN_CODE);
            phBasicDetails.setIdProofValue(raw.get("stringval122"));
        }

        PhPersonalDetailsWrapper phPersonalDetails = new PhPersonalDetailsWrapper();
        phPersonalDetails.setBasicPersonDetails(phBasicDetails);
        phPersonalDetails.setContactDetails(
                buildContactDetails(raw.get("stringval20"), raw.get("stringval19"))); // fix 1

        // fix 7: annualIncome in occupationDetails
        OccupationDetails occupationDetails = new OccupationDetails();
        occupationDetails.setAnnualIncome(raw.get("stringval130"));
        occupationDetails.setOccupation(raw.get("stringval133"));

        EducationAndOccupationDetails phEduOcc = new EducationAndOccupationDetails();
        phEduOcc.setOccupationDetails(occupationDetails);

        PhDetails phDetails = new PhDetails();
        phDetails.setPhPersonalDetails(phPersonalDetails);
        phDetails.setPhEducationAndOccupationDetails(phEduOcc);

        // fix 8: empty string instead of null for relationshipToIP
        String relationshipToIP = raw.get("stringval47");
        phDetails.setRelationshipToIP(relationshipToIP != null ? relationshipToIP : "");

        return phDetails;
    }

    /**
     * Builds payer details.
     *
     * Fix 6: payerPersonalDetails.basicPersonDetails — gender must be the full string "MALE"/"FEMALE".
     */
    private PayerDetails buildPayerDetails(Map<String, String> raw) {
        PersonDetails payerBasicDetails = new PersonDetails();
        payerBasicDetails.setGender(mapGender(raw.get("stringval49")));  // fix 6: full gender

        PayerPersonalDetailsWrapper payerPersonalDetails = new PayerPersonalDetailsWrapper();
        payerPersonalDetails.setBasicPersonDetails(payerBasicDetails);
        payerPersonalDetails.setContactDetails(
                buildContactDetails(raw.get("stringval20"), raw.get("stringval19"))); // fix 1

        PayerDetails payerDetails = new PayerDetails();
        payerDetails.setPayerPersonalDetails(payerPersonalDetails);
        return payerDetails;
    }

    /**
     * Builds productDetailsDTO.
     *
     * Fix 9: ppt must be 0 (not null) and riderDetails must be an empty list
     *        (not null, and not a list containing null-field objects).
     */
    private ProductDetailsDTO buildProductDetails(JourneyContext context, Map<String, String> raw) {
        ProductDetailsDTO productDetails = new ProductDetailsDTO();

        if (context.getPremiumResult() != null) {
            productDetails.setPremiumAmount(
                    context.getPremiumResult().getCalculatedPremium() != null
                            ? context.getPremiumResult().getCalculatedPremium().toPlainString() : null);
            productDetails.setPremFrequency(context.getPremiumResult().getFrequency());
        }

        productDetails.setPpt(0);                       // fix 9: 0 instead of null
        productDetails.setRiderDetails(new ArrayList<>()); // fix 9: empty list, never null-entry list

        return productDetails;
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    /**
     * Builds a ContactDetails with email and mobile wrapped in their typed objects.
     *
     * Fix 1: emailId must be an EmailAddress object — passing a bare String causes
     *        incorrect JSON serialization and PAS rejects the request.
     */
    private ContactDetails buildContactDetails(String email, String mobile) {
        ContactDetails contactDetails = new ContactDetails();

        if (email != null && !email.trim().isEmpty()) {
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.setAddress(email);
            contactDetails.setEmailId(emailAddress);
        }

        if (mobile != null && !mobile.trim().isEmpty()) {
            PhoneNumber phoneNumber = new PhoneNumber();
            phoneNumber.setNumber(mobile);
            contactDetails.setMobileNumber(phoneNumber);
        }

        return contactDetails;
    }

    /**
     * Maps single-character gender code to full PAS-accepted value.
     *
     * Fix 3, 6, 8: PAS requires "MALE" / "FEMALE" — not "M" / "F".
     */
    private String mapGender(String genderCode) {
        if ("M".equalsIgnoreCase(genderCode)) return "MALE";
        if ("F".equalsIgnoreCase(genderCode)) return "FEMALE";
        return genderCode;
    }

    /**
     * Maps single-character marital status code to full PAS-accepted value.
     *
     * Fix 4: PAS requires "MARRIED" — not "M".
     */
    private String mapMaritalStatus(String maritalCode) {
        if ("M".equalsIgnoreCase(maritalCode)) return "MARRIED";
        if ("S".equalsIgnoreCase(maritalCode)) return "SINGLE";
        if ("D".equalsIgnoreCase(maritalCode)) return "DIVORCED";
        if ("W".equalsIgnoreCase(maritalCode)) return "WIDOWED";
        return maritalCode;
    }
}
