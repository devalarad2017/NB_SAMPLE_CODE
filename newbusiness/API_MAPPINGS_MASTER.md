# API Mappings Master Document — New Business Journey
**God Document for BA & Developer: field-level mapping, inter-API data flow, LOV/defaults, pending items**
**Source files:** `InboundRequest.java` · `JourneyOrchestrator.java` · `PasApiClient.java` · all `*Request/*.Response` POJOs
**Updated:** 2026-06-02

---

## Legend

| Symbol | Meaning |
|--------|---------|
| `stringvalN` | Key in `InboundRequest.params` map sent by partner |
| **ENRICHED** | Field is NOT from partner input — set from a prior API's response output |
| **HARDCODED** | Fixed value in code, not configurable |
| **TODO** | Mapping not yet implemented; needs BA/Dev action |
| LOV | List of Values — only these values accepted |
| Default | Value applied in code when partner sends blank/null |
| Fix N | Known production fix documented in `PasApiClient.java` |

---

## 1. Journey Execution Order & High-Level Flow

```
InboundRequest
  └─ partnerCode
  └─ params { stringval1..N }
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  Stage 1 ─ ELIGIBILITY_API                                      │
│    Input  : stringval1,2,3,4,5,6,7,10,11,45,50,51,52           │
│    Output : eligibilityId, ageAtEntry  ──────────────────────┐  │
└─────────────────────────────────────────────────────────────────┘
        │                                                         │
        ▼                                                         │
┌─────────────────────────────────────────────────────────────────┐
│  Stage 2 ─ SCORING (EDC + PASA + TASA run in PARALLEL)          │
│    Input  : stringval1,2,4,45,51,52,60,61,62                   │
│    EDC  Output  : creditScore  ─────────────────────────────┐  │
│    PASA Output  : pasaScore    ─────────────────────────────┤  │
│    TASA Output  : riskCategory ─────────────────────────────┤  │
└─────────────────────────────────────────────────────────────────┘
        │                                                    │    │
        ▼                                                    │    │
┌─────────────────────────────────────────────────────────────────┐
│  Stage 3 ─ MEDICAL_API                                          │
│    Input  : stringval1,2,4,45,51,52,70,71,72,73,74             │
│           + ENRICHED: eligibilityId, ageAtEntry ◄───────────┘  │
│           + ENRICHED: creditScore (from EDC)    ◄────────────┘ │
│    Output : medicalScore, riskCategory, loadingFactor ──────┐  │
└─────────────────────────────────────────────────────────────────┘
        │                                                     │
        ▼                                                     │
┌─────────────────────────────────────────────────────────────────┐
│  Stage 4 ─ KYC_API                  *** SKELETON — TODO ***     │
│    Input  : stringval45,1,2,4,117 + (address params TBD)        │
│    Output : status (logged only — no downstream enrichment yet) │
└─────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────┐
│  Stage 5 ─ PREMIUM_CALC_API         *** SKELETON — TODO ***     │
│    Input  : stringval50,51,52,70                               │
│           + ENRICHED: ageAtEntry (from Eligibility) ◄──────────┘│
│           + ENRICHED: riskCategory, loadingFactor (from Medical)◄┘
│    Output : calculatedPremium, frequency  ──────────────────┐  │
└─────────────────────────────────────────────────────────────────┘
        │                                                     │
        ▼                                                     │
┌─────────────────────────────────────────────────────────────────┐
│  Stage 6 ─ UNDERWRITING_API         *** SKELETON — TODO ***     │
│    Input  : stringval50,51,52                                   │
│           + ENRICHED: creditScore (EDC), pasaScore (PASA),      │
│             tasaRiskCategory (TASA), medicalScore,              │
│             medicalRiskCategory, loadingFactor (Medical) ◄──────┘
│    Output : decision  ──────────────────────────────────────┐  │
│  !! If decision = DECLINED → Journey STOPS here !!              │
└─────────────────────────────────────────────────────────────────┘
        │                                                     │
        ▼                                                     │
┌─────────────────────────────────────────────────────────────────┐
│  Stage 7 ─ DOCUMENT_API             *** SKELETON — TODO ***     │
│    Input  : correlationId (runtime), productCode, applicantName │
│    Output : documentId  ────────────────────────────────────┐  │
└─────────────────────────────────────────────────────────────────┘
        │                                                     │
        ▼                                                     │
┌─────────────────────────────────────────────────────────────────┐
│  Stage 8 ─ PROPOSAL_SUBMIT_API      *** SKELETON — TODO ***     │
│    Input  : stringval50,51,52                                   │
│           + ENRICHED: calculatedPremium, frequency ◄────────────┘
│           + ENRICHED: underwritingDecision ◄────────────────────┘
│           + ENRICHED: documentId ◄──────────────────────────────┘
│    Output : proposalNumber  ────────────────────────────────┐  │
└─────────────────────────────────────────────────────────────────┘
        │                                                     │
        ▼                                                     │
┌─────────────────────────────────────────────────────────────────┐
│  PAS_API — PasApiClient.buildPasRequest()    *** DETAILED ***   │
│    Input  : stringval12-16,18,19,20,31,46,47,48,49,            │
│             99,117,122,130,133                                  │
│           + ENRICHED: proposalNumber ◄──────────────────────────┘
│           + ENRICHED: calculatedPremium, frequency (Premium)    │
│           + HARDCODED: status=DRAFT, ppt=0, all habit flags=false│
│    Output : applicationNumber  (stored in DB)                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Inter-API Output → Input Data Flow

This table shows every response field that becomes an input to a later API.
All enrichment is done explicitly in `JourneyOrchestrator.java` after `resolveAs()` returns.

| From API | Response Field | Java Getter | To API | Request Field | Set In Code |
|----------|---------------|-------------|--------|---------------|-------------|
| ELIGIBILITY_API | `eligibilityId` | `getEligibilityId()` | MEDICAL_API | `eligibilityId` | `req.setEligibilityId(context.getEligibilityResult().getEligibilityId())` |
| ELIGIBILITY_API | `ageAtEntry` | `getAgeAtEntry()` | MEDICAL_API | `ageAtEntry` | `req.setAgeAtEntry(context.getEligibilityResult().getAgeAtEntry())` |
| ELIGIBILITY_API | `ageAtEntry` | `getAgeAtEntry()` | PREMIUM_CALC_API | `ageAtEntry` | `req.setAgeAtEntry(context.getEligibilityResult().getAgeAtEntry())` |
| EDC_API | `creditScore` | `getCreditScore()` | MEDICAL_API | `creditScore` | `req.setCreditScore(context.getEdcResult().getCreditScore())` |
| EDC_API | `creditScore` | `getCreditScore()` | UNDERWRITING_API | `creditScore` | `req.setCreditScore(context.getEdcResult().getCreditScore())` |
| PASA_API | `pasaScore` | `getPasaScore()` | UNDERWRITING_API | `pasaScore` | `req.setPasaScore(context.getPasaResult().getPasaScore())` |
| TASA_API | `riskCategory` | `getRiskCategory()` | UNDERWRITING_API | `tasaRiskCategory` | `req.setTasaRiskCategory(context.getTasaResult().getRiskCategory())` |
| MEDICAL_API | `riskCategory` | `getRiskCategory()` | PREMIUM_CALC_API | `riskCategory` | `req.setRiskCategory(context.getMedicalResult().getRiskCategory())` |
| MEDICAL_API | `loadingFactor` | `getLoadingFactor()` | PREMIUM_CALC_API | `loadingFactor` | `req.setLoadingFactor(context.getMedicalResult().getLoadingFactor())` |
| MEDICAL_API | `medicalScore` | `getMedicalScore()` | UNDERWRITING_API | `medicalScore` | `req.setMedicalScore(context.getMedicalResult().getMedicalScore())` |
| MEDICAL_API | `riskCategory` | `getRiskCategory()` | UNDERWRITING_API | `medicalRiskCategory` | `req.setMedicalRiskCategory(context.getMedicalResult().getRiskCategory())` |
| MEDICAL_API | `loadingFactor` | `getLoadingFactor()` | UNDERWRITING_API | `loadingFactor` | `req.setLoadingFactor(context.getMedicalResult().getLoadingFactor())` |
| PREMIUM_CALC_API | `calculatedPremium` | `getCalculatedPremium()` | PROPOSAL_SUBMIT_API | `calculatedPremium` | `req.setCalculatedPremium(context.getPremiumResult().getCalculatedPremium())` |
| PREMIUM_CALC_API | `frequency` | `getFrequency()` | PROPOSAL_SUBMIT_API | `frequency` | `req.setFrequency(context.getPremiumResult().getFrequency())` |
| PREMIUM_CALC_API | `calculatedPremium` | `getCalculatedPremium()` | PAS_API | `productDetailsDTO.premiumAmount` | `productDetails.setPremiumAmount(...)` in `buildProductDetails()` |
| PREMIUM_CALC_API | `frequency` | `getFrequency()` | PAS_API | `productDetailsDTO.premFrequency` | `productDetails.setPremFrequency(...)` in `buildProductDetails()` |
| UNDERWRITING_API | `decision` | `getDecision()` | PROPOSAL_SUBMIT_API | `underwritingDecision` | `req.setUnderwritingDecision(context.getUnderwritingResult().getDecision())` |
| UNDERWRITING_API | `decision` (null check) | — | PAS_API | `integrations[AWS]` | If `underwritingResult != null` → adds AWS integration entry |
| MEDICAL_API | result (null check) | — | PAS_API | `integrations[MRS]` | If `medicalResult != null` → adds MRS integration entry |
| DOCUMENT_API | `documentId` | `getDocumentId()` | PROPOSAL_SUBMIT_API | `documentId` | `req.setDocumentId(context.getDocumentResult().getDocumentId())` |
| PROPOSAL_SUBMIT_API | `proposalNumber` | `getProposalNumber()` | PAS_API | `request.proposalNumber` | `body.setProposalNumber(context.getProposalResult().getProposalNumber())` |

---

## 3. Inbound Params Master Reference

All known `stringvalN` assignments across all APIs. Source: POJO comments + `PasApiClient.java` direct `raw.get()` calls.

| stringvalN | Logical Field | Data Type | Used By APIs | Notes / LOV / Default |
|------------|--------------|-----------|-------------|----------------------|
| `stringval1` | IP firstName | String | ELIGIBILITY, SCORING, MEDICAL | TRIM |
| `stringval2` | IP lastName | String | ELIGIBILITY, SCORING, MEDICAL | TRIM |
| `stringval3` | IP middleName | String | ELIGIBILITY | TRIM, optional |
| `stringval4` | IP dateOfBirth | Date | ELIGIBILITY, SCORING, MEDICAL | Format: `dd/MM/yyyy` |
| `stringval5` | gender | String | ELIGIBILITY | UPPERCASE; LOV: `MALE`,`FEMALE`; Default: `MALE` |
| `stringval6` | maritalStatus | String | ELIGIBILITY | UPPERCASE; LOV: `SINGLE`,`MARRIED`,`DIVORCED` |
| `stringval7` | nationality | String | ELIGIBILITY | UPPERCASE; Default: `INDIAN` |
| `stringval10` | mobileNumber | String | ELIGIBILITY | Mandatory |
| `stringval11` | emailAddress | String | ELIGIBILITY | LOWERCASE |
| `stringval12` | IP salutation | String | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval13` | IP firstName | String | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval14` | IP middleName | String | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval15` | IP lastName | String | PAS (basicPolicyInsured, ipBasicDetails) | — |
| `stringval16` | IP dateOfBirth | String | PAS (basicPolicyInsured, ipBasicDetails) | Passed as String (not Date) to PAS |
| `stringval18` | IP gender | String | PAS (basicPolicyInsured, ipBasicDetails) | M→MALE, F→FEMALE (Fix 3) |
| `stringval19` | mobileNumber | String | PAS (IP/PH/Payer contactDetails) | Wrapped in `PhoneNumber` object |
| `stringval20` | emailAddress | String | PAS (IP/PH/Payer contactDetails) | Wrapped in `EmailAddress` object (Fix 1) |
| `stringval31` | baseCoverageCode | String | PAS (productSelection) | — |
| `stringval45` | panNumber | String | ELIGIBILITY, SCORING, MEDICAL, KYC | UPPERCASE, Mandatory |
| `stringval46` | PH firstName | String | PAS (phBasicDetails) | — |
| `stringval47` | PH relationshipToIP | String | PAS (phDetails) | Default: `""` empty string if null (Fix 8) |
| `stringval48` | PH dateOfBirth | String | PAS (phBasicDetails) | — |
| `stringval49` | PH/Payer gender | String | PAS (phBasicDetails, payerBasicDetails) | M→MALE, F→FEMALE (Fix 6, Fix 8) |
| `stringval50` | productCode | String | ELIGIBILITY, PREMIUM_CALC, UNDERWRITING, PROPOSAL | — |
| `stringval51` | policyTerm | Integer | ELIGIBILITY, SCORING, MEDICAL, PREMIUM_CALC, UNDERWRITING, PROPOSAL | — |
| `stringval52` | sumAssured | Decimal | ELIGIBILITY, SCORING, MEDICAL, PREMIUM_CALC, UNDERWRITING, PROPOSAL | — |
| `stringval60` | annualIncome | Decimal | SCORING (EDC/PASA/TASA) | — |
| `stringval61` | existingLoans | Decimal | SCORING | Default: `0` |
| `stringval62` | employmentType | String | SCORING | UPPERCASE; LOV: `SALARIED`,`SELF_EMPLOYED` |
| `stringval70` | smokingStatus | String | MEDICAL, PREMIUM_CALC | UPPERCASE; Default: `NON_SMOKER` |
| `stringval71` | alcoholConsumption | String | MEDICAL | UPPERCASE; Default: `NONE` |
| `stringval72` | existingConditions | String | MEDICAL | Optional |
| `stringval73` | height | Decimal | MEDICAL | cm, optional |
| `stringval74` | weight | Decimal | MEDICAL | kg, optional |
| `stringval99` | IP maritalStatus | String | PAS (ipBasicDetails) | M→MARRIED, S→SINGLE, D→DIVORCED, W→WIDOWED (Fix 4) |
| `stringval117` | Aadhar number | String | KYC (inferred), PAS (IP/PH/basicPolicyInsured) | Preferred ID proof; if blank → PAN fallback (Fix 2) |
| `stringval122` | PAN number | String | PAS (IP/PH/basicPolicyInsured) fallback | Used when stringval117 is blank (Fix 2) |
| `stringval130` | PH annualIncome | String | PAS (phOccupationDetails) | Fix 7: null causes PAS rejection |
| `stringval133` | PH occupation | String | PAS (phOccupationDetails) | — |

