# API Mappings Master Document
## New Business Journey — All API Field Mappings

**Purpose:** God document for BA and Developer to track, review, and correct field mappings across all 12 APIs in the New Business journey.
**Last Updated:** 2026-06-02
**Source files:** `JourneyOrchestrator.java`, `PasApiClient.java`, `InboundRequest.java`, integration model POJOs

---

## Legend

| Symbol | Meaning |
|--------|---------|
| `stringvalN` | Generic key from partner's inbound request (InboundRequest.params map) |
| **ENRICHED** | Field is NOT from partner input — set programmatically from a prior API's response |
| **HARDCODED** | Value is fixed in code, not configurable via DB mapping |
| **DEFAULT** | Partner may omit the field; DB mapping supplies a fallback value |
| **LOV** | List of Values — only these values are accepted |
| **TODO** | Mapping not yet implemented or confirmed; needs BA/Dev action |
| Fix N | References a known production fix documented in `PasApiClient.java` |

---

## Journey Stage Execution Order

```
1. ELIGIBILITY_API        (Stage 1 — sequential)
2. EDC_API                (Stage 2 — parallel)
   PASA_API               (Stage 2 — parallel, same ScoringRequest)
   TASA_API               (Stage 2 — parallel, same ScoringRequest)
3. MEDICAL_API            (Stage 3 — sequential)
4. KYC_API                (Stage 4 — sequential)
5. PREMIUM_CALC_API       (Stage 5 — sequential)
6. UNDERWRITING_API       (Stage 6 — sequential)
7. DOCUMENT_API           (Stage 7 — sequential)
8. PROPOSAL_SUBMIT_API    (Stage 8 — sequential)
9. PAS_API                (Post-Journey — handled in PasApiClient)
10. REVERSE_FEED_API      (Post-PAS — handled in DefaultPartnerNotifier)
```

> **Retry behaviour:** Any failed stage can be resumed. `journey_stage_log` records each API call with `status = SUCCESS / FAILED`. On retry, stages with `SUCCESS` in the log are skipped.

---

## How Mappings Work

Partner sends up to 600 generic params (`stringval1` … `stringvalN`).
The `partner_field_mapping` DB table translates them to typed API fields:

```
partner_field_mapping columns:
  partner_code   — e.g. PARTNER_A
  source_param   — e.g. stringval1
  target_api     — e.g. ELIGIBILITY_API
  target_field   — e.g. firstName  (must EXACTLY match Java field name in POJO)
  data_type      — STRING / INTEGER / DECIMAL / BOOLEAN / DATE
  is_mandatory   — true/false
  default_value  — fallback when partner sends blank
  transformation — TRIM / UPPERCASE / LOWERCASE
  date_format    — e.g. dd/MM/yyyy (only for DATE type)
  is_active      — false = row is ignored (soft-disable)
```

**Priority of resolution per field:**
1. Partner's raw `stringvalN` value
2. `default_value` from DB (if raw is blank)
3. Mandatory check — if still null, exception with ALL missing fields listed
4. Not mandatory + blank → field omitted (POJO field is `null`)

---

---

## API 1: ELIGIBILITY\_API

**Stage:** 1 (first — must pass before any other stage)
**DB key (target_api):** `ELIGIBILITY_API`
**POJO:** `EligibilityRequest.java`
**Orchestrator method:** `executeEligibilityStage()`
**Retry:** `@Retryable` on `EligibilityApiClient.call()` — 3 attempts, 2 s / 4 s backoff

| # | API Field | Source Param | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|--------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `firstName` | `stringval1` | STRING | Yes | TRIM | — | DB Mapping | Confirmed |
| 2 | `lastName` | `stringval2` | STRING | Yes | TRIM | — | DB Mapping | Confirmed |
| 3 | `middleName` | `stringval3` | STRING | No | TRIM | — | DB Mapping | Optional; partner may omit |
| 4 | `dateOfBirth` | `stringval4` | DATE | Yes | — | Format: `dd/MM/yyyy` | DB Mapping | Date format must match DB `date_format` column |
| 5 | `gender` | `stringval5` | STRING | No | UPPERCASE | **LOV:** `MALE`, `FEMALE` / **Default:** `MALE` | DB Mapping | Default applied when blank |
| 6 | `maritalStatus` | `stringval6` | STRING | No | UPPERCASE | **LOV:** `SINGLE`, `MARRIED`, `DIVORCED` | DB Mapping | TODO: Confirm full LOV with API contract |
| 7 | `nationality` | `stringval7` | STRING | No | UPPERCASE | **Default:** `INDIAN` | DB Mapping | Default applied when blank |
| 8 | `mobileNumber` | `stringval10` | STRING | Yes | — | — | DB Mapping | Confirmed |
| 9 | `emailAddress` | `stringval11` | STRING | No | LOWERCASE | — | DB Mapping | Optional |
| 10 | `panNumber` | `stringval45` | STRING | Yes | UPPERCASE | — | DB Mapping | Confirmed |
| 11 | `productCode` | `stringval50` | STRING | Yes | — | — | DB Mapping | Confirmed |
| 12 | `policyTerm` | `stringval51` | INTEGER | Yes | — | — | DB Mapping | Confirmed |
| 13 | `sumAssured` | `stringval52` | DECIMAL | Yes | — | — | DB Mapping | Confirmed |

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `eligibilityId` | MEDICAL_API | `req.setEligibilityId(...)` in orchestrator |
| `ageAtEntry` | MEDICAL_API, PREMIUM_CALC_API | `req.setAgeAtEntry(...)` in orchestrator |
| `status` | Logged | Journey log |

---

---

## API 2: EDC\_API (Credit Score)

**Stage:** 2 — runs in **PARALLEL** with PASA_API and TASA_API
**DB key (target_api):** `SCORING_API` (shared with PASA and TASA)
**POJO:** `ScoringRequest.java`
**Orchestrator method:** `executeScoringStage()` via `CompletableFuture.runAsync()`
**Retry:** Per-API retry on `ScoringApiClient.callEdc()`

