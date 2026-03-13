package com.insurance.newbusiness.journey;

import com.insurance.newbusiness.exception.JourneyStageException;
import com.insurance.newbusiness.integration.client.*;
import com.insurance.newbusiness.integration.model.eligibility.EligibilityRequest;
import com.insurance.newbusiness.integration.model.scoring.ScoringRequest;
import com.insurance.newbusiness.integration.model.medical.MedicalRequest;
import com.insurance.newbusiness.integration.model.kyc.KycRequest;
import com.insurance.newbusiness.integration.model.premium.PremiumRequest;
import com.insurance.newbusiness.integration.model.underwriting.UnderwritingRequest;
import com.insurance.newbusiness.integration.model.document.DocumentRequest;
import com.insurance.newbusiness.integration.model.proposal.ProposalRequest;
import com.insurance.newbusiness.mapping.ParameterMappingService;
import com.insurance.newbusiness.tracking.JourneyTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * JourneyOrchestrator — drives the full request processing journey.
 *
 * ── STAGE EXECUTION ORDER ─────────────────────────────────────────────────────
 *   1. ELIGIBILITY  — must pass before anything else runs
 *   2. SCORING      — EDC + PASA + TASA run in PARALLEL (CompletableFuture)
 *   3. MEDICAL      — uses eligibility + scoring results
 *   4. KYC          — identity verification
 *   5. PREMIUM      — uses medical risk and eligibility age
 *   6. UNDERWRITING — uses scoring + medical + premium results
 *   7. DOCUMENT     — generate policy documents
 *   8. PROPOSAL     — final proposal before PAS
 *   (PAS + reverse feed handled separately in NewBusinessService)
 *
 * ── HOW A STAGE IS BUILT ─────────────────────────────────────────────────────
 * Every stage follows this pattern:
 *
 *   Step 1 — resolveAs(): fills POJO fields from partner's stringvalN via DB mapping
 *   Step 2 — enrich():    sets fields from prior stage responses (typed, compile-safe)
 *   Step 3 — call():      invokes the downstream API with retry built-in
 *   Step 4 — store():     sets typed result on JourneyContext for later stages
 *   Step 5 — skip check:  on retry, already-succeeded stages are skipped entirely
 *
 * ── HOW RETRY RESUME WORKS ────────────────────────────────────────────────────
 * JourneyTrackingService.getSucceededApiNames() queries journey_stage_log for
 * api_names with status=SUCCESS. Any stage whose API name is in that set is skipped.
 *
 * Example — TASA_API failed, EDC + PASA succeeded:
 *   Retry: ELIGIBILITY, EDC, PASA skipped → TASA runs → journey continues from MEDICAL
 *
 * ── ADDING A NEW STAGE ────────────────────────────────────────────────────────
 * 1. Create XxxRequest and XxxResponse POJOs in integration/model/xxx/
 * 2. Create XxxApiClient in integration/client/  (copy from EligibilityApiClient)
 * 3. Add URL to application.properties: api.endpoints.xxx=...
 * 4. Add partner_field_mapping rows with target_api = XXX_API
 * 5. Add a new executeXxxStage() method below following the same pattern
 * 6. Call it in execute() in the correct sequence
 * 7. Add typed result to JourneyContext
 *
 * ── REMOVING A STAGE ──────────────────────────────────────────────────────────
 * 1. Remove or comment out the stage call in execute()
 * 2. Remove the executeXxxStage() method
 * 3. Deactivate partner_field_mapping rows (set is_active = false)
 * 4. Remove from JourneyContext if its result isn't used by other stages
 */