---

## 4. API-by-API Mapping Tables

---

### API 1 — ELIGIBILITY\_API
**Client:** `EligibilityApiClient.java` · **Request:** `EligibilityRequest.java` · **Response:** `EligibilityResponse.java`
**Stage:** 1 · First stage — must pass before all others

| # | Request Field | Source | InboundRequest Param | Type | Mandatory | LOV / Default | BA Comment |
|---|--------------|--------|----------------------|------|-----------|---------------|------------|
| 1 | `firstName` | InboundRequest | `stringval1` | String | Yes | — TRIM | Confirmed |
| 2 | `lastName` | InboundRequest | `stringval2` | String | Yes | — TRIM | Confirmed |
| 3 | `middleName` | InboundRequest | `stringval3` | String | No | — TRIM | Optional |
| 4 | `dateOfBirth` | InboundRequest | `stringval4` | Date | Yes | Format: `dd/MM/yyyy` | Confirmed |
| 5 | `gender` | InboundRequest | `stringval5` | String | No | LOV: `MALE`,`FEMALE` / Default: `MALE` | Default applied when blank |
| 6 | `maritalStatus` | InboundRequest | `stringval6` | String | No | LOV: `SINGLE`,`MARRIED`,`DIVORCED` | TODO: Confirm full LOV |
| 7 | `nationality` | InboundRequest | `stringval7` | String | No | Default: `INDIAN` | — |
| 8 | `mobileNumber` | InboundRequest | `stringval10` | String | Yes | — | Confirmed |
| 9 | `emailAddress` | InboundRequest | `stringval11` | String | No | — LOWERCASE | Optional |
| 10 | `panNumber` | InboundRequest | `stringval45` | String | Yes | — UPPERCASE | Confirmed |
| 11 | `productCode` | InboundRequest | `stringval50` | String | Yes | — | Confirmed |
| 12 | `policyTerm` | InboundRequest | `stringval51` | Integer | Yes | — | Confirmed |
| 13 | `sumAssured` | InboundRequest | `stringval52` | Decimal | Yes | — | Confirmed |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `eligibilityId` | String | MEDICAL_API input | Traceability link |
| `ageAtEntry` | Integer | MEDICAL_API input, PREMIUM_CALC_API input | Calculated by eligibility service |
| `status` | String | Logged | `ELIGIBLE` / `NOT_ELIGIBLE` / `PENDING_REVIEW` |