| # | API Field | Source Param | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|--------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `panNumber` | `stringval45` | STRING | Yes | UPPERCASE | — | DB Mapping | Same as Eligibility |
| 2 | `firstName` | `stringval1` | STRING | No | TRIM | — | DB Mapping | Reused from Eligibility mapping |
| 3 | `lastName` | `stringval2` | STRING | No | TRIM | — | DB Mapping | Reused |
| 4 | `dateOfBirth` | `stringval4` | DATE | No | — | Format: `dd/MM/yyyy` | DB Mapping | Reused |
| 5 | `annualIncome` | `stringval60` | DECIMAL | No | — | — | DB Mapping | TODO: Confirm if mandatory for EDC |
| 6 | `existingLoans` | `stringval61` | DECIMAL | No | — | **Default:** `0` | DB Mapping | Default applied when blank |
| 7 | `employmentType` | `stringval62` | STRING | No | UPPERCASE | **LOV:** `SALARIED`, `SELF_EMPLOYED` | DB Mapping | TODO: Confirm full LOV |
| 8 | `sumAssured` | `stringval52` | DECIMAL | Yes | — | — | DB Mapping | Confirmed |
| 9 | `policyTerm` | `stringval51` | INTEGER | Yes | — | — | DB Mapping | Confirmed |

> **Note:** The same `ScoringRequest` object is sent to EDC, PASA, and TASA. If any of these APIs needs a different field, either add it here (others ignore it) or split into separate request classes.

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `creditScore` | MEDICAL_API, UNDERWRITING_API | `req.setCreditScore(...)` in orchestrator |
| `status` | Logged | Journey log |

---

---

## API 3: PASA\_API (Financial Score)

**Stage:** 2 — runs in **PARALLEL** with EDC_API and TASA_API
**DB key (target_api):** `SCORING_API` (shared request class — see EDC_API table above)
**POJO:** `ScoringRequest.java`
**Orchestrator method:** `executeScoringStage()` via `CompletableFuture.runAsync()`

> All input fields are identical to EDC_API (shared `ScoringRequest`). Refer to **API 2** table above.

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `pasaScore` | UNDERWRITING_API | `req.setPasaScore(...)` in orchestrator |
| `scoreGrade` | Logged | Journey log |

---

---

## API 4: TASA\_API (Risk Score)

**Stage:** 2 — runs in **PARALLEL** with EDC_API and PASA_API
**DB key (target_api):** `SCORING_API` (shared request class — see EDC_API table above)
**POJO:** `ScoringRequest.java`
**Orchestrator method:** `executeScoringStage()` via `CompletableFuture.runAsync()`

> All input fields are identical to EDC_API (shared `ScoringRequest`). Refer to **API 2** table above.

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `riskCategory` | UNDERWRITING_API | `req.setTasaRiskCategory(...)` in orchestrator |

---

---

## API 5: MEDICAL\_API

**Stage:** 3 (after all scoring APIs complete)
**DB key (target_api):** `MEDICAL_API`
**POJO:** `MedicalRequest.java`
**Orchestrator method:** `executeMedicalStage()`
**Retry:** `@Retryable` on `MedicalApiClient.call()`

| # | API Field | Source Param / Source | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|----------------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `firstName` | `stringval1` | STRING | Yes | TRIM | — | DB Mapping | Confirmed |
| 2 | `lastName` | `stringval2` | STRING | Yes | TRIM | — | DB Mapping | Confirmed |
| 3 | `dateOfBirth` | `stringval4` | DATE | Yes | — | Format: `dd/MM/yyyy` | DB Mapping | Confirmed |
| 4 | `panNumber` | `stringval45` | STRING | Yes | UPPERCASE | — | DB Mapping | Confirmed |
| 5 | `sumAssured` | `stringval52` | DECIMAL | Yes | — | — | DB Mapping | Confirmed |
| 6 | `policyTerm` | `stringval51` | INTEGER | Yes | — | — | DB Mapping | Confirmed |
| 7 | `smokingStatus` | `stringval70` | STRING | No | UPPERCASE | **Default:** `NON_SMOKER` | DB Mapping | Default applied when blank |
| 8 | `alcoholConsumption` | `stringval71` | STRING | No | UPPERCASE | **Default:** `NONE` | DB Mapping | Default applied when blank |
| 9 | `existingConditions` | `stringval72` | STRING | No | — | — | DB Mapping | Partner may not know; optional |
| 10 | `height` | `stringval73` | DECIMAL | No | — | — (cm) | DB Mapping | Optional |
| 11 | `weight` | `stringval74` | DECIMAL | No | — | — (kg) | DB Mapping | Optional |
| 12 | `eligibilityId` | `EligibilityResponse.eligibilityId` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 1 |
| 13 | `ageAtEntry` | `EligibilityResponse.ageAtEntry` | INTEGER | — | — | — | **ENRICHED** | Calculated by eligibility service |
| 14 | `creditScore` | `EdcResponse.creditScore` | INTEGER | — | — | — | **ENRICHED** | Set in orchestrator after EDC (Stage 2) |

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `riskCategory` | PREMIUM_CALC_API, UNDERWRITING_API | `req.setRiskCategory(...)` / `req.setMedicalRiskCategory(...)` |
| `loadingFactor` | PREMIUM_CALC_API, UNDERWRITING_API | `req.setLoadingFactor(...)` |
| `medicalScore` | UNDERWRITING_API | `req.setMedicalScore(...)` |
| `status` | PAS_API integrations block | If medicalResult != null, `MRS` integration added to PAS request |

---

---

## API 6: KYC\_API

**Stage:** 4
**DB key (target_api):** `KYC_API`
**POJO:** `KycRequest.java`
**Orchestrator method:** `executeKycStage()`
**Retry:** `@Retryable` on `KycApiClient.call()`

