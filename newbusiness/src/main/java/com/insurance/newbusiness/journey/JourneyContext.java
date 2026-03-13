package com.insurance.newbusiness.journey;

import com.insurance.newbusiness.integration.model.eligibility.EligibilityResponse;
import com.insurance.newbusiness.integration.model.scoring.EdcResponse;
import com.insurance.newbusiness.integration.model.scoring.PasaResponse;
import com.insurance.newbusiness.integration.model.scoring.TasaResponse;
import com.insurance.newbusiness.integration.model.medical.MedicalResponse;
import com.insurance.newbusiness.integration.model.kyc.KycResponse;
import com.insurance.newbusiness.integration.model.premium.PremiumResponse;
import com.insurance.newbusiness.integration.model.underwriting.UnderwritingResponse;
import com.insurance.newbusiness.integration.model.document.DocumentResponse;
import com.insurance.newbusiness.integration.model.proposal.ProposalResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JourneyContext — shared state for one request's end-to-end journey.
 *
 * Created once in NewBusinessService.receiveAndAcknowledge() and passed
 * through every stage in JourneyOrchestrator.
 *
 * ── TYPED RESULTS vs MAP ─────────────────────────────────────────────────────
 * Each stage sets its typed result on this context after the API call.
 * Later stages read from these typed results directly — no Map casting needed.
 *
 * Example:
 *   context.setMedicalResult(medicalResponse);   // set in MEDICAL_STAGE
 *   ...
 *   uwReq.setMedicalScore(                       // read in UNDERWRITING_STAGE
 *       context.getMedicalResult().getMedicalScore());
 *
 * This is compile-time safe. IDE autocomplete works. No ClassCastException at runtime.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────────
 * rawParams is immutable (set once at construction).
 * Typed result fields are written once per stage in sequence — no concurrent writes
 * EXCEPT during SCORING_STAGE where EDC/PASA/TASA write concurrently.
 * The three scoring setters use synchronized to handle concurrent writes safely.
 *
 * ── ADDING A NEW API RESULT ───────────────────────────────────────────────────
 * 1. Add private XxxResponse xxxResult field
 * 2. Add getter and setter
 * 3. Set it in JourneyOrchestrator after the API call
 * 4. Read it in later stages when building downstream requests
 */
public class JourneyContext {

    // ── Immutable core identity ───────────────────────────────────────────────
    private final String correlationId;   // UUID, in every log line via MDC
    private final String partnerCode;     // used for mapping lookup and reverse feed
    private final Map<String, String> rawParams; // the 600 stringval1..N — never changes

    // ── Journey result — set after PAS returns ────────────────────────────────
    private String applicationNumber;

    // ── Typed stage results — set by JourneyOrchestrator after each API call ──
    // These replace the old enrichedParams Map — fields are now type-safe.
    private EligibilityResponse  eligibilityResult;
    private EdcResponse          edcResult;          // parallel scoring
    private PasaResponse         pasaResult;         // parallel scoring
    private TasaResponse         tasaResult;         // parallel scoring
    private MedicalResponse      medicalResult;
    private KycResponse          kycResult;
    private PremiumResponse      premiumResult;
    private UnderwritingResponse underwritingResult;
    private DocumentResponse     documentResult;
    private ProposalResponse     proposalResult;

    public JourneyContext(String correlationId, String partnerCode, Map<String, String> rawParams) {
        this.correlationId = correlationId;
        this.partnerCode   = partnerCode;
        this.rawParams     = Collections.unmodifiableMap(new HashMap<>(rawParams));
    }

    // ── Core identity getters ─────────────────────────────────────────────────
    public String getCorrelationId()              { return correlationId; }
    public String getPartnerCode()                { return partnerCode; }
    public Map<String, String> getRawParams()     { return rawParams; }
    public String getApplicationNumber()          { return applicationNumber; }
    public void setApplicationNumber(String n)    { this.applicationNumber = n; }

    // ── Stage result getters/setters ─────────────────────────────────────────
    // Scoring setters are synchronized — EDC/PASA/TASA write concurrently

    public EligibilityResponse getEligibilityResult()              { return eligibilityResult; }
    public void setEligibilityResult(EligibilityResponse r)        { this.eligibilityResult = r; }

    public synchronized EdcResponse getEdcResult()                 { return edcResult; }
    public synchronized void setEdcResult(EdcResponse r)           { this.edcResult = r; }

    public synchronized PasaResponse getPasaResult()               { return pasaResult; }
    public synchronized void setPasaResult(PasaResponse r)         { this.pasaResult = r; }

    public synchronized TasaResponse getTasaResult()               { return tasaResult; }
    public synchronized void setTasaResult(TasaResponse r)         { this.tasaResult = r; }

    public MedicalResponse getMedicalResult()                       { return medicalResult; }
    public void setMedicalResult(MedicalResponse r)                 { this.medicalResult = r; }

    public KycResponse getKycResult()                               { return kycResult; }
    public void setKycResult(KycResponse r)                         { this.kycResult = r; }

    public PremiumResponse getPremiumResult()                       { return premiumResult; }
    public void setPremiumResult(PremiumResponse r)                 { this.premiumResult = r; }

    public UnderwritingResponse getUnderwritingResult()             { return underwritingResult; }
    public void setUnderwritingResult(UnderwritingResponse r)       { this.underwritingResult = r; }

    public DocumentResponse getDocumentResult()                     { return documentResult; }
    public void setDocumentResult(DocumentResponse r)               { this.documentResult = r; }

    public ProposalResponse getProposalResult()                     { return proposalResult; }
    public void setProposalResult(ProposalResponse r)               { this.proposalResult = r; }
}