---

### API 2 — EDC\_API (Credit Score)
### API 3 — PASA\_API (Financial Score)
### API 4 — TASA\_API (Risk Score)

**Client:** `ScoringApiClient.java` (single client handles all three) · **Request:** `ScoringRequest.java`
**Responses:** `EdcResponse.java` · `PasaResponse.java` · `TasaResponse.java`
**Stage:** 2 · All three run **in parallel** via `CompletableFuture`. Same `ScoringRequest` sent to each.

| # | Request Field | Source | InboundRequest Param | Type | Mandatory | LOV / Default | BA Comment |
|---|--------------|--------|----------------------|------|-----------|---------------|------------|
| 1 | `panNumber` | InboundRequest | `stringval45` | String | Yes | — UPPERCASE | Confirmed |
| 2 | `firstName` | InboundRequest | `stringval1` | String | No | — TRIM | — |
| 3 | `lastName` | InboundRequest | `stringval2` | String | No | — TRIM | — |
| 4 | `dateOfBirth` | InboundRequest | `stringval4` | Date | No | Format: `dd/MM/yyyy` | — |
| 5 | `annualIncome` | InboundRequest | `stringval60` | Decimal | No | — | TODO: Is this mandatory for EDC? |
| 6 | `existingLoans` | InboundRequest | `stringval61` | Decimal | No | Default: `0` | Default when blank |
| 7 | `employmentType` | InboundRequest | `stringval62` | String | No | LOV: `SALARIED`,`SELF_EMPLOYED` UPPERCASE | TODO: Confirm full LOV |
| 8 | `sumAssured` | InboundRequest | `stringval52` | Decimal | Yes | — | Confirmed |
| 9 | `policyTerm` | InboundRequest | `stringval51` | Integer | Yes | — | Confirmed |