> **STATUS: INCOMPLETE — FIELDS ARE PLACEHOLDER / TODO**
> `KycRequest.java` contains stub fields only. The full KYC API contract has not been implemented yet.

| # | API Field | Source Param | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|--------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `panNumber` | `stringval45` (inferred) | STRING | Yes | UPPERCASE | — | DB Mapping | **TODO:** Confirm source param with KYC API contract |
| 2 | `firstName` | `stringval1` (inferred) | STRING | Yes | TRIM | — | DB Mapping | **TODO:** Confirm |
| 3 | `lastName` | `stringval2` (inferred) | STRING | Yes | TRIM | — | DB Mapping | **TODO:** Confirm |
| 4 | `dateOfBirth` | `stringval4` (inferred) | DATE | Yes | — | Format: `dd/MM/yyyy` | DB Mapping | **TODO:** Confirm |
| 5 | `aadhaarNumber` | `stringval117` (inferred) | STRING | No | — | — | DB Mapping | **TODO:** Confirm; also used in PAS for ID proof |
| 6 | `addressLine1` | **Unknown** | STRING | ? | — | — | DB Mapping | **TODO:** Identify source stringvalN |
| 7 | `city` | **Unknown** | STRING | ? | — | — | DB Mapping | **TODO:** Identify source stringvalN |
| 8 | `pincode` | **Unknown** | STRING | ? | — | — | DB Mapping | **TODO:** Identify source stringvalN |

> **BA Action Required:** Get KYC API contract and fill all fields. Add corresponding `partner_field_mapping` rows with `target_api = KYC_API`.

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `status` | Logged | Journey log — no downstream enrichment currently defined |

---

---

## API 7: PREMIUM\_CALC\_API

**Stage:** 5
**DB key (target_api):** `PREMIUM_CALC_API`
**POJO:** `PremiumRequest.java`
**Orchestrator method:** `executePremiumStage()`
**Retry:** `@Retryable` on `PremiumApiClient.call()`

> **STATUS: PARTIAL — TODO note in POJO for full contract fields**

| # | API Field | Source Param / Source | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|----------------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `sumAssured` | `stringval52` | DECIMAL | Yes | — | — | DB Mapping | Confirmed |
| 2 | `policyTerm` | `stringval51` | INTEGER | Yes | — | — | DB Mapping | Confirmed |
| 3 | `productCode` | `stringval50` | STRING | Yes | — | — | DB Mapping | Confirmed |
| 4 | `smokingStatus` | `stringval70` (inferred) | STRING | No | UPPERCASE | **Default:** `NON_SMOKER` | DB Mapping | **TODO:** Confirm if premium API needs this |
| 5 | `ageAtEntry` | `EligibilityResponse.ageAtEntry` | INTEGER | — | — | — | **ENRICHED** | Set in orchestrator after Stage 1 |
| 6 | `riskCategory` | `MedicalResponse.riskCategory` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 3 |
| 7 | `loadingFactor` | `MedicalResponse.loadingFactor` | DECIMAL | — | — | — | **ENRICHED** | Set in orchestrator after Stage 3 |

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `calculatedPremium` | PROPOSAL_SUBMIT_API, PAS_API | `req.setCalculatedPremium(...)` / `productDetails.setPremiumAmount(...)` |
| `frequency` | PROPOSAL_SUBMIT_API, PAS_API | `req.setFrequency(...)` / `productDetails.setPremFrequency(...)` |

---

---

## API 8: UNDERWRITING\_API

**Stage:** 6
**DB key (target_api):** `UNDERWRITING_API`
**POJO:** `UnderwritingRequest.java`
**Orchestrator method:** `executeUnderwritingStage()`
**Retry:** `@Retryable` on `UnderwritingApiClient.call()`

> **CRITICAL:** If underwriting `decision = DECLINED`, the journey stops immediately. No document or proposal stage runs.

> **STATUS: PARTIAL — TODO note in POJO for full contract fields**

| # | API Field | Source Param / Source | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|----------------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `sumAssured` | `stringval52` | DECIMAL | Yes | — | — | DB Mapping | Confirmed |
| 2 | `policyTerm` | `stringval51` | INTEGER | Yes | — | — | DB Mapping | Confirmed |
| 3 | `productCode` | `stringval50` | STRING | Yes | — | — | DB Mapping | Confirmed |
| 4 | `creditScore` | `EdcResponse.creditScore` | INTEGER | — | — | — | **ENRICHED** | Set in orchestrator after Stage 2 (EDC) |
| 5 | `pasaScore` | `PasaResponse.pasaScore` | INTEGER | — | — | — | **ENRICHED** | Set in orchestrator after Stage 2 (PASA) |
| 6 | `tasaRiskCategory` | `TasaResponse.riskCategory` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 2 (TASA) |
| 7 | `medicalScore` | `MedicalResponse.medicalScore` | INTEGER | — | — | — | **ENRICHED** | Set in orchestrator after Stage 3 |
| 8 | `medicalRiskCategory` | `MedicalResponse.riskCategory` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 3 |
| 9 | `loadingFactor` | `MedicalResponse.loadingFactor` | DECIMAL | — | — | — | **ENRICHED** | Set in orchestrator after Stage 3 |

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `decision` | Journey control | If `DECLINED` → `JourneyStageException` thrown, journey stops |
| `decisionCode` | Journey exception | Included in exception message |
| `decision` | PROPOSAL_SUBMIT_API | `req.setUnderwritingDecision(...)` |
| `decision` | PAS_API | If `underwritingResult != null` → AWS integration added to PAS request |

---

---

## API 9: DOCUMENT\_API

**Stage:** 7
**DB key (target_api):** `DOCUMENT_API`
**POJO:** `DocumentRequest.java`
**Orchestrator method:** `executeDocumentStage()`
**Retry:** `@Retryable` on `DocumentApiClient.call()`