@Service
public class JourneyOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JourneyOrchestrator.class);

    @Autowired private ParameterMappingService  mappingService;
    @Autowired private JourneyTrackingService   trackingService;

    // ── API Clients — one per API or group ────────────────────────────────────
    @Autowired private EligibilityApiClient   eligibilityClient;
    @Autowired private ScoringApiClient       scoringClient;    // handles EDC + PASA + TASA
    @Autowired private MedicalApiClient       medicalClient;
    @Autowired private KycApiClient           kycClient;
    @Autowired private PremiumApiClient       premiumClient;
    @Autowired private UnderwritingApiClient  underwritingClient;
    @Autowired private DocumentApiClient      documentClient;
    @Autowired private ProposalApiClient      proposalClient;

    @Autowired
    @Qualifier("journeyTaskExecutor")
    private Executor taskExecutor;

    // ==========================================================================
    // execute() — called by NewBusinessService.processJourney()
    //
    // Runs all stages in order. On retry, already-succeeded stages are skipped.
    // PAS and reverse feed are NOT here — they run in NewBusinessService after
    // this method returns because PAS gives applicationNumber that must be stored.
    // ==========================================================================
    public void execute(JourneyContext context) {
        Set<String> succeeded = trackingService.getSucceededApiNames(context.getCorrelationId());
        log.info("[{}] Journey execute | alreadySucceeded={}",
                context.getCorrelationId(), succeeded);

        executeEligibilityStage(context, succeeded);
        executeScoringStage(context, succeeded);
        executeMedicalStage(context, succeeded);
        executeKycStage(context, succeeded);
        executePremiumStage(context, succeeded);
        executeUnderwritingStage(context, succeeded);
        executeDocumentStage(context, succeeded);
        executeProposalStage(context, succeeded);
    }

    // ==========================================================================
    // Stage 1 — ELIGIBILITY
    // ==========================================================================
    private void executeEligibilityStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("ELIGIBILITY_API")) {
            log.info("[{}] ELIGIBILITY_API already succeeded — skipping", context.getCorrelationId());
            // On retry: result is already stored in journey_stage_log.
            // We need the response to enrich later stages — reload from DB or cache.
            // TODO: If eligibilityResult is needed by later stages on retry,
            // add a method to reload it from journey_stage_log or a separate store.
            return;
        }

        // Step 1: resolve stringvalN → typed POJO via DB mapping
        // All fields in EligibilityRequest must have matching rows in partner_field_mapping
        // with target_api = ELIGIBILITY_API
        EligibilityRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "ELIGIBILITY_API",
                context.getRawParams(), EligibilityRequest.class);

        // Step 2: No prior stage results to enrich for this first stage

        // Step 3: Call API (has @Retryable — 3 attempts with 2s/4s backoff)
        context.setEligibilityResult(eligibilityClient.call(req, context));

        log.info("[{}] Eligibility complete | status={}",
                context.getCorrelationId(),
                context.getEligibilityResult().getStatus());
    }

    // ==========================================================================
    // Stage 2 — SCORING (EDC + PASA + TASA run in PARALLEL)
    //
    // All three run concurrently using CompletableFuture.allOf().
    // Each has its own retry — if TASA fails, EDC and PASA are not retried.
    // ==========================================================================
    private void executeScoringStage(JourneyContext context, Set<String> succeeded) {
        boolean edcDone  = succeeded.contains("EDC_API");
        boolean pasaDone = succeeded.contains("PASA_API");
        boolean tasaDone = succeeded.contains("TASA_API");

        if (edcDone && pasaDone && tasaDone) {
            log.info("[{}] All scoring APIs already succeeded — skipping", context.getCorrelationId());
            return;
        }

        // Step 1: resolve once — same ScoringRequest sent to all three
        // Add rows in partner_field_mapping with target_api = SCORING_API for shared fields
        ScoringRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "SCORING_API",
                context.getRawParams(), ScoringRequest.class);

        // Step 2: enrich with eligibility result (age, smokingFlag etc.)
        // TODO: set fields from eligibilityResult if scoring APIs need them
        // req.setSomething(context.getEligibilityResult().getSomething());

        // Step 3: fire only the APIs that haven't succeeded yet
        // CompletableFuture.runAsync runs on journeyTaskExecutor thread pool
        CompletableFuture<Void> edcFuture = edcDone
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.runAsync(() ->
                    context.setEdcResult(scoringClient.callEdc(req, context)), taskExecutor);

        CompletableFuture<Void> pasaFuture = pasaDone
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.runAsync(() ->
                    context.setPasaResult(scoringClient.callPasa(req, context)), taskExecutor);

        CompletableFuture<Void> tasaFuture = tasaDone
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.runAsync(() ->
                    context.setTasaResult(scoringClient.callTasa(req, context)), taskExecutor);

        // Block until all pending scoring APIs complete.
        // If any throws JourneyStageException, it propagates here and stops the journey.
        CompletableFuture.allOf(edcFuture, pasaFuture, tasaFuture).join();

        log.info("[{}] Scoring complete | edcStatus={} pasaGrade={} tasaRisk={}",
                context.getCorrelationId(),
                context.getEdcResult()  != null ? context.getEdcResult().getStatus()       : "skipped",
                context.getPasaResult() != null ? context.getPasaResult().getScoreGrade()  : "skipped",
                context.getTasaResult() != null ? context.getTasaResult().getRiskCategory(): "skipped");
    }

    // ==========================================================================
    // Stage 3 — MEDICAL
    // Uses eligibility and scoring results to enrich the request.
    // ==========================================================================
    private void executeMedicalStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("MEDICAL_API")) {
            log.info("[{}] MEDICAL_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        // Step 1: resolve stringvalN → MedicalRequest (firstName, dob, panNumber, sumAssured etc.)
        MedicalRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "MEDICAL_API",
                context.getRawParams(), MedicalRequest.class);

        // Step 2: enrich from prior stage results
        // These fields are NOT in partner_field_mapping — partner doesn't send them.
        // They are computed outputs from prior APIs, set here explicitly.
        if (context.getEligibilityResult() != null) {
            req.setEligibilityId(context.getEligibilityResult().getEligibilityId());
            req.setAgeAtEntry(context.getEligibilityResult().getAgeAtEntry());
        }
        if (context.getEdcResult() != null) {
            req.setCreditScore(context.getEdcResult().getCreditScore());
        }

        // Step 3: call API
        context.setMedicalResult(medicalClient.call(req, context));

        log.info("[{}] Medical complete | status={} score={}",
                context.getCorrelationId(),
                context.getMedicalResult().getStatus(),
                context.getMedicalResult().getMedicalScore());
    }

    // ==========================================================================
    // Stage 4 — KYC
    // ==========================================================================
    private void executeKycStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("KYC_API")) {
            log.info("[{}] KYC_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        KycRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "KYC_API",
                context.getRawParams(), KycRequest.class);

        // TODO: Enrich from prior stages if KYC_API needs any prior results
        // req.setEligibilityId(context.getEligibilityResult().getEligibilityId());

        context.setKycResult(kycClient.call(req, context));

        log.info("[{}] KYC complete | status={}", context.getCorrelationId(),
                context.getKycResult().getStatus());
    }

    // ==========================================================================
    // Stage 5 — PREMIUM CALCULATION
    // Uses medical risk and eligibility age to calculate final premium.
    // ==========================================================================
    private void executePremiumStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("PREMIUM_API")) {
            log.info("[{}] PREMIUM_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        PremiumRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "PREMIUM_CALC_API",
                context.getRawParams(), PremiumRequest.class);

        // Enrich — these fields come from prior API responses, not from stringvalN
        if (context.getEligibilityResult() != null) {
            req.setAgeAtEntry(context.getEligibilityResult().getAgeAtEntry());
        }
        if (context.getMedicalResult() != null) {
            req.setRiskCategory(context.getMedicalResult().getRiskCategory());
            req.setLoadingFactor(context.getMedicalResult().getLoadingFactor());
        }

        context.setPremiumResult(premiumClient.call(req, context));

        log.info("[{}] Premium complete | premium={} frequency={}",
                context.getCorrelationId(),
                context.getPremiumResult().getCalculatedPremium(),
                context.getPremiumResult().getFrequency());
    }

    // ==========================================================================
    // Stage 6 — UNDERWRITING
    // Aggregates all scoring + medical + premium data for final UW decision.
    // ==========================================================================
    private void executeUnderwritingStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("UNDERWRITING_API")) {
            log.info("[{}] UNDERWRITING_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        UnderwritingRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "UNDERWRITING_API",
                context.getRawParams(), UnderwritingRequest.class);

        // Enrich with all scoring results
        if (context.getEdcResult() != null)  req.setCreditScore(context.getEdcResult().getCreditScore());
        if (context.getPasaResult() != null) req.setPasaScore(context.getPasaResult().getPasaScore());
        if (context.getTasaResult() != null) req.setTasaRiskCategory(context.getTasaResult().getRiskCategory());

        // Enrich with medical results
        if (context.getMedicalResult() != null) {
            req.setMedicalScore(context.getMedicalResult().getMedicalScore());
            req.setMedicalRiskCategory(context.getMedicalResult().getRiskCategory());
            req.setLoadingFactor(context.getMedicalResult().getLoadingFactor());
        }

        context.setUnderwritingResult(underwritingClient.call(req, context));

        // Underwriting decline stops the journey — no point continuing to document/proposal
        if ("DECLINED".equalsIgnoreCase(context.getUnderwritingResult().getDecision())) {
            log.warn("[{}] Underwriting DECLINED — stopping journey", context.getCorrelationId());
            throw new JourneyStageException("UNDERWRITING_STAGE",
                    "Application declined by underwriting: " + context.getUnderwritingResult().getDecisionCode());
        }

        log.info("[{}] Underwriting complete | decision={}",
                context.getCorrelationId(), context.getUnderwritingResult().getDecision());
    }

    // ==========================================================================
    // Stage 7 — DOCUMENT GENERATION
    // ==========================================================================
    private void executeDocumentStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("DOCUMENT_API")) {
            log.info("[{}] DOCUMENT_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        DocumentRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "DOCUMENT_API",
                context.getRawParams(), DocumentRequest.class);

        // TODO: enrich with any fields from prior stages needed by DOCUMENT_API
        req.setCorrelationId(context.getCorrelationId());

        context.setDocumentResult(documentClient.call(req, context));

        log.info("[{}] Document complete | documentId={}",
                context.getCorrelationId(),
                context.getDocumentResult().getDocumentId());
    }

    // ==========================================================================
    // Stage 8 — PROPOSAL SUBMISSION (final before PAS)
    // Aggregates premium + underwriting + document data.
    // ==========================================================================
    private void executeProposalStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("PROPOSAL_API")) {
            log.info("[{}] PROPOSAL_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        ProposalRequest req = mappingService.resolveAs(
                context.getPartnerCode(), "PROPOSAL_SUBMIT_API",
                context.getRawParams(), ProposalRequest.class);

        // Enrich with results from all prior stages
        if (context.getPremiumResult() != null) {
            req.setCalculatedPremium(context.getPremiumResult().getCalculatedPremium());
            req.setFrequency(context.getPremiumResult().getFrequency());
        }
        if (context.getUnderwritingResult() != null) {
            req.setUnderwritingDecision(context.getUnderwritingResult().getDecision());
        }
        if (context.getDocumentResult() != null) {
            req.setDocumentId(context.getDocumentResult().getDocumentId());
        }

        context.setProposalResult(proposalClient.call(req, context));

        log.info("[{}] Proposal complete | proposalNumber={}",
                context.getCorrelationId(),
                context.getProposalResult().getProposalNumber());
    }
}