> **Note:** Orchestrator has a TODO to enrich `ScoringRequest` from `eligibilityResult` if scoring APIs need any eligibility fields.

**Response fields consumed downstream:**

| API | Response Field | Type | Consumed By | Purpose |
|-----|---------------|------|-------------|---------|
| EDC_API | `creditScore` | Integer | MEDICAL_API, UNDERWRITING_API | Bureau credit score |
| EDC_API | `status` | String | Logged | `PASS` / `FAIL` / `REFER` |
| PASA_API | `pasaScore` | Integer | UNDERWRITING_API | Financial score |
| PASA_API | `scoreGrade` | String | Logged | `A` / `B` / `C` / `D` |
| TASA_API | `riskCategory` | String | UNDERWRITING_API | `LOW` / `MEDIUM` / `HIGH` |
| TASA_API | `status` | String | Logged | — |

---

### API 5 — MEDICAL\_API
**Client:** `MedicalApiClient.java` · **Request:** `MedicalRequest.java` · **Response:** `MedicalResponse.java`
**Stage:** 3 · Runs after ALL scoring APIs complete

| # | Request Field | Source | InboundRequest Param / Prior API | Type | Mandatory | LOV / Default | BA Comment |
|---|--------------|--------|----------------------------------|------|-----------|---------------|------------|
| 1 | `firstName` | InboundRequest | `stringval1` | String | Yes | — TRIM | Confirmed |
| 2 | `lastName` | InboundRequest | `stringval2` | String | Yes | — TRIM | Confirmed |
| 3 | `dateOfBirth` | InboundRequest | `stringval4` | Date | Yes | Format: `dd/MM/yyyy` | Confirmed |
| 4 | `panNumber` | InboundRequest | `stringval45` | String | Yes | — UPPERCASE | Confirmed |
| 5 | `sumAssured` | InboundRequest | `stringval52` | Decimal | Yes | — | Confirmed |
| 6 | `policyTerm` | InboundRequest | `stringval51` | Integer | Yes | — | Confirmed |
| 7 | `smokingStatus` | InboundRequest | `stringval70` | String | No | Default: `NON_SMOKER` UPPERCASE | Default when blank |
| 8 | `alcoholConsumption` | InboundRequest | `stringval71` | String | No | Default: `NONE` UPPERCASE | Default when blank |
| 9 | `existingConditions` | InboundRequest | `stringval72` | String | No | — | Optional; partner may not know |
| 10 | `height` | InboundRequest | `stringval73` | Decimal | No | — (cm) | Optional |
| 11 | `weight` | InboundRequest | `stringval74` | Decimal | No | — (kg) | Optional |
| 12 | `eligibilityId` | **ENRICHED** | `EligibilityResponse.eligibilityId` | String | — | — | Set in orchestrator after Stage 1 |
| 13 | `ageAtEntry` | **ENRICHED** | `EligibilityResponse.ageAtEntry` | Integer | — | — | Set in orchestrator after Stage 1 |
| 14 | `creditScore` | **ENRICHED** | `EdcResponse.creditScore` | Integer | — | — | Set in orchestrator after EDC (Stage 2) |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `medicalScore` | Integer | UNDERWRITING_API | Health score |
| `riskCategory` | String | PREMIUM_CALC_API, UNDERWRITING_API | `STANDARD` / `SUBSTANDARD` / `DECLINED` |
| `loadingFactor` | Decimal | PREMIUM_CALC_API, UNDERWRITING_API | Extra premium % due to health risk |
| `status` (null check) | — | PAS_API | If `medicalResult != null` → `MRS` integration added to PAS request |

---

### API 6 — KYC\_API
**Client:** `KycApiClient.java` · **Request:** `KycRequest.java` · **Response:** `KycResponse.java`
**Stage:** 4
**STATUS: SKELETON — Contract not yet implemented. Fields below are placeholder/inferred.**

| # | Request Field | Source | InboundRequest Param | Type | Mandatory | BA Comment |
|---|--------------|--------|----------------------|------|-----------|------------|
| 1 | `panNumber` | InboundRequest | `stringval45` (inferred) | String | Yes | **TODO: Confirm with KYC API contract** |
| 2 | `firstName` | InboundRequest | `stringval1` (inferred) | String | Yes | **TODO: Confirm** |
| 3 | `lastName` | InboundRequest | `stringval2` (inferred) | String | Yes | **TODO: Confirm** |
| 4 | `dateOfBirth` | InboundRequest | `stringval4` (inferred) | Date | Yes | **TODO: Confirm** |
| 5 | `aadhaarNumber` | InboundRequest | `stringval117` (inferred) | String | No | **TODO: Confirm; same param used in PAS** |
| 6 | `addressLine1` | InboundRequest | **UNKNOWN — param not mapped** | String | ? | **TODO: Identify source stringvalN** |
| 7 | `city` | InboundRequest | **UNKNOWN — param not mapped** | String | ? | **TODO: Identify source stringvalN** |
| 8 | `pincode` | InboundRequest | **UNKNOWN — param not mapped** | String | ? | **TODO: Identify source stringvalN** |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `status` | String | Logged only | `VERIFIED` / `FAILED` / `PENDING` — no downstream enrichment currently |