> **STATUS: INCOMPLETE — Most fields are TODO. Only correlationId is explicitly set.**

| # | API Field | Source Param / Source | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|----------------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `correlationId` | `context.getCorrelationId()` | STRING | Yes | — | — | **HARDCODED (runtime)** | Set from journey context, not partner params |
| 2 | `productCode` | `stringval50` (inferred) | STRING | ? | — | — | DB Mapping | **TODO:** Confirm with DOCUMENT_API contract |
| 3 | `applicantName` | Derived from name fields (inferred) | STRING | ? | — | — | DB Mapping | **TODO:** Confirm source param and format |

> **BA Action Required:** Get full DOCUMENT_API contract. Add all fields to `DocumentRequest.java` and add `partner_field_mapping` rows with `target_api = DOCUMENT_API`.

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `documentId` | PROPOSAL_SUBMIT_API | `req.setDocumentId(...)` |

---

---

## API 10: PROPOSAL\_SUBMIT\_API

**Stage:** 8 (final stage before PAS)
**DB key (target_api):** `PROPOSAL_SUBMIT_API`
**POJO:** `ProposalRequest.java`
**Orchestrator method:** `executeProposalStage()`
**Retry:** `@Retryable` on `ProposalApiClient.call()`

> **STATUS: PARTIAL — TODO note in POJO for full contract fields**

| # | API Field | Source Param / Source | Data Type | Mandatory | Transformation | LOV / Default | Source Type | BA Comment / Mapping Status |
|---|-----------|----------------------|-----------|-----------|----------------|---------------|-------------|------------------------------|
| 1 | `productCode` | `stringval50` | STRING | Yes | — | — | DB Mapping | Confirmed |
| 2 | `sumAssured` | `stringval52` | DECIMAL | Yes | — | — | DB Mapping | Confirmed |
| 3 | `policyTerm` | `stringval51` | INTEGER | Yes | — | — | DB Mapping | Confirmed |
| 4 | `applicantName` | **Unknown** | STRING | ? | — | — | DB Mapping | **TODO:** Confirm source — derived from stringval1+stringval2? |
| 5 | `calculatedPremium` | `PremiumResponse.calculatedPremium` | DECIMAL | — | — | — | **ENRICHED** | Set in orchestrator after Stage 5 |
| 6 | `frequency` | `PremiumResponse.frequency` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 5 |
| 7 | `underwritingDecision` | `UnderwritingResponse.decision` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 6 |
| 8 | `documentId` | `DocumentResponse.documentId` | STRING | — | — | — | **ENRICHED** | Set in orchestrator after Stage 7 |

**Response fields used by downstream stages:**

| Response Field | Used By | How Used |
|----------------|---------|----------|
| `proposalNumber` | PAS_API | `body.setProposalNumber(...)` |

---

---

## API 11: PAS\_API (Policy Administration System)

**Stage:** Post-Journey (runs after all 8 stages complete successfully)
**Handler:** `PasApiClient.java` — `submitAndGetApplicationNumber()`
**Retry:** `@Retryable` — 3 attempts, 3 s / 6 s backoff
**Note:** This API is handled separately from the main journey because the `applicationNumber` it returns must be stored in the DB and passed to the reverse feed.

---

### 11a. PAS Header

| # | PAS Field Path | Source | Hardcoded/Default Value | BA Comment / Mapping Status |
|---|----------------|--------|-------------------------|-----------------------------|
| 1 | `header.correlationId` | `context.getCorrelationId()` | — | Auto-set from journey context |
| 2 | `header.processVars` | — | `new ProcessVars()` (empty object) | **HARDCODED** — empty default |

---

### 11b. PAS Request Body — Top Level

| # | PAS Field Path | Source | Hardcoded/Default Value | BA Comment / Mapping Status |
|---|----------------|--------|-------------------------|-----------------------------|
| 1 | `request.proposalNumber` | `ProposalResponse.proposalNumber` | — | **ENRICHED** from Stage 8 |
| 2 | `request.status` | — | `"DRAFT"` | **HARDCODED** — always DRAFT at this point |

---

### 11c. basicPolicyInsured (IP identity block for PAS)

> **Fix 2:** ID proof logic — prefer Aadhar (`stringval117`) if present; fall back to PAN (`stringval122`).
> **Fix 3:** Gender — map `M` → `MALE`, `F` → `FEMALE`.

| # | PAS Field Path | Source Param | Hardcoded/Default | Transformation / LOV | Source Type | BA Comment / Mapping Status |
|---|----------------|--------------|-------------------|----------------------|-------------|------------------------------|
| 1 | `basicPolicyInsured[0].salutation` | `stringval12` | — | — | Direct | Confirmed |
| 2 | `basicPolicyInsured[0].policyInsuredFirstName` | `stringval13` | — | — | Direct | Confirmed |
| 3 | `basicPolicyInsured[0].policyInsuredMiddleName` | `stringval14` | — | — | Direct | Optional |
| 4 | `basicPolicyInsured[0].policyInsuredLastName` | `stringval15` | — | — | Direct | Confirmed |
| 5 | `basicPolicyInsured[0].policyInsuredDateOfBirth` | `stringval16` | — | — | Direct | Confirmed — passed as string |
| 6 | `basicPolicyInsured[0].gender` | `stringval18` | — | **M→MALE, F→FEMALE** (Fix 3) | Direct + Transform | LOV: `MALE`, `FEMALE` |
| 7 | `basicPolicyInsured[0].policyInsuredLegalIdentifierCode` | `stringval117` (Aadhar) | `AADHAR_REFERENCE_CODE` or `PAN` | If stringval117 non-blank → Aadhar (Fix 2) | Conditional | BA to confirm fallback logic is correct |
| 8 | `basicPolicyInsured[0].policyInsuredLegalIdentifierValue` | `stringval117` or `stringval122` | — | Aadhar preferred; PAN fallback (Fix 2) | Conditional | Confirmed |

