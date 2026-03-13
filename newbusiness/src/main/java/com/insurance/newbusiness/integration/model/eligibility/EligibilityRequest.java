package com.insurance.newbusiness.integration.model.eligibility;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

/**
 * EligibilityRequest — sent to ELIGIBILITY_API.
 *
 * ── HOW THIS POJO IS POPULATED ───────────────────────────────────────────────
 * Fields here are filled by ParameterMappingService.resolveAs() which reads
 * the partner_field_mapping DB table and maps stringvalN → this POJO.
 *
 * CRITICAL RULE: Every field name here MUST exactly match the target_field
 * value in the partner_field_mapping table for ELIGIBILITY_API.
 * Jackson uses field names to map the resolved Map into this POJO.
 *
 * DB row example (partner_field_mapping):
 *   source_param  = stringval1
 *   target_api    = ELIGIBILITY_API
 *   target_field  = firstName         ← must match field name below
 *   data_type     = STRING
 *   is_mandatory  = true
 *   transformation = TRIM
 *
 * ── ADDING A NEW FIELD ────────────────────────────────────────────────────────
 * 1. Add field here with correct Java type
 * 2. Add a row in partner_field_mapping with target_field = exact field name
 * 3. No other code change needed
 *
 * ── REMOVING A FIELD ──────────────────────────────────────────────────────────
 * 1. Remove field here
 * 2. Delete/deactivate the row in partner_field_mapping (set is_active = false)
 * 3. No other code change needed
 */
@Data
public class EligibilityRequest {

    // ── Applicant personal details ────────────────────────────────────────────
    // target_field in DB must match these names exactly

    private String firstName;       // stringval1 → TRIM
    private String lastName;        // stringval2 → TRIM
    private String middleName;      // stringval3 → TRIM (not mandatory)

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;  // stringval4 → DATE (dd/MM/yyyy in DB)

    private String gender;          // stringval5 → UPPERCASE | default_value = MALE
    private String maritalStatus;   // stringval6 → UPPERCASE (SINGLE/MARRIED/DIVORCED)
    private String nationality;     // stringval7 → UPPERCASE | default_value = INDIAN

    // ── Contact details ───────────────────────────────────────────────────────
    private String mobileNumber;    // stringval10 → mandatory
    private String emailAddress;    // stringval11 → LOWERCASE

    // ── Identity ──────────────────────────────────────────────────────────────
    private String panNumber;       // stringval45 → UPPERCASE | mandatory

    // ── Policy intent ────────────────────────────────────────────────────────
    private String productCode;     // stringval50 → mandatory
    private Integer policyTerm;     // stringval51 → INTEGER | mandatory
    private java.math.BigDecimal sumAssured; // stringval52 → DECIMAL | mandatory
}