---

### API 7 — PREMIUM\_CALC\_API
**Client:** `PremiumApiClient.java` · **Request:** `PremiumRequest.java` · **Response:** `PremiumResponse.java`
**Stage:** 5
**STATUS: SKELETON — TODO note in POJO; contract fields not confirmed**

| # | Request Field | Source | InboundRequest Param / Prior API | Type | Mandatory | BA Comment |
|---|--------------|--------|----------------------------------|------|-----------|------------|
| 1 | `sumAssured` | InboundRequest | `stringval52` | Decimal | Yes | Confirmed |
| 2 | `policyTerm` | InboundRequest | `stringval51` | Integer | Yes | Confirmed |
| 3 | `productCode` | InboundRequest | `stringval50` | String | Yes | Confirmed |
| 4 | `smokingStatus` | InboundRequest | `stringval70` (inferred) | String | No | **TODO: Confirm if premium API needs this** |
| 5 | `ageAtEntry` | **ENRICHED** | `EligibilityResponse.ageAtEntry` | Integer | — | Set in orchestrator after Stage 1 |
| 6 | `riskCategory` | **ENRICHED** | `MedicalResponse.riskCategory` | String | — | Set in orchestrator after Stage 3 |
| 7 | `loadingFactor` | **ENRICHED** | `MedicalResponse.loadingFactor` | Decimal | — | Set in orchestrator after Stage 3 |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `calculatedPremium` | Decimal | PROPOSAL_SUBMIT_API, PAS_API | Final premium amount |
| `frequency` | String | PROPOSAL_SUBMIT_API, PAS_API | `ANNUAL` / `SEMI_ANNUAL` / `QUARTERLY` / `MONTHLY` |

---

### API 8 — UNDERWRITING\_API
**Client:** `UnderwritingApiClient.java` · **Request:** `UnderwritingRequest.java` · **Response:** `UnderwritingResponse.java`
**Stage:** 6
**CRITICAL: If `decision = DECLINED` → `JourneyStageException` thrown → journey stops. No Document or Proposal runs.**
**STATUS: SKELETON — TODO note in POJO; contract fields not confirmed**

| # | Request Field | Source | InboundRequest Param / Prior API | Type | Mandatory | BA Comment |
|---|--------------|--------|----------------------------------|------|-----------|------------|
| 1 | `sumAssured` | InboundRequest | `stringval52` | Decimal | Yes | Confirmed |
| 2 | `policyTerm` | InboundRequest | `stringval51` | Integer | Yes | Confirmed |
| 3 | `productCode` | InboundRequest | `stringval50` | String | Yes | Confirmed |
| 4 | `creditScore` | **ENRICHED** | `EdcResponse.creditScore` | Integer | — | Set after Stage 2 (EDC) |
| 5 | `pasaScore` | **ENRICHED** | `PasaResponse.pasaScore` | Integer | — | Set after Stage 2 (PASA) |
| 6 | `tasaRiskCategory` | **ENRICHED** | `TasaResponse.riskCategory` | String | — | Set after Stage 2 (TASA) |
| 7 | `medicalScore` | **ENRICHED** | `MedicalResponse.medicalScore` | Integer | — | Set after Stage 3 |
| 8 | `medicalRiskCategory` | **ENRICHED** | `MedicalResponse.riskCategory` | String | — | Set after Stage 3 |
| 9 | `loadingFactor` | **ENRICHED** | `MedicalResponse.loadingFactor` | Decimal | — | Set after Stage 3 |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `decision` | String | PROPOSAL_SUBMIT_API | `APPROVED` / `DECLINED` / `REFERRED` |
| `decisionCode` | String | Journey exception | Included in exception message on DECLINED |
| `decision` (null check) | — | PAS_API | If `underwritingResult != null` → `AWS` integration entry added |

---

### API 9 — DOCUMENT\_API
**Client:** `DocumentApiClient.java` · **Request:** `DocumentRequest.java` · **Response:** `DocumentResponse.java`
**Stage:** 7
**STATUS: SKELETON — Most fields are TODO. Only `correlationId` explicitly set in orchestrator.**

| # | Request Field | Source | InboundRequest Param / Context | Type | Mandatory | BA Comment |
|---|--------------|--------|-------------------------------|------|-----------|------------|
| 1 | `correlationId` | Runtime context | `context.getCorrelationId()` | String | Yes | **HARDCODED** from journey context (UUID) |
| 2 | `productCode` | InboundRequest | `stringval50` (inferred) | String | ? | **TODO: Confirm with DOCUMENT_API contract** |
| 3 | `applicantName` | InboundRequest | **UNKNOWN** | String | ? | **TODO: Derived from stringval1+stringval2? Confirm format** |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `documentId` | String | PROPOSAL_SUBMIT_API | Document reference |

---

### API 10 — PROPOSAL\_SUBMIT\_API
**Client:** `ProposalApiClient.java` · **Request:** `ProposalRequest.java` · **Response:** `ProposalResponse.java`
**Stage:** 8 · Final stage before PAS
**STATUS: SKELETON — TODO note in POJO**

| # | Request Field | Source | InboundRequest Param / Prior API | Type | Mandatory | BA Comment |
|---|--------------|--------|----------------------------------|------|-----------|------------|
| 1 | `productCode` | InboundRequest | `stringval50` | String | Yes | Confirmed |
| 2 | `sumAssured` | InboundRequest | `stringval52` | Decimal | Yes | Confirmed |
| 3 | `policyTerm` | InboundRequest | `stringval51` | Integer | Yes | Confirmed |
| 4 | `applicantName` | InboundRequest | **UNKNOWN** | String | ? | **TODO: stringval1+stringval2? Confirm** |
| 5 | `calculatedPremium` | **ENRICHED** | `PremiumResponse.calculatedPremium` | Decimal | — | Set after Stage 5 |
| 6 | `frequency` | **ENRICHED** | `PremiumResponse.frequency` | String | — | Set after Stage 5 |
| 7 | `underwritingDecision` | **ENRICHED** | `UnderwritingResponse.decision` | String | — | Set after Stage 6 |
| 8 | `documentId` | **ENRICHED** | `DocumentResponse.documentId` | String | — | Set after Stage 7 |