---

### 11d. productSelection

| # | PAS Field Path | Source Param | Hardcoded/Default | BA Comment / Mapping Status |
|---|----------------|--------------|-------------------|-----------------------------|
| 1 | `policyCheckIn.productSelection.baseCoverageCode` | `stringval31` | — | Confirmed |
| 2 | `policyCheckIn.productSelection.branchCode` | **Unknown** | — | **TODO:** Identify source param |
| 3 | `policyCheckIn.productSelection.policyIssueState` | **Unknown** | — | **TODO:** Identify source param |
| 4 | `policyCheckIn.productSelection.productPlan` | **Unknown** | — | **TODO:** Identify source param |

---

### 11e. bankDetailsDTO

| # | PAS Field Path | Source | BA Comment / Mapping Status |
|---|----------------|--------|-----------------------------|
| 1 | `policyCheckIn.bankDetailsDTO` | — | **HARDCODED** — `new BankDetailsDTO()` (empty object). **TODO:** Confirm if bank details need to be populated for final submission |

---

### 11f. integrations

| # | Integration Name | Condition | Set When | BA Comment / Mapping Status |
|---|-----------------|-----------|----------|-----------------------------|
| 1 | `AWS` | `context.getUnderwritingResult() != null` | Underwriting API completed | **HARDCODED** — name and status (`true`) are fixed |
| 2 | `MRS` | `context.getMedicalResult() != null` | Medical API completed | **HARDCODED** — name and status (`true`) are fixed |

---

### 11g. journeyDetails

| # | PAS Field Path | Source | BA Comment / Mapping Status |
|---|----------------|--------|-----------------------------|
| 1 | `policyCheckIn.journeyDetails` | — | **HARDCODED** — `new JourneyDetails()` (empty object). **TODO:** Confirm fields required |

---

### 11h. habbitDetailsDTO (Lifestyle flags)

> **Fix 5:** All lifestyle boolean flags must be `false` (not `null`) for PAS to accept the request.

| # | PAS Field Path | Hardcoded Value | BA Comment / Mapping Status |
|---|----------------|-----------------|-----------------------------|
| 1 | `policyCheckIn.habbitDetailsDTO.isAlcohol` | `false` | **HARDCODED default** — TODO: Map from partner if needed (e.g. stringval71) |
| 2 | `policyCheckIn.habbitDetailsDTO.isChangeInWeight` | `false` | **HARDCODED default** — TODO: Map from partner if needed |
| 3 | `policyCheckIn.habbitDetailsDTO.isDGH` | `false` | **HARDCODED default** |
| 4 | `policyCheckIn.habbitDetailsDTO.isPEP` | `false` | **HARDCODED default** |
| 5 | `policyCheckIn.habbitDetailsDTO.isSmoker` | `false` | **HARDCODED default** — TODO: Map from stringval70 (smokingStatus) |
| 6 | `policyCheckIn.habbitDetailsDTO.isTobacco` | `false` | **HARDCODED default** |
| 7 | `policyCheckIn.habbitDetailsDTO.height` | `null` | **TODO:** Map from stringval73 if PAS needs it |
| 8 | `policyCheckIn.habbitDetailsDTO.weight` | `null` | **TODO:** Map from stringval74 if PAS needs it |
| 9 | `policyCheckIn.habbitDetailsDTO.bmi` | `null` | **TODO:** Map or calculate from height/weight |

---

### 11i. ipDetails — IP (Insured Person) Personal Details

> **Fix 1:** `emailId` must be an `EmailAddress` object — not a bare String.
> **Fix 2:** ID proof — prefer Aadhar; fall back to PAN.
> **Fix 3:** Gender — `M`→`MALE`, `F`→`FEMALE`.
> **Fix 4:** Marital status — `M`→`MARRIED`, `S`→`SINGLE`, `D`→`DIVORCED`, `W`→`WIDOWED`.

| # | PAS Field Path | Source Param | Hardcoded/Default | Transformation / LOV | Source Type | BA Comment / Mapping Status |
|---|----------------|--------------|-------------------|----------------------|-------------|------------------------------|
| 1 | `ipDetails.ippersonalDetails.ipBasicDetails.salutation` | `stringval12` | — | — | Direct | Confirmed |
| 2 | `ipDetails.ippersonalDetails.ipBasicDetails.firstName` | `stringval13` | — | — | Direct | Confirmed |
| 3 | `ipDetails.ippersonalDetails.ipBasicDetails.middleName` | `stringval14` | — | — | Direct | Optional |
| 4 | `ipDetails.ippersonalDetails.ipBasicDetails.lastName` | `stringval15` | — | — | Direct | Confirmed |
| 5 | `ipDetails.ippersonalDetails.ipBasicDetails.dateOfBirth` | `stringval16` | — | — | Direct | Confirmed |
| 6 | `ipDetails.ippersonalDetails.ipBasicDetails.gender` | `stringval18` | — | **M→MALE, F→FEMALE** (Fix 3) | Direct + Transform | LOV: `MALE`, `FEMALE` |
| 7 | `ipDetails.ippersonalDetails.ipBasicDetails.maritalStatus` | `stringval99` | — | **M→MARRIED, S→SINGLE, D→DIVORCED, W→WIDOWED** (Fix 4) | Direct + Transform | LOV: `MARRIED`, `SINGLE`, `DIVORCED`, `WIDOWED` |
| 8 | `ipDetails.ippersonalDetails.ipBasicDetails.idProofDoc` | `stringval117` (Aadhar) | `AADHAR_REFERENCE_CODE` or `PAN` | If Aadhar non-blank → Aadhar; else PAN (Fix 2) | Conditional | Confirmed |
| 9 | `ipDetails.ippersonalDetails.ipBasicDetails.idProofValue` | `stringval117` or `stringval122` | — | — | Conditional | Confirmed |
| 10 | `ipDetails.ippersonalDetails.contactDetails.emailId` | `stringval20` | — | Wrapped in `EmailAddress` object (Fix 1) | Direct | Fix 1: bare String causes PAS rejection |
| 11 | `ipDetails.ippersonalDetails.contactDetails.mobileNumber` | `stringval19` | — | Wrapped in `PhoneNumber` object | Direct | Confirmed |
| 12 | `ipDetails.kycType` | **Unknown** | `null` | — | — | **TODO:** Confirm if required by PAS |
| 13 | `ipDetails.currentAddress` | **Unknown** | `null` | — | — | **TODO:** Identify source params for address |
| 14 | `ipDetails.permanentAddress` | **Unknown** | `null` | — | — | **TODO:** Identify source params for address |
| 15 | `ipDetails.ipEducationAndOccupationDetails` | **Unknown** | `null` | — | — | **TODO:** Required for IP? |