**Response fields consumed downstream:**

| Response Field | Type | Consumed By | Purpose |
|---------------|------|-------------|---------|
| `proposalNumber` | String | PAS_API | Set as `body.proposalNumber` |

---

### API 11 — PAS\_API
**Client:** `PasApiClient.java` · Method: `buildPasRequest()` → `submitAndGetApplicationNumber()`
**Stage:** Post-journey · Runs after all 8 stages succeed
**STATUS: FULLY IMPLEMENTED — All stringvalN directly accessed via `raw.get("stringvalN")`**

---

#### 11a. Header

| # | PAS Field | Source | Value / Param | BA Comment |
|---|-----------|--------|---------------|------------|
| 1 | `header.correlationId` | Runtime | `context.getCorrelationId()` | Auto-set |
| 2 | `header.processVars` | **HARDCODED** | `new ProcessVars()` (empty) | TODO: Does PAS need processVars populated? |

---

#### 11b. Request Body — Top Level

| # | PAS Field | Source | Value / Param | BA Comment |
|---|-----------|--------|---------------|------------|
| 1 | `request.proposalNumber` | **ENRICHED** | `ProposalResponse.proposalNumber` (Stage 8) | Confirmed |
| 2 | `request.status` | **HARDCODED** | `"DRAFT"` | Always DRAFT at submission time |

---

#### 11c. basicPolicyInsured\[0\]
*(IP identity block — `buildBasicPolicyInsured(raw)`)*

| # | PAS Field | InboundRequest Param | Transformation / Fix | BA Comment |
|---|-----------|----------------------|----------------------|------------|
| 1 | `salutation` | `stringval12` | — | Confirmed |
| 2 | `policyInsuredFirstName` | `stringval13` | — | Confirmed |
| 3 | `policyInsuredMiddleName` | `stringval14` | — | Optional |
| 4 | `policyInsuredLastName` | `stringval15` | — | Confirmed |
| 5 | `policyInsuredDateOfBirth` | `stringval16` | Passed as String | Confirmed |
| 6 | `gender` | `stringval18` | M→`MALE`, F→`FEMALE` **(Fix 3)** | LOV: `MALE`,`FEMALE` |
| 7 | `policyInsuredLegalIdentifierCode` | `stringval117` (Aadhar check) | If `stringval117` non-blank → `"AADHAR_REFERENCE_CODE"` else `"PAN"` **(Fix 2)** | Code value is hardcoded string |
| 8 | `policyInsuredLegalIdentifierValue` | `stringval117` or `stringval122` | Aadhar preferred; PAN fallback **(Fix 2)** | Confirmed |

---

#### 11d. productSelection
*(direct `raw.get()` in `buildPasRequest()`)*

| # | PAS Field | InboundRequest Param | BA Comment |
|---|-----------|----------------------|------------|
| 1 | `baseCoverageCode` | `stringval31` | Confirmed |
| 2 | `branchCode` | **NOT MAPPED** | **TODO: Identify source stringvalN** |
| 3 | `policyIssueState` | **NOT MAPPED** | **TODO: Identify source stringvalN** |
| 4 | `productPlan` | **NOT MAPPED** | **TODO: Identify source stringvalN** |

---

#### 11e. bankDetailsDTO

| PAS Field | Value | BA Comment |
|-----------|-------|------------|
| entire object | `new BankDetailsDTO()` (empty) | **HARDCODED** empty. **TODO: Does PAS need bank fields populated?** |

---

#### 11f. integrations list

| Integration | Condition | `integrationName` | `integrationStatus` | BA Comment |
|-------------|-----------|-------------------|--------------------|------------|
| Entry 1 | `context.getUnderwritingResult() != null` | `"AWS"` (**HARDCODED**) | `true` (**HARDCODED**) | Added if UW completed |
| Entry 2 | `context.getMedicalResult() != null` | `"MRS"` (**HARDCODED**) | `true` (**HARDCODED**) | Added if Medical completed |

---

#### 11g. journeyDetails

| PAS Field | Value | BA Comment |
|-----------|-------|------------|
| entire object | `new JourneyDetails()` (empty) | **HARDCODED** empty. **TODO: Confirm fields required by PAS** |

---

#### 11h. habbitDetailsDTO — Lifestyle flags
*(all defaults applied in `buildHabbitDetails()` — **Fix 5**)*

| # | PAS Field | InboundRequest Param | Hardcoded Default | BA Comment |
|---|-----------|----------------------|-------------------|------------|
| 1 | `isAlcohol` | **NOT MAPPED** | `false` **(Fix 5)** | **TODO: Should map from `stringval71` (alcoholConsumption)?** |
| 2 | `isChangeInWeight` | **NOT MAPPED** | `false` **(Fix 5)** | **TODO: Identify source param if needed** |
| 3 | `isDGH` | **NOT MAPPED** | `false` **(Fix 5)** | **TODO: Identify source param if needed** |
| 4 | `isPEP` | **NOT MAPPED** | `false` **(Fix 5)** | **TODO: Identify source param if needed** |
| 5 | `isSmoker` | **NOT MAPPED** | `false` **(Fix 5)** | **TODO: Should map from `stringval70` (smokingStatus)?** |
| 6 | `isTobacco` | **NOT MAPPED** | `false` **(Fix 5)** | **TODO: Identify source param if needed** |
| 7 | `height` | `stringval73` available | `null` | **TODO: Should PAS receive height? Already sent to Medical** |
| 8 | `weight` | `stringval74` available | `null` | **TODO: Should PAS receive weight? Already sent to Medical** |
| 9 | `bmi` | — | `null` | **TODO: Calculate from height/weight if needed** |

---

#### 11i. ipDetails — IP (Insured Person)
*(built in `buildIpDetails(raw)`)*