---

### 11j. phDetails — PH (Policyholder) Details

> **Fix 1:** email wrapped in `EmailAddress` object.
> **Fix 7:** `annualIncome` must be set in `occupationDetails`.
> **Fix 8:** Gender as full string; ID proof with Aadhar-or-PAN; `relationshipToIP` as empty string (not null).

| # | PAS Field Path | Source Param | Hardcoded/Default | Transformation / LOV | Source Type | BA Comment / Mapping Status |
|---|----------------|--------------|-------------------|----------------------|-------------|------------------------------|
| 1 | `phDetails.phPersonalDetails.basicPersonDetails.firstName` | `stringval46` | — | — | Direct | Confirmed — separate from IP name |
| 2 | `phDetails.phPersonalDetails.basicPersonDetails.dateOfBirth` | `stringval48` | — | — | Direct | Confirmed |
| 3 | `phDetails.phPersonalDetails.basicPersonDetails.gender` | `stringval49` | — | **M→MALE, F→FEMALE** (Fix 8) | Direct + Transform | LOV: `MALE`, `FEMALE` |
| 4 | `phDetails.phPersonalDetails.basicPersonDetails.idProofDoc` | `stringval117` (Aadhar) | `AADHAR_REFERENCE_CODE` or `PAN` | Aadhar preferred (Fix 8) | Conditional | Same Aadhar/PAN logic as IP |
| 5 | `phDetails.phPersonalDetails.basicPersonDetails.idProofValue` | `stringval117` or `stringval122` | — | — | Conditional | Confirmed |
| 6 | `phDetails.phPersonalDetails.basicPersonDetails.lastName` | **Unknown** | `null` | — | — | **TODO:** stringval47 is relationshipToIP — confirm PH last name param |
| 7 | `phDetails.phPersonalDetails.contactDetails.emailId` | `stringval20` | — | Wrapped in `EmailAddress` object (Fix 1) | Direct | Same email as IP — confirm if PH can have different email |
| 8 | `phDetails.phPersonalDetails.contactDetails.mobileNumber` | `stringval19` | — | Wrapped in `PhoneNumber` object | Direct | Same mobile as IP — confirm |
| 9 | `phDetails.phEducationAndOccupationDetails.occupationDetails.annualIncome` | `stringval130` | — | — (Fix 7) | Direct | Fix 7: must be set; PAS rejects if null |
| 10 | `phDetails.phEducationAndOccupationDetails.occupationDetails.occupation` | `stringval133` | — | — | Direct | Confirmed |
| 11 | `phDetails.relationshipToIP` | `stringval47` | `""` (empty string, not null) | — (Fix 8) | Direct | Fix 8: null causes PAS rejection; default is empty string |
| 12 | `phDetails.kycType` | **Unknown** | `null` | — | — | **TODO:** Confirm if required |
| 13 | `phDetails.currentAddress` | **Unknown** | `null` | — | — | **TODO:** Identify address params |
| 14 | `phDetails.permanentAddress` | **Unknown** | `null` | — | — | **TODO:** Identify address params |

---

### 11k. payerDetails — Payer Details

> **Fix 1:** email wrapped in `EmailAddress` object.
> **Fix 6:** Gender must be the full string `MALE`/`FEMALE`.

| # | PAS Field Path | Source Param | Hardcoded/Default | Transformation / LOV | Source Type | BA Comment / Mapping Status |
|---|----------------|--------------|-------------------|----------------------|-------------|------------------------------|
| 1 | `payerDetails.payerPersonalDetails.basicPersonDetails.gender` | `stringval49` | — | **M→MALE, F→FEMALE** (Fix 6) | Direct + Transform | LOV: `MALE`, `FEMALE` — using PH gender for payer |
| 2 | `payerDetails.payerPersonalDetails.contactDetails.emailId` | `stringval20` | — | Wrapped in `EmailAddress` object (Fix 1) | Direct | Same email as IP/PH — confirm if payer can differ |
| 3 | `payerDetails.payerPersonalDetails.contactDetails.mobileNumber` | `stringval19` | — | Wrapped in `PhoneNumber` object | Direct | Confirmed |
| 4 | `payerDetails.payerPersonalDetails.basicPersonDetails.firstName` | **Unknown** | `null` | — | — | **TODO:** Is payer always same as PH? Confirm with BA |
| 5 | `payerDetails.payerPersonalDetails.basicPersonDetails.dateOfBirth` | **Unknown** | `null` | — | — | **TODO:** Confirm payer DOB source param |

---

### 11l. productDetailsDTO

> **Fix 9:** `ppt` must be `0` (not null) and `riderDetails` must be an empty list (not null or list with null entries).

| # | PAS Field Path | Source / Value | Source Type | BA Comment / Mapping Status |
|---|----------------|----------------|-------------|------------------------------|
| 1 | `productDetailsDTO.premiumAmount` | `PremiumResponse.calculatedPremium` (as String) | **ENRICHED** | From Stage 5 result |
| 2 | `productDetailsDTO.premFrequency` | `PremiumResponse.frequency` | **ENRICHED** | From Stage 5 result |
| 3 | `productDetailsDTO.ppt` | `0` | **HARDCODED** (Fix 9) | Always 0; PAS rejects null |
| 4 | `productDetailsDTO.riderDetails` | `[]` (empty list) | **HARDCODED** (Fix 9) | Always empty list; PAS rejects null |
| 5 | `productDetailsDTO.productName` | **Unknown** | — | **TODO:** Source param not yet mapped |
| 6 | `productDetailsDTO.optionOrVariant` | **Unknown** | — | **TODO:** Source param not yet mapped |
| 7 | `productDetailsDTO.coverage` | **Unknown** | — | **TODO:** Source param not yet mapped |
| 8 | `productDetailsDTO.fundDetails` | `null` | — | **TODO:** Required for ULIP products? |

---

---

## API 12: REVERSE\_FEED\_API

**Stage:** Post-PAS (after `applicationNumber` is received and stored)
**Handler:** `DefaultPartnerNotifier.java` — `notify()`
**Retry:** `@Retryable` — 3 attempts, 5 s / 10 s backoff
**Auth:** Configured per partner in `partner_config` table (`auth_type`: `BEARER` or `API_KEY`)
**URL:** Configured per partner in `partner_config` table (`reverse_feed_url`)

| # | Payload Field | Source | Source Type | BA Comment / Mapping Status |
|---|---------------|--------|-------------|------------------------------|
| 1 | `correlationId` | `context.getCorrelationId()` | **HARDCODED (runtime)** | Unique journey ID |
| 2 | `applicationNumber` | PAS response `applicationNumber` | **ENRICHED** (from PAS) | Returned by PAS after successful submission |
| 3 | *(other fields)* | **Unknown** | — | **TODO:** Define full reverse feed payload contract with partner |

> **Auth configuration (in `partner_config` table):**
> - `auth_type = BEARER` → sends `Authorization: Bearer <credential>` header
> - `auth_type = API_KEY` → sends `X-Api-Key: <credential>` header
> - BASIC auth: **TODO** — not yet implemented in `buildAuthHeaders()`

---

---

## Summary: All stringvalN Param Assignments

| stringvalN | Assigned To | API(s) Using It | Notes |
|------------|------------|------------------|-------|
| `stringval1` | `firstName` | ELIGIBILITY, SCORING (EDC/PASA/TASA), MEDICAL | Common name field |
| `stringval2` | `lastName` | ELIGIBILITY, SCORING, MEDICAL | Common name field |
| `stringval3` | `middleName` | ELIGIBILITY | Optional |
| `stringval4` | `dateOfBirth` | ELIGIBILITY, SCORING, MEDICAL | Date format: `dd/MM/yyyy` |
| `stringval5` | `gender` | ELIGIBILITY | UPPERCASE, default MALE |
| `stringval6` | `maritalStatus` | ELIGIBILITY | UPPERCASE |
| `stringval7` | `nationality` | ELIGIBILITY | UPPERCASE, default INDIAN |
| `stringval10` | `mobileNumber` | ELIGIBILITY | Mandatory |
| `stringval11` | `emailAddress` | ELIGIBILITY | LOWERCASE |
| `stringval12` | `salutation` | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval13` | IP `firstName` | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval14` | IP `middleName` | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval15` | IP `lastName` | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval16` | IP `dateOfBirth` | PAS (basicPolicyInsured, ipBasicDetails) | Passed as String |
| `stringval18` | IP `gender` | PAS (basicPolicyInsured, ipBasicDetails) | M→MALE, F→FEMALE |
| `stringval19` | `mobileNumber` | PAS (IP contact, PH contact, Payer contact) | Wrapped in PhoneNumber object |
| `stringval20` | `emailAddress` | PAS (IP contact, PH contact, Payer contact) | Wrapped in EmailAddress object (Fix 1) |
| `stringval31` | `baseCoverageCode` | PAS (productSelection) | — |
| `stringval45` | `panNumber` | ELIGIBILITY, SCORING, MEDICAL | UPPERCASE, mandatory |
| `stringval46` | PH `firstName` | PAS (phBasicDetails) | — |
| `stringval47` | PH `relationshipToIP` | PAS (phDetails) | Default: `""` (empty string) if null |
| `stringval48` | PH `dateOfBirth` | PAS (phBasicDetails) | — |
| `stringval49` | PH/Payer `gender` | PAS (phBasicDetails, payerBasicDetails) | M→MALE, F→FEMALE |
| `stringval50` | `productCode` | ELIGIBILITY, PREMIUM_CALC, UNDERWRITING, PROPOSAL | — |
| `stringval51` | `policyTerm` | ELIGIBILITY, SCORING, MEDICAL, PREMIUM_CALC, UNDERWRITING, PROPOSAL | INTEGER |
| `stringval52` | `sumAssured` | ELIGIBILITY, SCORING, MEDICAL, PREMIUM_CALC, UNDERWRITING, PROPOSAL | DECIMAL |
| `stringval60` | `annualIncome` | SCORING | DECIMAL |
| `stringval61` | `existingLoans` | SCORING | DECIMAL, default `0` |
| `stringval62` | `employmentType` | SCORING | UPPERCASE, LOV: SALARIED/SELF_EMPLOYED |
| `stringval70` | `smokingStatus` | MEDICAL, PREMIUM_CALC (inferred) | UPPERCASE, default NON_SMOKER |
| `stringval71` | `alcoholConsumption` | MEDICAL | UPPERCASE, default NONE |
| `stringval72` | `existingConditions` | MEDICAL | Optional |
| `stringval73` | `height` | MEDICAL | DECIMAL (cm) |
| `stringval74` | `weight` | MEDICAL | DECIMAL (kg) |
| `stringval99` | IP `maritalStatus` | PAS (ipBasicDetails) | M→MARRIED, S→SINGLE, D→DIVORCED, W→WIDOWED |
| `stringval117` | `aadhaarNumber` / `idProofValue` (Aadhar) | KYC (inferred), PAS (IP, PH, basicPolicyInsured) | Preferred ID proof; fallback to PAN if blank |
| `stringval122` | `panNumber` / `idProofValue` (PAN) | PAS (IP, PH, basicPolicyInsured) fallback | Fallback when stringval117 is blank |
| `stringval130` | PH `annualIncome` | PAS (phOccupationDetails) | Fix 7: must not be null |
| `stringval133` | PH `occupation` | PAS (phOccupationDetails) | — |