| # | PAS Field Path | InboundRequest Param / Source | Transformation / Fix | BA Comment |
|---|----------------|-------------------------------|----------------------|------------|
| 1 | `ippersonalDetails.ipBasicDetails.salutation` | `stringval12` | — | Confirmed |
| 2 | `ippersonalDetails.ipBasicDetails.firstName` | `stringval13` | — | Confirmed |
| 3 | `ippersonalDetails.ipBasicDetails.middleName` | `stringval14` | — | Optional |
| 4 | `ippersonalDetails.ipBasicDetails.lastName` | `stringval15` | — | Confirmed |
| 5 | `ippersonalDetails.ipBasicDetails.dateOfBirth` | `stringval16` | — | Confirmed |
| 6 | `ippersonalDetails.ipBasicDetails.gender` | `stringval18` | M→`MALE`, F→`FEMALE` **(Fix 3)** | Confirmed |
| 7 | `ippersonalDetails.ipBasicDetails.maritalStatus` | `stringval99` | M→`MARRIED` S→`SINGLE` D→`DIVORCED` W→`WIDOWED` **(Fix 4)** | LOV fully mapped |
| 8 | `ippersonalDetails.ipBasicDetails.idProofDoc` | `stringval117` check | `"AADHAR_REFERENCE_CODE"` or `"PAN"` **(Fix 2)** | Confirmed |
| 9 | `ippersonalDetails.ipBasicDetails.idProofValue` | `stringval117` or `stringval122` | Aadhar preferred **(Fix 2)** | Confirmed |
| 10 | `ippersonalDetails.contactDetails.emailId` | `stringval20` | Wrapped in `EmailAddress` object **(Fix 1)** | Bare String causes PAS rejection |
| 11 | `ippersonalDetails.contactDetails.mobileNumber` | `stringval19` | Wrapped in `PhoneNumber` object | Confirmed |
| 12 | `currentAddress` | **NOT MAPPED** | `null` | **TODO: Identify address stringvalN params** |
| 13 | `permanentAddress` | **NOT MAPPED** | `null` | **TODO: Identify address stringvalN params** |
| 14 | `kycType` | **NOT MAPPED** | `null` | **TODO: Confirm if required** |
| 15 | `ipEducationAndOccupationDetails` | **NOT MAPPED** | `null` | **TODO: Is IP occupation needed? (PH occupation IS mapped)** |

---

#### 11j. phDetails — Policyholder
*(built in `buildPhDetails(raw)`)*

| # | PAS Field Path | InboundRequest Param / Source | Transformation / Fix | BA Comment |
|---|----------------|-------------------------------|----------------------|------------|
| 1 | `phPersonalDetails.basicPersonDetails.firstName` | `stringval46` | — | Confirmed — separate from IP |
| 2 | `phPersonalDetails.basicPersonDetails.dateOfBirth` | `stringval48` | — | Confirmed |
| 3 | `phPersonalDetails.basicPersonDetails.gender` | `stringval49` | M→`MALE`, F→`FEMALE` **(Fix 8)** | Confirmed |
| 4 | `phPersonalDetails.basicPersonDetails.idProofDoc` | `stringval117` check | `"AADHAR_REFERENCE_CODE"` or `"PAN"` **(Fix 8)** | Same Aadhar/PAN logic |
| 5 | `phPersonalDetails.basicPersonDetails.idProofValue` | `stringval117` or `stringval122` | **(Fix 8)** | Confirmed |
| 6 | `phPersonalDetails.basicPersonDetails.lastName` | **NOT MAPPED** | `null` | **TODO: Confirm source param — stringval47 is used for relationshipToIP** |
| 7 | `phPersonalDetails.contactDetails.emailId` | `stringval20` | Wrapped in `EmailAddress` object **(Fix 1)** | Same as IP email — TODO: confirm if PH can have different |
| 8 | `phPersonalDetails.contactDetails.mobileNumber` | `stringval19` | Wrapped in `PhoneNumber` object | Same as IP mobile |
| 9 | `phEducationAndOccupationDetails.occupationDetails.annualIncome` | `stringval130` | — **(Fix 7)** | Null causes PAS rejection |
| 10 | `phEducationAndOccupationDetails.occupationDetails.occupation` | `stringval133` | — | Confirmed |
| 11 | `relationshipToIP` | `stringval47` | Default: `""` if null **(Fix 8)** | Null causes PAS rejection |
| 12 | `currentAddress` | **NOT MAPPED** | `null` | **TODO: Identify address params** |
| 13 | `permanentAddress` | **NOT MAPPED** | `null` | **TODO: Identify address params** |
| 14 | `kycType` | **NOT MAPPED** | `null` | **TODO: Confirm if required** |

---

#### 11k. payerDetails
*(built in `buildPayerDetails(raw)`)*

| # | PAS Field Path | InboundRequest Param / Source | Transformation / Fix | BA Comment |
|---|----------------|-------------------------------|----------------------|------------|
| 1 | `payerPersonalDetails.basicPersonDetails.gender` | `stringval49` | M→`MALE`, F→`FEMALE` **(Fix 6)** | PH gender used for payer |
| 2 | `payerPersonalDetails.contactDetails.emailId` | `stringval20` | Wrapped in `EmailAddress` object **(Fix 1)** | Same email as IP/PH |
| 3 | `payerPersonalDetails.contactDetails.mobileNumber` | `stringval19` | Wrapped in `PhoneNumber` object | Same mobile as IP/PH |
| 4 | `payerPersonalDetails.basicPersonDetails.firstName` | **NOT MAPPED** | `null` | **TODO: Is payer always same as PH? Confirm** |
| 5 | `payerPersonalDetails.basicPersonDetails.dateOfBirth` | **NOT MAPPED** | `null` | **TODO: Confirm payer DOB source** |

---

#### 11l. productDetailsDTO
*(built in `buildProductDetails(context, raw)` — **Fix 9**)*