> **Note:** stringvalN params for KYC, DOCUMENT, and some PROPOSAL fields are not yet mapped in code. These are marked **TODO** in respective API tables above.

---

---

## Known Fixes Applied in Code (PasApiClient.java)

| Fix # | Issue | Resolution | Affected Fields |
|-------|-------|------------|-----------------|
| Fix 1 | Email passed as bare String — PAS rejected the request | Wrapped in `EmailAddress` object before setting on `ContactDetails` | `contactDetails.emailId` in IP, PH, and Payer |
| Fix 2 | ID proof logic not implemented | If `stringval117` (Aadhar) non-blank → use `AADHAR_REFERENCE_CODE`; else fall back to `stringval122` (PAN) | `policyInsuredLegalIdentifierCode/Value`, `idProofDoc/Value` |
| Fix 3 | PAS requires full gender string, not single character | Mapped `M` → `MALE`, `F` → `FEMALE` | IP gender in basicPolicyInsured and ipBasicDetails |
| Fix 4 | PAS requires full marital status string | Mapped `M`→`MARRIED`, `S`→`SINGLE`, `D`→`DIVORCED`, `W`→`WIDOWED` | `ipBasicDetails.maritalStatus` |
| Fix 5 | Lifestyle flags were `null` — PAS rejected | Defaulted all to `false` instead of null | All 6 boolean flags in `habbitDetailsDTO` |
| Fix 6 | Payer gender was not being mapped from `"M"`/`"F"` | Applied `mapGender()` to `stringval49` for payer | `payerBasicDetails.gender` |
| Fix 7 | `annualIncome` in PH occupation was null — PAS rejected | Explicitly set from `stringval130` | `phEducationAndOccupationDetails.occupationDetails.annualIncome` |
| Fix 8 | PH gender not mapped; `relationshipToIP` was null (PAS rejected); PH ID proof missing | Gender mapped; `relationshipToIP` defaults to `""` (empty string); Aadhar-or-PAN logic applied | `phBasicDetails.gender`, `phDetails.relationshipToIP`, `phBasicDetails.idProofDoc/Value` |
| Fix 9 | `ppt` was null; `riderDetails` was null — PAS rejected both | `ppt` hardcoded to `0`; `riderDetails` set to `new ArrayList<>()` | `productDetailsDTO.ppt`, `productDetailsDTO.riderDetails` |

---

## Open Items / Pending Actions

| # | API | Open Item | Owner | Priority |
|---|-----|-----------|-------|---------|
| 1 | KYC_API | Full KYC API contract not implemented — `KycRequest.java` has only placeholder fields | BA + Dev | High |
| 2 | DOCUMENT_API | Full DOCUMENT_API contract not implemented — only `correlationId`, `productCode`, `applicantName` stubbed | BA + Dev | High |
| 3 | PROPOSAL_SUBMIT_API | `applicantName` source param not confirmed — is it stringval1 + stringval2, or a separate field? | BA | Medium |
| 4 | PAS productSelection | `branchCode`, `policyIssueState`, `productPlan` source params not mapped | BA + Dev | High |
| 5 | PAS bankDetailsDTO | Empty object sent — does PAS require bank details for submission? | BA | High |
| 6 | PAS journeyDetails | Empty object sent — does PAS require journey details? | BA | Medium |
| 7 | PAS habbitDetailsDTO | `isSmoker`, `isAlcohol`, `isTobacco` are hardcoded to `false` — should these come from stringval70/71? | BA | Medium |
| 8 | PAS habbitDetailsDTO | `height`, `weight`, `bmi` are null — does PAS require them? (available as stringval73, stringval74) | BA | Medium |
| 9 | PAS ipDetails | `currentAddress`, `permanentAddress`, `kycType`, `ipEducationAndOccupationDetails` all null | BA + Dev | High |
| 10 | PAS phDetails | PH `lastName` source param not identified (`stringval47` is `relationshipToIP`) | BA | High |
| 11 | PAS phDetails | `currentAddress`, `permanentAddress`, `kycType` all null | BA + Dev | High |
| 12 | PAS payerDetails | Payer firstName, DOB not mapped — is payer always the same person as PH? | BA | High |
| 13 | PAS productDetailsDTO | `productName`, `optionOrVariant`, `coverage`, `fundDetails` not mapped | BA + Dev | Medium |
| 14 | REVERSE_FEED_API | Full reverse feed payload contract not defined — only `correlationId` and `applicationNumber` sent | BA + Dev | High |
| 15 | REVERSE_FEED_API | BASIC auth not implemented in `buildAuthHeaders()` | Dev | Low |
| 16 | ELIGIBILITY_API | On retry, `eligibilityResult` is not reloaded from DB — later stages that need it will fail on retry | Dev | High |
| 17 | SCORING_API | TODO in code: enrich `ScoringRequest` from `eligibilityResult` if EDC/PASA/TASA need it | BA + Dev | Medium |
| 18 | KYC_API | TODO in code: enrich `KycRequest` from prior stages (e.g. `eligibilityId`) | Dev | Medium |