| # | PAS Field | Source | Value / Param | BA Comment |
|---|-----------|--------|---------------|------------|
| 1 | `premiumAmount` | **ENRICHED** | `PremiumResponse.calculatedPremium` (as String) | From Stage 5 |
| 2 | `premFrequency` | **ENRICHED** | `PremiumResponse.frequency` | From Stage 5 |
| 3 | `ppt` | **HARDCODED** | `0` **(Fix 9)** | PAS rejects null |
| 4 | `riderDetails` | **HARDCODED** | `[]` empty list **(Fix 9)** | PAS rejects null |
| 5 | `productName` | **NOT MAPPED** | `null` | **TODO: Source param not identified** |
| 6 | `optionOrVariant` | **NOT MAPPED** | `null` | **TODO: Source param not identified** |
| 7 | `coverage` | **NOT MAPPED** | `null` | **TODO: Source param not identified** |
| 8 | `fundDetails` | **NOT MAPPED** | `null` | **TODO: Required for ULIP products?** |

---

## 5. Applied Fixes Reference (PasApiClient.java)

| Fix # | Problem | Code Fix | Affected PAS Fields |
|-------|---------|----------|-------------------|
| Fix 1 | Email sent as bare String — PAS rejected | Wrap in `EmailAddress` object before setting on `ContactDetails` | `contactDetails.emailId` in IP, PH, Payer |
| Fix 2 | No ID proof logic | If `stringval117` (Aadhar) non-blank → `AADHAR_REFERENCE_CODE`; else `PAN` from `stringval122` | `policyInsuredLegalIdentifierCode/Value`, `idProofDoc/Value` |
| Fix 3 | PAS needs full gender string, not `"M"` / `"F"` | `M`→`MALE`, `F`→`FEMALE` in `mapGender()` | IP gender in basicPolicyInsured and ipBasicDetails |
| Fix 4 | PAS needs full marital status string | `M`→`MARRIED`, `S`→`SINGLE`, `D`→`DIVORCED`, `W`→`WIDOWED` in `mapMaritalStatus()` | `ipBasicDetails.maritalStatus` |
| Fix 5 | Lifestyle boolean flags were `null` — PAS rejected | Default all to `false` in `buildHabbitDetails()` | All 6 boolean flags in `habbitDetailsDTO` |
| Fix 6 | Payer gender not mapped from `"M"`/`"F"` | Apply `mapGender()` to `stringval49` for payer | `payerBasicDetails.gender` |
| Fix 7 | PH `annualIncome` was `null` — PAS rejected | Explicitly set from `stringval130` | `phOccupationDetails.annualIncome` |
| Fix 8 | PH gender not mapped; `relationshipToIP` null causes rejection; PH ID proof missing | Gender mapped; `relationshipToIP` defaults to `""` (not null); Aadhar-or-PAN logic added | `phBasicDetails.gender`, `phDetails.relationshipToIP`, `phBasicDetails.idProofDoc/Value` |
| Fix 9 | `ppt` null causes rejection; `riderDetails` null causes rejection | `ppt = 0`; `riderDetails = new ArrayList<>()` | `productDetailsDTO.ppt`, `productDetailsDTO.riderDetails` |

---

## 6. Open Items — Pending Mapping Actions

| # | API | Open Item | BA Action | Dev Action | Priority |
|---|-----|-----------|-----------|------------|---------|
| 1 | KYC_API | Full API contract not defined — `KycRequest` has only placeholder fields | Get contract, confirm all field params | Fill KycRequest/KycResponse | **High** |
| 2 | DOCUMENT_API | Contract not defined — only `correlationId` explicitly set | Get contract, confirm fields | Fill DocumentRequest/DocumentResponse | **High** |
| 3 | PROPOSAL_SUBMIT_API | `applicantName` source not confirmed | Confirm: is it stringval1+stringval2 or separate param? | — | Medium |
| 4 | PAS productSelection | `branchCode`, `policyIssueState`, `productPlan` have no source param | Identify source stringvalN for each | Add to `buildPasRequest()` | **High** |
| 5 | PAS bankDetailsDTO | Sent as empty object — PAS may require bank fields | Confirm if bank details needed for submission | Implement if needed | **High** |
| 6 | PAS journeyDetails | Sent as empty object | Confirm required fields with PAS team | Implement if needed | Medium |
| 7 | PAS habbitDetailsDTO | `isSmoker`, `isAlcohol`, `isTobacco` hardcoded `false` — should they come from stringval70/71? | Confirm business rule — should partner-sent values override defaults? | Map from stringval70/71 if yes | Medium |
| 8 | PAS habbitDetailsDTO | `height`, `weight`, `bmi` are null | Confirm if PAS needs these (already sent to Medical via stringval73/74) | Map if needed | Medium |
| 9 | PAS ipDetails | `currentAddress`, `permanentAddress`, `kycType`, `ipEducationAndOccupationDetails` all null | Identify source params for address and IP occupation | Implement in `buildIpDetails()` | **High** |
| 10 | PAS phDetails | PH `lastName` has no source param (`stringval47` = relationshipToIP) | Identify correct stringvalN for PH last name | Add to `buildPhDetails()` | **High** |
| 11 | PAS phDetails | `currentAddress`, `permanentAddress`, `kycType` all null | Identify source params | Implement in `buildPhDetails()` | **High** |
| 12 | PAS payerDetails | Payer firstName, DOB not mapped — is payer always same as PH? | Confirm payer = PH or separate person | Map if separate | **High** |
| 13 | PAS productDetailsDTO | `productName`, `optionOrVariant`, `coverage`, `fundDetails` null | Identify source params | Map in `buildProductDetails()` | Medium |
| 14 | ELIGIBILITY_API | On retry, `eligibilityResult` not reloaded from DB — downstream stages fail | — | Add reload from `journey_stage_log` on retry | **High** |
| 15 | SCORING_API | Orchestrator has TODO: enrich `ScoringRequest` from `eligibilityResult` if needed | Confirm if EDC/PASA/TASA need eligibility fields | Implement enrichment | Medium |
| 16 | KYC_API | Orchestrator has TODO: enrich `KycRequest` from prior stages | Confirm if KYC needs `eligibilityId` or other outputs | Implement enrichment | Medium |
