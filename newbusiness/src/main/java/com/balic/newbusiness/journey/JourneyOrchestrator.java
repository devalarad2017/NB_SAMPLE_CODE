package com.balic.newbusiness.journey;

import com.balic.newbusiness.integration.model.pennydrop.PennyDropRequest;
import com.balic.newbusiness.integration.model.proposal.ProposalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.balic.newbusiness.integration.client.*;
import com.balic.newbusiness.integration.model.auw.AuwRequest;
import com.balic.newbusiness.integration.model.bi.BiRequest;
import com.balic.newbusiness.integration.model.bi.BiRequest.BasicInfo;
import com.balic.newbusiness.integration.model.bi.BiRequest.InputOptions;
import com.balic.newbusiness.integration.model.bi.BiRequest.Funds;
import com.balic.newbusiness.integration.model.bi.BiRequest.Riders;
import com.balic.newbusiness.integration.model.cibil.CibilRequest;
import com.balic.newbusiness.integration.model.dcs.DcsRequest;
import com.balic.newbusiness.integration.model.edc.EdcRequest;
import com.balic.newbusiness.integration.model.ekyc.EkycRequest;
import com.balic.newbusiness.integration.model.fes.FesRequest;
import com.balic.newbusiness.integration.model.fes.FesRequest.DataArrayDetail;
import com.balic.newbusiness.integration.model.mrs.MrsRequest;
import com.balic.newbusiness.integration.model.pan.PanRequest;
import com.balic.newbusiness.integration.model.pan.PanRequest.PanCardList;
import com.balic.newbusiness.integration.model.pan.PanResponse.PanCardDto;
import com.balic.newbusiness.integration.model.ucs.UcsApiRequest;
import com.balic.newbusiness.integration.model.ucs.UcsApiRequest.ProposalDetail;
import com.balic.newbusiness.mapping.MappingService;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * JourneyOrchestrator — drives the full request processing journey.
 *
 * ── STAGE EXECUTION ORDER ─────────────────────────────────────────────────────
 *   1. CIBIL  — must pass before anything else runs
 *   2. UCS          — identity verification
 *   (PAS + reverse feed handled separately in NewBusinessService)
 *
 * ── HOW A STAGE IS BUILT ─────────────────────────────────────────────────────
 * Every stage follows this pattern:
 *
 *   Step 1 — resolveAs(): fills POJO fields from partner's stringvalN via property file
 *   Step 2 — enrich():    sets fields from prior stage responses (typed, compile-safe)
 *   Step 3 — call():      invokes the downstream API with retry built-in
 *   Step 4 — store():     sets typed result on JourneyContext for later stages
 *   Step 5 — skip check:  on retry, already-succeeded stages are skipped entirely
 *
 * ── HOW RETRY RESUME WORKS ────────────────────────────────────────────────────
 * JourneyTrackingService.getSucceededApiNames() queries journey_stage_log for
 * api_names with status=SUCCESS. Any stage whose API name is in that set is skipped.
 *
 * ── ADDING A NEW STAGE ────────────────────────────────────────────────────────
 * 1. Create XxxRequest and XxxResponse POJOs in integration/model/xxx/
 * 2. Create XxxApiClient in integration/client/  (copy from UCSApiClient)
 * 3. Add URL to application.properties: api.endpoints.xxx=...
 * 4. Create mapping/default-XXX_API.properties with stringvalN=fieldName entries
 * 5. Add a new executeXxxStage() method below following the same pattern
 * 6. Call it in execute() in the correct sequence
 * 7. Add typed result to JourneyContext
 *
 * ── REMOVING A STAGE ──────────────────────────────────────────────────────────
 * 1. Remove or comment out the stage call in execute()
 * 2. Remove the executeXxxStage() method
 * 3. Delete or leave the .properties file (harmless if unused)
 * 4. Remove from JourneyContext if its result isn't used by other stages
 */
@Service
public class JourneyOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JourneyOrchestrator.class);

    @Autowired private MappingService            mappingService;
    @Autowired private JourneyTrackingService   trackingService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private BiProductImpl biProductImpl;

    // ── API Clients — one per API or group ────────────────────────────────────
    @Autowired private CibilApiClient        cibilClient;       // CIBIL credit check — first stage
    @Autowired private UcsApiClient			 ucsClient;
    @Autowired private PanApiClient          panClient;
    @Autowired private BiApiClient			 biClient;
    @Autowired private EkycApiClient		 ekycClient;
    @Autowired private EdcApiClient			 edcClient;
    @Autowired private DcsApiClient          dcsClient;
    @Autowired private AuwApiClient          auwClient;
    @Autowired private MrsApiClient          mrsClient;
    @Autowired private FesApiClient          fesClient;
    @Autowired private PennyDropApiClient    pennyDropClient;
    @Autowired private ProposalApiClient     proposalClient;

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

    //Themporatry created to test BI for all the products
    public void executeBi(JourneyContext context) {
        Set<String> succeeded = new HashSet<>(Arrays.asList("A", "B", "C"));
        executeBiStage(context, succeeded);
    }
    public void execute(JourneyContext context) {
        Set<String> succeeded = trackingService.getSucceededApiNames(context.getCorrelationId());
        log.info("[{}] Journey execute | alreadySucceeded={}",
                context.getCorrelationId(), succeeded);
        
        Map<String, String> params = context.getRawParams();
        String ekycFlag =  params.get("obj3.stringval107");

        executeCibilStage(context, succeeded);
        executeUcsStage(context, succeeded);
        executePanStage(context, succeeded);
        executeBiStage(context, succeeded);
        if("Y".equalsIgnoreCase(ekycFlag)) {
            executeEkycStage(context, succeeded);
        }
        executeEdcStage(context, succeeded);
        executeDcsStage(context, succeeded);
        executeAuwStage(context, succeeded);
        executeMrsStage(context, succeeded);
        executeFesStage(context, succeeded);
        executeProposalStage(context,  succeeded);
//        executePennyDropStage(context, succeeded);
    }

    // ==========================================================================
    // Stage 1 — CIBIL (Credit Bureau Check)
    //
    // Built manually because the request has nested BasicDetailsDto structure.
    // Property file mapping won't handle nested objects — direct assignment is
    // cleaner and matches the actual CIBIL API contract.
    // ==========================================================================
    private void executeCibilStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("CIBIL_API")) {
            log.info("[{}] CIBIL_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        // Build CibilRequest manually — nested structure, not flat mapping
        CibilRequest req = new CibilRequest();
        req.setAppName(params.get("obj1.stringval1"));
        req.setNbId(23936); // TEMP hardcoded
        req.setApplicationNumber(params.get("obj1.stringval6"));
        req.setIsCibilBypass(params.get("obj4.stringval76"));
        req.setIpPhSame(params.get("obj1.stringval9"));
        req.setIsMinorLife(null);
        req.setIsWop(null);

        // Build nested BasicDetailsDto
        CibilRequest.BasicDetailsDto details = new CibilRequest.BasicDetailsDto();
        details.setApplicantFirstName(params.get("obj1.stringval13"));
        details.setApplicantMiddleName(params.get("obj1.stringval14"));
        details.setApplicantLastName(params.get("obj1.stringval15"));
        details.setDateOfBirth(params.get("obj1.stringval16"));
        details.setGender(params.get("obj1.stringval18"));
        details.setMobileNo(params.get("obj1.stringval19"));
        details.setEmailAddress(params.get("obj1.stringval20"));
        details.setIdNumber(params.get("obj1.stringval122"));       // IP PAN number
        details.setIdType("01");                                     // 01 = PAN
        details.setIpPhType("PH");
        details.setResidenceType("01");
        details.setAddressType("02");
        details.setAddressLine1(params.get("obj1.stringval56"));
        details.setAddressLine2(params.get("obj1.stringval57"));
        details.setAddressLine3(params.get("obj1.stringval58"));
        details.setCity(params.get("obj1.stringval61"));
        details.setPinCode(params.get("obj1.stringval55"));
        details.setStateCode(params.get("obj1.stringval62"));
        details.setGstStateCode(params.get("obj1.stringval62"));
        
        // Sum assured — parse to Long safely
        String amountStr = params.get("obj1.stringval40");
        if (amountStr != null && !amountStr.trim().isEmpty()) {
            try {
                details.setAmount(Long.parseLong(amountStr.trim()));
            } catch (NumberFormatException e) {
                log.warn("[{}] Could not parse amount '{}' — skipping", context.getCorrelationId(), amountStr);
            }
        }

        req.setBasicDetailsDto(Collections.singletonList(details));

        // Call CIBIL API
        context.setCibilResult(cibilClient.call(req, context));

        log.info("[{}] CIBIL complete | score={} status={}",
                context.getCorrelationId(),
                context.getCibilResult()[0].getCibilId(),
                context.getCibilResult()[0].getCibilStatus());
    }
    
    // ==========================================================================
    // Stage 1 — CIBIL (Credit Bureau Check)
    //
    // Built manually because the request has nested BasicDetailsDto structure.
    // Property file mapping won't handle nested objects — direct assignment is
    // cleaner and matches the actual CIBIL API contract.
    // ==========================================================================
    private void executeUcsStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("UCS_API")) {
            log.info("[{}] UCS_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        // Build CibilRequest manually — nested structure, not flat mapping
        UcsApiRequest req = new UcsApiRequest();
        req.setRequestSource(params.get("obj1.stringval1"));
        req.setProposalNumber(params.get("obj1.stringval6"));
        
        
        UcsApiRequest.CustomerDetails custDetails = new UcsApiRequest.CustomerDetails();
        custDetails.setApplicationNo(params.get("obj1.stringval6"));
        custDetails.setAadhaarNo(params.get("obj1.stringval117"));
        custDetails.setPolicyRef(params.get("obj1.stringval6"));
        custDetails.setIpType(params.get("list4.stringval5"));
        custDetails.setPartId(params.get("obj2.stringval82"));
        custDetails.setFirstName(params.get("obj1.stringval13"));
        custDetails.setMiddleName(params.get("obj1.stringval14"));
        custDetails.setLastName(params.get("obj1.stringval15"));
        custDetails.setGender(params.get("obj1.stringval18"));
//        custDetails.setDob(params.get("obj1.stringval16"));    //TODO 21-03-1986 - expected format 
        custDetails.setDob("21-03-1986"); 						//TODO
        custDetails.setDob(convertDobFormat(params.get("obj1.stringval16")));
        
        custDetails.setAddressLine1(params.get("obj1.stringval56"));
        custDetails.setAddressLine2(params.get("obj1.stringval57"));
        custDetails.setAddressLine3(params.get("obj1.stringval58"));
        custDetails.setCity(params.get("obj1.stringval61"));
        custDetails.setState(params.get("obj1.stringval62"));
        custDetails.setCountry(params.get("obj1.stringval52"));
        custDetails.setPincode(params.get("obj1.stringval55"));
        custDetails.setMobile1(params.get("obj1.stringval19"));
        custDetails.setMobile2(params.get("obj3.stringval116"));
        custDetails.setTelephone1(params.get("obj3.stringval116"));
        custDetails.setTelephone2("");
        custDetails.setEmail(params.get("obj1.stringval20"));
        custDetails.setPanNumber(params.get("obj1.stringval122"));
        custDetails.setFatherName(params.get("obj1.stringval102"));
        custDetails.setMotherName(params.get("obj3.stringval110"));
        custDetails.setBankAccount1(params.get("obj2.stringval32"));
        custDetails.setBankAccount2("");
        custDetails.setCkycId("");
        custDetails.setVirtualId("");
        custDetails.setPassportId("");
        custDetails.setVoterId("");
        custDetails.setCreateDate("");
        custDetails.setCreateUser("");
        req.setCustomerDetails(custDetails);
        
        ProposalDetail proposalDetails = new ProposalDetail();
        proposalDetails.setPolicyNumber(params.get("obj1.stringval6"));
        proposalDetails.setCustomerId(params.get("obj2.stringval82"));
        proposalDetails.setBenefitCode("L155A01"); //TODO find details.
        
        String premiumStr = params.get("obj1.stringval37");
        String frequencyStr = params.get("obj1.stringval39");
        double annualPremium = 0.0;
        if (premiumStr != null && !premiumStr.isEmpty() && frequencyStr != null && !premiumStr.isEmpty()) {
            double premium = Double.parseDouble(premiumStr);
            double frequency = Double.parseDouble(frequencyStr);
            annualPremium = premium * frequency;
        }
        
        proposalDetails.setAnnualPremium(String.valueOf(annualPremium));
        proposalDetails.setPpt(params.get("obj1.stringval36"));
        proposalDetails.setTotalInstallmentPremium(String.valueOf(annualPremium));
        
        String saValue1 = params.get("obj1.stringval40");
        String saValue2 = params.get("list1.stringval2");
        double totalBenefitSa = 0.0;
        double val1 = (saValue1 != null && !saValue1.isEmpty()) ? Double.parseDouble(saValue1) : 0.0;
        double val2 = (saValue2 != null && !saValue2.isEmpty()) ? Double.parseDouble(saValue2) : 0.0;
        totalBenefitSa = val1 + val2;
        proposalDetails.setBenefitSa(String.valueOf(totalBenefitSa));
        proposalDetails.setFundValue("0");		//TODO
        proposalDetails.setSinglePremium(params.get("obj1.stringval37"));
        proposalDetails.setTerm(params.get("obj1.stringval36"));
        proposalDetails.setPaymentMode("1"); //TODO LOV's  - obj5.stringval13
        proposalDetails.setBenefitInstallmentPremium(params.get("obj1.stringval37"));
        proposalDetails.setBenefitStatus("Proposal");
        proposalDetails.setBenefitTerm(params.get("obj1.stringval35"));
        
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        proposalDetails.setDoef(currentDate);
        proposalDetails.setGmb("0");   //TODO  find it's value by default = "0"
        proposalDetails.setInstallmentsPaid("0");
        
        ArrayList<ProposalDetail> proposalLst = new ArrayList<ProposalDetail>();
        proposalLst.add(proposalDetails);
        req.setProposalDetails(proposalLst);
        
        //        req.setAppName(params.get("obj1.stringval1"));
//        details.setGstStateCode(params.get("obj1.stringval62"));
 //       req.setBasicDetailsDto(Collections.singletonList(details));

        // Call UCS API
        context.setUcsResult(ucsClient.call(req, context));

        log.info("[{}] UCS complete | score={} status={}",
                context.getCorrelationId(),
                context.getUcsResult().getTranxResponse(),
                context.getUcsResult().getTranxStatus());
    }
    
    private void executePanStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("PAN_API")) {
            log.info("[{}] PAN_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        PanRequest req = new PanRequest();
        req.setRequestSource(params.get("obj1.stringval1"));
        req.setRecordsCount("1");
        req.setSourceType("BALIC");
        req.setUniqueId(params.get("obj1.stringval6"));
        req.setUniqueIdentifier("policy_Number");
        req.setUniqueKey(params.get("obj1.stringval1"));
        req.setModuleName(params.get("obj1.stringval1"));
        req.setPartnerId("");
        
        String fullName = (params.get("obj1.stringval13") != null ? params.get("obj1.stringval13") : "") + " " +
                (params.get("obj1.stringval14") != null ? params.get("obj1.stringval14") : "") + " " +
                (params.get("obj1.stringval15") != null ? params.get("obj1.stringval15") : "");
        
        PanCardList panCardList = new PanCardList();
        panCardList.setStringval1(params.get("obj1.stringval122"));
        panCardList.setStringval2(fullName.trim().replaceAll("\\s+", " "));
        panCardList.setStringval3(params.get("obj1.stringval16"));
        
        req.setPancardList(Collections.singletonList(panCardList));
        
     // Call PAN API
        context.setPanResult(panClient.call(req, context));

        log.info("[{}] PAN complete | score={} status={}",
                context.getCorrelationId(),
                context.getPanResult().getStatus(),
                context.getPanResult().getStatusCode());
    }
    
    
    private void executeBiStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("BI_API")) {
            log.info("[{}] BI_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        String policyOption = "";
        String channel = biProductImpl.channel(params);
        String productId = params.get("obj1.stringval10");

        String name1 = "", name2, proposerName, proposerAge, proposerDob, proposerGender;
        String sameProposer = params.get("obj1.stringval9");

        if (Arrays.asList("321", "329", "331").contains(productId) && Arrays.asList("Y", "0").contains(sameProposer)) {
            String firstName = params.get("obj1.stringval13");
            String middleName = params.get("obj1.stringval14");
            String lastName = params.get("obj1.stringval15");
            name1 = buildFullName(firstName, middleName, lastName);
            proposerName = name1;
            proposerAge = params.get("obj1.stringval17");
            proposerDob = params.get("obj1.stringval16");
            proposerGender = params.get("obj1.stringval18");
        } else {
            String firstName = params.get("obj1.stringval22");
            String middleName = params.get("obj1.stringval23");
            String lastName = params.get("obj1.stringval24");
            name2 = buildFullName(firstName, middleName, lastName);
            if (name2.trim().isEmpty()) {
                proposerName = name1;
            }else {
                proposerName = name2;
            }
            proposerAge = (params.get("obj1.stringval26") != null && !params.get("obj1.stringval26").trim().isEmpty()) ? params.get("obj1.stringval26") : params.get("obj1.stringval17");
            proposerDob = (params.get("obj1.stringval25") != null && !params.get("obj1.stringval25").trim().isEmpty()) ? params.get("obj1.stringval25") : params.get("obj1.stringval16");
            proposerGender = (params.get("obj1.stringval27") != null && !params.get("obj1.stringval27").trim().isEmpty()) ? params.get("obj1.stringval27") : params.get("obj1.stringval18");
        }

        BiRequest req = new BiRequest();
        req.setQuoteId(params.getOrDefault("obj1.stringval6", ""));
        req.setApplnNo(params.getOrDefault("obj1.stringval6", ""));
        req.setUserId(params.getOrDefault("obj1.stringval5", ""));
        req.setIpAddress(params.getOrDefault("obj1.stringval3", ""));
        req.setModule(params.getOrDefault("obj1.stringval1", ""));
        
        BasicInfo basicInfo = new BasicInfo();
        basicInfo.setApiKey("");
        basicInfo.setLiName(proposerName);
        basicInfo.setLiEntryAge(proposerAge);
        basicInfo.setLiDob(proposerDob);
        basicInfo.setLiGender(proposerGender);
        basicInfo.setLiState(params.get("obj1.stringval62"));
        basicInfo.setProposerName(proposerName);
        basicInfo.setProposerAge(proposerAge);
        basicInfo.setProposerDob(proposerDob);
        basicInfo.setProposerGender(proposerGender);

        String kfd = "HIN".equals(params.get("obj1.stringval34")) ? "H" : "E";
        basicInfo.setKfd(kfd);

        basicInfo.setPincode(params.getOrDefault("obj1.stringval55", ""));

        basicInfo.setSameproposer(("Y".equals(sameProposer) || "0".equals(sameProposer)) ? "0" : "1");

        basicInfo.setCompanyState("");
        basicInfo.setGstin("");
        basicInfo.setGstinNumber("");
        basicInfo.setInputMode(params.getOrDefault("obj1.stringval39", ""));			//LOV'S
        basicInfo.setPrId(params.getOrDefault("obj1.stringval10", ""));
        basicInfo.setPrPt(params.get("obj1.stringval35"));
        basicInfo.setPrPpt(params.get("obj1.stringval36"));
        basicInfo.setAgentId(params.get("obj1.stringval4"));
        basicInfo.setLiMobileNo(params.get("obj1.stringval19"));
        basicInfo.setLiEmailId(params.get("obj1.stringval20"));
        basicInfo.setPrAnnPrem("");
        basicInfo.setPrMi("");
        basicInfo.setPrSa(params.get("obj1.stringval40"));
        basicInfo.setPrSamf(params.get("obj1.stringval38"));
        basicInfo.setPrModalPrem(params.get("obj1.stringval37"));
        basicInfo.setLiSmoke(params.get("obj1.stringval8"));
        basicInfo.setExistingCustomer(params.get("obj1.stringval44"));
        basicInfo.setPrChannel(channel);

        String matBen = "";
        if ("309".equals(productId) || "319".equals(productId)) {
            matBen = params.getOrDefault("obj6.stringval24", "");
        }
        basicInfo.setMatBen(matBen);
        basicInfo.setPartnerDisc(params.getOrDefault("obj6.stringval84", ""));
        
        req.setBasicInfo(basicInfo);

//        BiProductConfigLoader configLoader = new BiProductConfigLoader();
//
//        // Input Options
//        List<Map<String, String>> optionMappings = configLoader.getSection(productId, "inputOptions");
//        List<InputOptions> inputOptions = new ArrayList<>();
//        for (Map<String, String> mapping : optionMappings) {
//            InputOptions inputOption = new InputOptions();
//            inputOption.setOptionId(mapping.get("optionId"));
//            String paramKey = mapping.get("param");
//            inputOption.setOptionValue(params.get(paramKey));
//            inputOptions.add(inputOption);
//        }

        // Add policy option for each rider
        List<Integer> indices1 = params.keySet().stream()
                .filter(k -> k.startsWith("list1[") && k.contains(".stringval1"))
                .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                .distinct()
                .sorted()
                .collect(Collectors.toList());


        List<InputOptions> inputOptions = new ArrayList<>(biProductImpl.getInputOptions(params));

        List<Funds> funds =  new ArrayList<>(biProductImpl.getFunds(params));

        List<Riders> riders = new ArrayList<>(biProductImpl.getRiders(params));

        req.setInputOptions(inputOptions);
        req.setFunds(funds);
        req.setRiders(riders);
        
        // Call Bi API
        context.setBiResult(biClient.call(req, context));

        log.info("[{}] BI complete | score={} status={}",
                context.getCorrelationId(),
                context.getBiResult().getStatus(),
                context.getBiResult().getMessage());
    }

    private void executeEdcStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("EDC_API")) {
            log.info("[{}] EDC_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        EdcRequest req = new EdcRequest();
        
        req.setAppNo(params.get("obj1.stringval6"));
        req.setIsBsoApplicable("N");
        req.setNbId(null);
        req.setSource(params.get("obj1.stringval1"));
        
        EdcRequest.CibilRequestDto cibilRequest = new EdcRequest.CibilRequestDto();
        cibilRequest.setIpPhSame(params.get("obj1.stringval9"));
        
        String ageStr = params.get("obj1.stringval17");
        boolean isMinor = false;
        if (ageStr != null && !ageStr.isEmpty()) {
            try {
                int age = Integer.parseInt(ageStr);
                // Set to "Y" if age is less than 18, otherwise "N"
                isMinor = (age < 18) ? true : false;
            } catch (NumberFormatException e) {
            	isMinor = false; 
            }
        }
        
        cibilRequest.setMinorLife(isMinor);
        cibilRequest.setWop(false);
        req.setCibilRequestDto(cibilRequest);
        
        EdcRequest.BasicDetailsDto details = new EdcRequest.BasicDetailsDto();
        details.setApplicantFirstName(params.get("obj1.stringval13"));
        details.setApplicantMiddleName(params.get("obj1.stringval14"));
        details.setApplicantLastName(params.get("obj1.stringval15"));
        details.setDateOfBirth(params.get("obj1.stringval16"));
        details.setGender(params.get("obj1.stringval18"));      //LOV
        details.setMobileNo(params.get("obj1.stringval19"));
        details.setEmailAddress(params.get("obj1.stringval20"));
        details.setIdNumber(params.get("obj1.stringval122"));       // PAN number
        details.setIdType("01");                                     // 01 = PAN
        details.setIpPhType(params.get("obj1.stringval19"));
        details.setResidenceType("01");
        details.setAddressType("02");
        details.setAddressLine1(params.get("obj1.stringval56"));
        details.setAddressLine2(params.get("obj1.stringval57"));
        details.setAddressLine3(params.get("obj1.stringval58"));
        details.setCity(params.get("obj1.stringval61"));
        details.setPinCode(params.get("obj1.stringval55"));
        details.setStateCode(params.get("obj1.stringval62"));
        details.setGstStateCode(params.get("obj1.stringval62"));
        details.setAmount((params.get("obj1.stringval37")) != null && !(params.get("obj1.stringval37")).trim().isEmpty() ? Long.parseLong((params.get("obj1.stringval37")).trim()) : null);
        
        req.getCibilRequestDto().setBasicDetailsDto(Collections.singletonList(details));

        EdcRequest.DrcRequest drcReq = new EdcRequest.DrcRequest();
        drcReq.setSource("DIGIBANCA");
        drcReq.setSalesChannel("-999");
        drcReq.setRiskCommencementDate("-999");
        drcReq.setLifeAssuredClientId("-999");
        drcReq.setLifeAssuredIdProof("-999");
        drcReq.setPolicyOwnerOccupation(null);
        drcReq.setPolicyOwnerQualification(null);
        drcReq.setBasePlanSumAssured(params.get("obj1.stringval40"));
        drcReq.setPolicyOwnerPincode(params.get("obj1.stringval79"));
        drcReq.setPolicyOwnerCountry(params.get("obj1.stringval76"));
        drcReq.setPolicyOwnerIndustry(params.get("obj1.stringval92"));
        drcReq.setPolicyIssueDate("-999");
        drcReq.setProductName(params.get("obj1.stringval30"));
        drcReq.setPolicyOwnerGender(params.get("obj1.stringval27"));
        drcReq.setAgentType("-999");
        drcReq.setPolicyOwnerClientId("-999");
        drcReq.setLifeAssuredQualification(params.get("obj1.stringval129"));
        drcReq.setPolicyOwnerAgeProof(params.get("obj2.stringval145"));
        drcReq.setPolicyOwnerIdProof(params.get("obj2.stringval107"));
        drcReq.setPolicyOwnerAge(params.get("obj2.stringval12"));
        drcReq.setPremiumPaymentMode(params.get("obj5.stringval13"));
        drcReq.setLifeAssuredDob(params.get("obj1.stringval16"));
        drcReq.setLifeAssuredState(params.get("obj1.stringval72"));
        drcReq.setLineOfBusiness("-999");
        drcReq.setProductCode(params.get("obj1.stringval10"));
        drcReq.setContractNo("-999");
        drcReq.setLifeAssuredNationality(params.get("obj1.stringval51"));
        drcReq.setBranchLocation("-999");
        drcReq.setAdvisorCode("-999");
        drcReq.setLifeAssuredGender(params.get("obj1.stringval18"));
        drcReq.setSumUnderConsideration(params.get("obj1.stringval40"));
        drcReq.setPolicyOwnerMaritalStatus(params.get("obj1.stringval108"));
        drcReq.setPolicyOwnerDob(params.get("obj1.stringval25"));
        drcReq.setZoneLocation("-999");
        drcReq.setLifeAssuredCity(params.get("obj1.stringval71"));
        drcReq.setLifeAssuredIndustry(params.get("obj3.stringval98"));
        drcReq.setAgentClass("-999");
        drcReq.setBillingFrequency(params.get("obj1.stringval39"));
        drcReq.setAnnualPremium("-999");
        drcReq.setPolicyOwnerCity(params.get("obj1.stringval85"));
        drcReq.setAgentCode(params.get("obj1.stringval4"));
        drcReq.setSmokerFlag(params.get("obj3.stringval1"));
        drcReq.setLifeAssuredCountry(params.get("obj1.stringval52"));
        drcReq.setLifeAssuredAge(params.get("obj1.stringval17"));
        drcReq.setPolicyOwnerNationality(params.get("obj1.stringval75"));
        drcReq.setOnlineOfflineFlag("-999");
        drcReq.setPolicyOwnerAnnualIncome(params.get("obj1.stringval142"));
        drcReq.setNomineeRelation(params.get("obj2.stringval18"));
        drcReq.setLifeAssuredOccupation(params.get("obj1.stringval131"));
        drcReq.setLifeAssuredMaritalStatus(params.get("obj1.stringval99"));
        drcReq.setApplicationNo(params.get("obj1.stringval6"));
        drcReq.setLifeAssuredPincode(params.get("obj1.stringval55"));
        drcReq.setPolicyOwnerIncomeProof(params.get("obj1.stringval144"));
        drcReq.setLifeAssuredAnnualIncome(params.get("obj1.stringval130"));
        drcReq.setPremiumPaymentTerm(params.get("obj1.stringval36"));
        drcReq.setLifeAssuredIncomeProof(params.get("obj1.stringval132"));
        drcReq.setLifeAssuredAgeProof(params.get("obj2.stringval145"));
        drcReq.setPolicyTerm(params.get("obj1.stringval35"));
        req.setDrcRequest(drcReq);
        
        EdcRequest.AdditionalInput additionalInput = new EdcRequest.AdditionalInput();
        additionalInput.setLifeAssuredName(params.get("obj1.stringval13"));
        additionalInput.setLifeAssuredEmailId(params.get("obj1.stringval20"));
        additionalInput.setLifeAssuredContactNo(params.get("obj1.stringval19"));
        additionalInput.setLifeAssuredPanNo(params.get("obj1.stringval122"));
        req.getDrcRequest().setAdditionalInput(additionalInput);
        
        EdcRequest.EdcData edcData = new EdcRequest.EdcData();
        edcData.setIsSuperWoman(params.get("obj3.stringval89"));
        edcData.setContractId("-999");
        
        String currentDateTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        
        edcData.setDateOfCommencement(currentDateTime);
        edcData.setEmailValidity("-999");
        edcData.setFormFillingMode(params.get("obj1.stringval1"));
        edcData.setCallSource(params.get("obj1.stringval1"));
        edcData.setIpEmploymentStatus("P"); //TODO Find values
        edcData.setIrdaChannel("-999");
        edcData.setLifeStage("-999");
        edcData.setNegativeAgencyPinCode("SELF"); //TODO find correct value
        edcData.setNegativePinCode("N");
        
        String fullName = (params.get("obj1.stringval13") != null ? params.get("obj1.stringval13") : "") + " " +
                (params.get("obj1.stringval14") != null ? params.get("obj1.stringval14") : "") + " " +
                (params.get("obj1.stringval15") != null ? params.get("obj1.stringval15") : "");
        
        if (context != null && 
        	    context.getPanResult() != null && 
        	    context.getPanResult().getPancardList() != null && 
        	    !context.getPanResult().getPancardList().isEmpty()) {
        	
        	PanCardDto panData = context.getPanResult().getPancardList().get(0);
        	
        	String nsdlPanResponse = fullName + "|" + 
        			((panData.getStringval1() != null) ? panData.getStringval1() : "") + "|" + 
        			((panData.getStringval2() != null) ? panData.getStringval2() : "") + "|" + 
        			((panData.getStringval15() != null) ? panData.getStringval15() : "") + "|" + 
        			((panData.getStringval13() != null) ? panData.getStringval13() : "") + "|" + 
        			((panData.getStringval16() != null) ? panData.getStringval16() : "");
            edcData.setNsdlPanResponse(nsdlPanResponse);

        }
        
        edcData.setPhEmploymentStatus("P");
        if (context != null && context.getBiResult() != null) {
            edcData.setProductUIN(context.getBiResult().getProductUIN());
        }
        edcData.setStmStartDate("-999");
        edcData.setDeviceCode("-999");
        edcData.setDeviceOS("-999");
        edcData.setDeviceType("DIGIBANCA");
        edcData.setPhoneMake("-999");
        edcData.setAppNo(params.get("obj1.stringval6"));
        edcData.setAgentClubStatus("-999");
        edcData.setAgentCode(params.get("obj1.stringval4"));
        edcData.setBranchCode("-999");
        edcData.setCkycMatch("N|N|Manual Journey Selected");   	//TODO 
        edcData.setEkycMatch("Y|Y|P");           //obj3.stringval107 flag  //TODO
        edcData.setFpPayMode(params.get("obj2.stringval23"));
        edcData.setProposalNo(params.get("obj5.stringval24"));
        edcData.setStmCode(params.get("obj1.stringval4"));
        edcData.setAgentStartDate("-999");
        edcData.setChannel("WEBSALES");
        edcData.setDistributionChannel("-999");
        edcData.setSubChannel1(params.get("obj5.stringval126"));
        
        String premiumStr = params.get("obj1.stringval37");
        String frequencyStr = params.get("obj1.stringval39");
        double annualPremium = 0.0;
        if (premiumStr != null && !premiumStr.isEmpty() && frequencyStr != null && !premiumStr.isEmpty()) {
            double premium = Double.parseDouble(premiumStr);
            double frequency = Double.parseDouble(frequencyStr);
            annualPremium = premium * frequency;
        }
        
        edcData.setAnnualPremium((String.valueOf(annualPremium)));
        edcData.setBookingFrequency("Quarterly");          //TODO LOV p_in_obj_1.stringval39
        edcData.setPolicyTerm(params.get("obj5.stringval35"));
        edcData.setPremiumTerm(params.get("obj5.stringval36"));
        edcData.setProductId(params.get("obj1.stringval10"));
        edcData.setIsAxisBurgundyCustomer("-999");
        edcData.setSumAssured(params.get("obj1.stringval40"));
        edcData.setCoverCode("L167A01");			//TODO
        edcData.setPhDob(params.get("obj1.stringval25"));
        edcData.setPhEducation(params.get("obj1.stringval141"));
        edcData.setPhEmail(params.get("obj1.stringval29"));
        edcData.setPhGender(params.get("obj1.stringval27"));
        edcData.setPhIncome(params.get("obj1.stringval142"));
        edcData.setPhMaritalStatus(params.get("obj1.stringval108"));
        edcData.setPhMobile(params.get("obj1.stringval28"));

        
       // edcData.setPhName(params.get("obj1.stringval22" + " " + "obj1.stringval23" + " " + "obj1.stringval24"));
        
        String phFullName = (params.get("obj1.stringval22") != null ? params.get("obj1.stringval22") : "") + " " +
                (params.get("obj1.stringval23") != null ? params.get("obj1.stringval23") : "") + " " +
                (params.get("obj1.stringval24") != null ? params.get("obj1.stringval24") : "");
        edcData.setPhName(phFullName.trim().replaceAll("\\s+", " "));
        
        edcData.setPhFacebookId("-999");
        edcData.setPhOccupation(params.get("obj1.stringval143"));
        edcData.setPhResidenceCountry(params.get("obj1.stringval76"));
        edcData.setPhNRIFlag("list9.stringval10");
        edcData.setLaFirstName(params.get("obj1.stringval13"));
        edcData.setLaMiddleName(params.get("obj1.stringval14"));
        edcData.setLaLastName(params.get("obj1.stringval15"));
        edcData.setIpDob(params.get("obj1.stringval16"));
        edcData.setIpEducation(params.get("obj1.stringval129"));
        edcData.setIpEmail(params.get("obj1.stringval20"));
        edcData.setIpGender(params.get("obj1.stringval18"));
        edcData.setIpIncome(params.get("obj1.stringval130"));
        edcData.setIpMaritalStatus(params.get("obj1.stringval99"));
        edcData.setIpMobile(params.get("obj1.stringval19"));

        String ipFullName = (params.get("obj1.stringval13") != null ? params.get("obj1.stringval13") : "") + " " +
                (params.get("obj1.stringval14") != null ? params.get("obj1.stringval14") : "") + " " +
                (params.get("obj1.stringval15") != null ? params.get("obj1.stringval15") : "");
        edcData.setIpName(ipFullName.trim().replaceAll("\\s+", " "));
        edcData.setIpOccupation(params.get("obj1.stringval131"));
        edcData.setIpResidenceCountry(params.get("obj1.stringval52"));
        edcData.setLifeGoal("-999");
        edcData.setPanNo(params.get("obj1.stringval122"));
        
        if (context != null && context.getPanResult() != null && 
        	    context.getPanResult().getPancardList() != null && 
        	    !context.getPanResult().getPancardList().isEmpty()) {
        	    
        	    String val = context.getPanResult().getPancardList().get(0).getStringval2();
        	    String status = (val != null) ? val : "";
            	edcData.setPanVerificationStatus(status);   
        	}

        edcData.setPurposeOfInvestment("-999");
        edcData.setIpNRIFlag(params.get("obj1.stringval11"));
        edcData.setCpId(params.get("obj2.stringval82"));
        edcData.setIpAddress(params.get("obj1.stringval135"));
        edcData.setIpCity(params.get("obj1.stringval61"));
        edcData.setIpCountry(params.get("obj1.stringval52"));
        edcData.setIpPinCode(params.get("obj1.stringval55"));
        edcData.setIpState(params.get("obj1.stringval62"));
        edcData.setMailingAddress(params.get("obj1.stringval135"));
        edcData.setMailingCity(params.get("obj1.stringval61"));
        edcData.setMailingPincode(params.get("obj1.stringval155"));
        edcData.setMailingState(params.get("obj1.stringval162"));
        
        String part1 = params.get("obj1.stringval56");
        String part2 = params.get("obj1.stringval57");
        String part3 = params.get("obj1.stringval58");
        String part4 = params.get("obj1.stringval59");
        
        StringBuilder phFullAddress = new StringBuilder();

        if (part1 != null && !part1.isEmpty()) phFullAddress.append(part1).append(" ");
        if (part2 != null && !part2.isEmpty()) phFullAddress.append(part2).append(" ");
        if (part3 != null && !part3.isEmpty()) phFullAddress.append(part3).append(" ");
        if (part4 != null && !part4.isEmpty()) phFullAddress.append(part4);
        
        edcData.setPhAddress(phFullAddress.toString().trim());          
        edcData.setPhCity(params.get("obj1.stringval94"));
        edcData.setPhCountry(params.get("obj1.stringval76"));
        edcData.setPhPinCode(params.get("obj1.stringval89"));
        edcData.setPhState(params.get("obj1.stringval86"));
        edcData.setNominee4Dob(params.get("list8.stringval3"));
        edcData.setNominee4Relation(params.get("list8.stringval4"));
        edcData.setNominee4Share(params.get("list8.stringval5"));
        edcData.setNomineeDob(params.get("list8.stringval3"));
        edcData.setNomineeRelation(params.get("list8.stringval4"));
        edcData.setNomineeShare(params.get("list8.stringval5"));
        edcData.setAppointeeDob(params.get("obj2.stringval20"));
        edcData.setAppointeeName(params.get("obj2.stringval19"));
        edcData.setAppointeeRelation(params.get("obj2.stringval21"));
        edcData.setAutoPayStatus("-999");
        edcData.setIpHeight(params.get("obj2.stringval46"));
        edcData.setIpWeight(params.get("obj2.stringval47"));
        edcData.setSmoker(params.get("obj1.stringval8"));
        edcData.setRider5AnnualPremium("-999");
        
        //TODO rider5Code, rider5SA
        edcData.setRider5Code("{\"PH_FName\":\"SANKET CHADRASHEKHAR MAHAJAN\",\"PH_Mname\":\"\",\"PH_Lname\":\"\",\"PH_KYC_FName\":\"Sanket\",\"PH_KYC_Mname\":\"Chandrashekhar\",\"PH_KYC_Lname\":\"Mahajan\",\"Cust_DOB\":\"07042000\",\"DOB\":\"07042000\",\"Gender\":\"M\",\"MobileNo\":\"\",\"EmailID\":\"\",\"PH_Permenant_address\":\"s no 61 plot no 23 shiv nagre_chinchwad gaon_chinchwad_near ganpati mandir_-__\",\"PH_communication_address\":\"______\",\"PH_PerAdd_Pincode\":\"411033\",\"PH_ComAdd_Pincode\":\"\",\"ExistingLifeInsuranceCover\":\"\",\"Education\":\"\",\"Occupation\":\"\",\"AnnualIncome\":\"\"}");
        edcData.setRider5SA("{\"PH_FName\":\"SANKET CHADRASHEKHAR MAHAJAN\",\"PH_Mname\":\"\",\"PH_Lname\":\"\",\"PH_KYC_FName\":\"Sanket\",\"PH_KYC_Mname\":\"Chandrashekhar\",\"PH_KYC_Lname\":\"Mahajan\",\"Cust_DOB\":\"07042000\",\"DOB\":\"07042000\",\"Gender\":\"M\",\"MobileNo\":\"18550983851\",\"EmailID\":\"\",\"PH_Permenant_address\":\"s no 61 plot no 23 shiv nagre_chinchwad gaon_chinchwad_near ganpati mandir_-_PUNE_MAHARASHTRA\",\"PH_communication_address\":\"s no 61 plot no 23 shiv nagre_chinchwad gaon_chinchwad_near ganpati mandir_-_PUNE_MAHARASHTRA\",\"PH_PerAdd_Pincode\":\"411033\",\"PH_ComAdd_Pincode\":\"411033\",\"ExistingLifeInsuranceCover\":\"0\",\"Education\":\"GH\",\"Occupation\":\"PR\",\"AnnualIncome\":\"800000\"}");

        
        edcData.setRider4AnnualPremium(params.get("list1.stringval4"));
        edcData.setRider4Code(params.get("list1.stringval1"));
        edcData.setRider4SA(params.get("list1.stringval2"));
        edcData.setRider3AnnualPremium(params.get("list1.stringval4"));
        edcData.setRider3Code(params.get("list1.stringval1"));
        edcData.setRider3SA(params.get("list1.stringval2"));
        edcData.setRider2AnnualPremium(params.get("list1.stringval4"));
        edcData.setRider2Code(params.get("list1.stringval1"));
        edcData.setRider2SA(params.get("list1.stringval2"));
        edcData.setRiderAnnualPremium(params.get("list1.stringval4"));
        edcData.setRiderCode(params.get("list1.stringval1"));
        edcData.setRiderSA(params.get("list1.stringval2"));
        edcData.setJourneyPaymentType(params.get("obj2.stringval23"));
        edcData.setEdcFirstOrRepeatCall("First_call");
        edcData.setNewField11("-999");
        edcData.setNewField13(params.get("obj1.stringval126"));
        edcData.setNewField14(params.get("obj1.stringval122"));
        //TODO set questionnaire
        edcData.setNewField16("FAT01_0:N|FAT02_0:N|DGHP01_0:Y|DGHP01_DGH01:N|DGHP01_DGH02:N|DGHP01_DGH03:N|DGHP01_DGH04:N|DGHP01_DGH05:N|DGHP01_DGH06:N|DGHP01_DGH07:N|DGHP01_DGH08:N|DGHP01_DGH09:N|DGHP01_DGH10:N|DGHP01_DGH11:N|DGHP01_DGH12:N|DGHP01_DGH16:-999|DGHP01_DGH13:N|DGHP01_DGH17:-999|DGHP01_DGH18:-999|DGHP01_DGH19:-999|DGHP01_DGH20:-999|DGHP01_DGH21:-999|IFH01_0:N|IFH06_0:N|IFH010_0:N|IFH07_0:0|IFH08_0:N|LS01_0:178|LS02_0:54|LS03_0:SAME|LS04_0:N|LS05_0:N|LS06_0:N|LS07_0:N|LS08_0:N|IFH02_0:-999|IFH03_0:-999|IFH04_0:-999|IFH09_0:-999|IFH011_0:-999|IFH017_0:-999|DGHF01_0:-999|DGHF02_0:-999");
        edcData.setNewField17("N");
        edcData.setNewField18("N");
        edcData.setNewField15("-999");
        edcData.setNewField19("N");
        edcData.setNewField20("-999"); //TODO BI SAR VALUE
        edcData.setNewField24(params.get("obj1.stringval11"));      //Proposal type
        edcData.setNewField22("0");
        
        Map<String, String> mapping = new HashMap<>();
        mapping.put("PH_FName", params.get("obj1.stringval13") != null ? params.get("obj1.stringval13").toString().trim() : "");
        mapping.put("PH_Mname", params.get("obj1.stringval14") != null ? params.get("obj1.stringval14").toString().trim() : "");
        mapping.put("PH_Lname", params.get("obj1.stringval15") != null ? params.get("obj1.stringval15").toString().trim() : "");
        String ekycFullName = (context.getEkycResult() != null && context.getEkycResult().getAadhaarResident() != null) 
                ? context.getEkycResult().getAadhaarResident().toString().trim() : "";
        String[] nameParts = ekycFullName.isEmpty() ? new String[0] : ekycFullName.trim().split("\\s+");
        mapping.put("PH_KYC_FName", nameParts.length > 0 ? nameParts[0] : "");
        mapping.put("PH_KYC_Mname", nameParts.length > 2 ? nameParts[1] : "");
        mapping.put("PH_KYC_Lname", nameParts.length > 1 ? nameParts[nameParts.length - 1] : "");
        mapping.put("DOB", (context.getEkycResult() != null && context.getEkycResult().getAadhaarDob() != null) 
                ? context.getEkycResult().getAadhaarDob().toString().trim() : "");
        mapping.put("Gender", (context.getEkycResult() != null && context.getEkycResult().getAadhaarGender() != null) 
                ? context.getEkycResult().getAadhaarGender().toString().trim() : "");
        mapping.put("MobileNo", (context.getEkycResult() != null && context.getEkycResult().getAadhaarPhone() != null) 
                ? context.getEkycResult().getAadhaarPhone().toString().trim() : "");
        mapping.put("EmailID", (context.getEkycResult() != null && context.getEkycResult().getAadhaarEmail() != null) 
                ? context.getEkycResult().getAadhaarEmail().toString().trim() : "");
        mapping.put("PH_ComAdd_Pincode", (context.getEkycResult() != null && context.getEkycResult().getAadhaarPin() != null) 
                ? context.getEkycResult().getAadhaarPin().toString().trim() : "");
        mapping.put("PH_FName", params.get("obj1.stringval13") != null ? params.get("obj1.stringval13").toString().trim() : "");
        mapping.put("PH_Mname", params.get("obj1.stringval14") != null ? params.get("obj1.stringval14").toString().trim() : "");
        mapping.put("PH_Lname", params.get("obj1.stringval15") != null ? params.get("obj1.stringval15").toString().trim() : "");
        mapping.put("Cust_DOB", params.get("obj1.stringval16") != null ? params.get("obj1.stringval16").toString().trim() : "");
        mapping.put("PH_Permenant_address", params.get("obj1.stringval17") != null ? params.get("obj1.stringval17").toString().trim() : "");
        mapping.put("PH_communication_address", params.get("obj1.stringval18") != null ? params.get("obj1.stringval18").toString().trim() : "");
        mapping.put("PH_PerAdd_Pincode", params.get("obj1.stringval55") != null ? params.get("obj1.stringval55").toString().trim() : "");
        mapping.put("ExistingLifeInsuranceCover", "");
        mapping.put("Education", params.get("obj1.stringval129") != null ? params.get("obj1.stringval129").toString().trim()  : "");
        mapping.put("Occupation", params.get("obj1.stringval131") != null ? params.get("obj1.stringval131").toString().trim()  : "");
        mapping.put("AnnualIncome", params.get("obj1.stringval130") != null ? params.get("obj1.stringval130").toString().trim()  : "");
        
        try {
        	edcData.setNewField34(objectMapper.writeValueAsString(mapping));            
        } catch (JsonProcessingException e) {
        	edcData.setNewField34("-999");
        }
        
         //TODO  setNewField9 setNewField23
        edcData.setNewField9("T_NO_PASA|PASA|PASA");
        
        String val104 = params.get("obj5.stringval104") != null ? params.get("obj5.stringval104") : "";
        String val105 = params.get("obj5.stringval105") != null ? params.get("obj5.stringval105") : "";
        String val106 = params.get("obj5.stringval106") != null ? params.get("obj5.stringval106") : "";

        edcData.setNewField21(val104 + "|" + val105 + "|" + val106);
        edcData.setNewField23("Y|Y|Y|Y|Y|200000|NA");
        edcData.setNewField25("N");
        edcData.setNewField52("Non-Assisted");  //by default Non-Assisted for PB
        edcData.setSubChannel2("-999");
        edcData.setAppgraphy1("-999");
        edcData.setAppgraphy2("-999");
        edcData.setXmlFile("-999");

        String jsonString = null;
        if (context != null && context.getCibilResult() != null) {
            try {
                jsonString = objectMapper.writeValueAsString(context.getCibilResult());
                // Extra check: Jackson sometimes returns literal "null" string
                if ("null".equals(jsonString)) {
                    jsonString = null;
                }
            } catch (JsonProcessingException e) {
                jsonString = "-999"; // Handle serialization error
            }
        } else {
            jsonString = "-999";
        }

        edcData.setNewField1(jsonString);        
        edcData.setNewField54("N");
        edcData.setNewField55("N");
        req.setEdcData(edcData);
        
        // Call EDC API
        context.setEdcResult(edcClient.call(req, context));

        log.info("[{}] EDC complete | score={} status={}",
                context.getCorrelationId(),
                context.getEdcResult().getErrorCode(),
                context.getEdcResult().getErrorCode());
    }
    
    
    private void executeDcsStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("DCS_API")) {
            log.info("[{}] DCS_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        DcsRequest req = new DcsRequest();
        
        req.setRefId("22185"); //TODO find what to pass
        req.setPolicyNumber(params.get("obj5.stringval24"));
        req.setApplicationNo(params.get("obj1.stringval6"));
        req.setServiceId("DCS");
        req.setRequestSource("INSTAB");
        
        DcsRequest.DataDetails detail = new DcsRequest.DataDetails();
        detail.setSlaProposerPayer(params.get("obj1.stringval8"));
        detail.setPropSignDuration("0");           //Duration in days between current date and proposal signdate. //TODO find details
        detail.setJlKycType(null); 				//Manual/CKYC/EKYC/PARTNERKYC //TODO find details
        detail.setJlAge("");				//TODO
        detail.setIsProposerPayer("No"); //TODO find details
        detail.setOccupation(params.get("obj1.stringval143"));      //LOV
        detail.setPpKycType("Manual");       //TODO //Manual/CKYC/EKYC/PARTNERKYC
        detail.setChannel("Chief Agency Officer"); //TODO get the data for PB from PMACS AND store as master data LOV  //Sales division (Agent API: salesDivisionName)—bases on agent code
        detail.setActionCode("n00"); //TODO get details
        detail.setPlvcIndicator("No"); //TODO
        detail.setIsForm60(params.get("obj1.stringval127"));
        detail.setDrcRiskFlag("");
        detail.setIsPos("No"); //TODO
        detail.setIsOfflineCase("No"); //TODO
        detail.setPpPanVerified("No"); //TODO
        detail.setProductId(params.get("obj1.stringval10"));
        detail.setResidentialStatus(params.get("obj1.stringval76"));
        detail.setAgentCode(params.get("obj1.stringval4"));
        detail.setJourneyType(params.get("obj1.stringval1"));		//PHYSICAL/INSTAB/STP/WEBSALES (Its mandate for parameter for all journey)
        detail.setSubChannel(params.get("obj5.stringval126"));
        
//        String premiumStr = params.get("obj1.stringval37");
//        String frequencyStr = params.get("obj1.stringval39");
//        double annualPremium = 0.0;
//        if (premiumStr != null && !premiumStr.isEmpty() && frequencyStr != null && !premiumStr.isEmpty()) {
//            double premium = Double.parseDouble(premiumStr);
//            double frequency = Double.parseDouble(frequencyStr);
//            annualPremium = premium * frequency;
//        }
//        detail.setTap(Integer.valueOf((int) annualPremium));       

        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {
        	
            	Double tapValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap();

	            if (tapValue != null && !String.valueOf(tapValue).trim().isEmpty() && !String.valueOf(tapValue).equalsIgnoreCase("null")) {
	            	detail.setTap(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap());		//TotalAnnualPremium
	            }        
            }
        
        detail.setPhIndustry(params.get("obj3.stringval92"));        //LOV
        detail.setIsWop("Yes");				//TODO //If Waiver of Premium rider is selected pass on “Yes” else “No”
        detail.setAutoRenewalPaymode(params.get("obj5.stringval13"));
        detail.setMhrFlag(null);
        detail.setExistingPh(params.get("obj4.stringval112"));
        detail.setPasaFlag(params.get("obj5.stringval23"));
        detail.setPremPayMode(params.get("obj2.stringval23"));
        detail.setRisksCore(null);
        detail.setIsSignature("");			//TODO //If thumb impression is marked then pass on “No” else “Yes”
        detail.setDistributionChannel("Himalayan"); //TODO
        detail.setIsJlPanVerified(""); //TODO
        detail.setExistingLa("No");		//TODO //Pass on “Yes” if the Lifeassured is an existing customer or else “No”
        detail.setIpKycType("Manual");		//TODO //Manual/CKYC/EKYC/PARTNERKYC p_in_obj_4.stringval7 =Ckyc ,p_in_obj_3.stringval107 =Ekyc 
        detail.setPhPanVerified("Yes"); //TODO get from NSDL
        detail.setIsLaproposer("No"); //TODO
        detail.setIsPennyDropSuccessful("No"); //TODO
        detail.setCreditScore(0); //TODO
        detail.setIsPhysicalHandicap(""); //TODO
        detail.setIsIcAvailable("Yes");			//TODO //This is applicable only for Instab and If Is IC Available is selected then pass on “Yes” else “No
        detail.setIndustry(params.get("obj3.stringval92"));
        detail.setIspep("No"); //TODO
       // detail.setTasa(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {
 
        	    Object tasaValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa();
 
        	    if (tasaValue != null && !String.valueOf(tasaValue).trim().isEmpty() && !String.valueOf(tasaValue).equalsIgnoreCase("null")) {
        	        detail.setTasa(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());
        	    }
        }
        
        detail.setPhIsForm60("No"); //TODO

        detail.setPremium(params.get("obj1.stringval37"));
        detail.setOccupationOthers(""); 
        detail.setKycType("Manual");	//TODO	//Manual/CKYC/EKYC/PARTNERKYC
        detail.setBillFreq(params.get("obj1.stringval39"));
        detail.setPayOrRelation(params.get("obj1.stringval47"));
        detail.setIpPanVerified("No"); //TODO
        detail.setProcessType("New Business"); //TODO
        detail.setPhRelation(params.get("obj1.stringval18"));
        detail.setIsDuplicateContact(""); //TODO
        detail.setIsJointLife("No");			//TODO //Pass on yes for jointlife products and no for non-joint life// p_in_obj_1.stringval11 if JL
        detail.setIsmwp("No");					//TODO //Pass on Yes for MWP cases p_in_obj_1.stringval11 if MWP
        detail.setProposalType(params.get("obj1.stringval11"));	
        detail.setPpIsForm60("");
        detail.setPhKycType("Manual"); //TODO
        detail.setProductType("ENDOW"); //TODO
        detail.setBopFlag(null);
        detail.setLaAge(params.get("obj1.stringval17"));
        detail.setLatestPolicyDurationFlag("99");			//TODO - find value for this (days duration from application generated till current date)
        req.setDataDetails(Collections.singletonList(detail));
        
     // Call DCS API
        context.setDcsResult(dcsClient.call(req, context));

        log.info("[{}] DCS complete | score={} status={}",
                context.getCorrelationId(),
                context.getDcsResult().getStatus(),
                context.getDcsResult().getStatusCode());
    }
    
    private void executeAuwStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("AUW_API")) {
            log.info("[{}] AUW_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        AuwRequest req = new AuwRequest();
        req.setPolicyNo(params.get("obj5.stringval24")); 
        req.setServiceId("STP");
        req.setRefNo("2307191125030125"); //TODO 
        req.setRequestSource("NGIN");
        req.setPanValidationStatus(""); //TODO from PAN API

        req.setEdcCategory("");
        req.setIsPhotoOcr("NO");
        req.setExistingLa("NO"); //TODO
        req.setExistingPh("NO"); //TODO
        req.setCheckOtc("");
        req.setKycType("PARTNERKYC"); //TODO confirm for PB
        req.setPhNsdlVerified("NA");
        req.setLaNsdlVerified("NA");
        req.setPaymentMode(params.get("obj5.stringval13"));



        req.setTrbsa(0);			//Term rider benefit Sumassured
        req.setTibsa(0);			//Terminal illness benefit Sumassured
        req.setSlbsa(0);			//Start Life benefit Sumassured
        req.setPdabsa(0);			//PDAB benefit Sumassured
        req.setMgbsa(0);			//Mahil Gain benefit Sumassured
        req.setHisa(0);			//Hospitalization Insurance benefit Sumassured
        req.setCibsa(0);			//Critical illness benefit Sumassured
        req.setAdbsa(0);			//Accidental Death benefit Sumassured
        req.setAcisa(0);			//Accelerated CI benefit Sumassured
        req.setTinFlag("");

        
//        if (context.getUcsResult() != null && 
//        	    context.getUcsResult().getTranxResponse() != null && 
//        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
//        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {
//        	    
//        		req.setSuc(params.get(String.valueOf(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getSuc())));
//        	}
        
        req.setPhEducation(params.get("obj1.stringval141"));
        req.setPhCreditScore("0");
        req.setCreditScore("0");
        req.setBranchVisit("");
        req.setBasicSa("");
        
        AuwRequest.DataDetails data = new AuwRequest.DataDetails();
        data.setEducation(params.get("obj1.stringval141"));
        data.setOccupation(params.get("obj1.stringval143"));
        data.setPhRelationship("SELF");  //TODO
        data.setTopUp("No");
        data.setChannel(params.get("obj5.stringval126"));
        data.setIsForm60(params.get("obj1.stringval123"));
        data.setResidenceCountry(params.get("obj1.stringval76"));
        
        String premiumStr = params.get("obj1.stringval37");
        String frequencyStr = params.get("obj1.stringval39");
        double annualPremium = 0.0;
        if (premiumStr != null && !premiumStr.isEmpty() && frequencyStr != null && !premiumStr.isEmpty()) {
            double premium = Double.parseDouble(premiumStr);
            double frequency = Double.parseDouble(frequencyStr);
            annualPremium = premium * frequency;
        }
        
        data.setAnnualPremium(Integer.valueOf((int) annualPremium));
        //data.setAgeOfDeath(params.get("list5(0).stringval4"));  //TODO - params.get("list5.WeoRecStrings150User(0).stringval4")
        data.setAgeOfDeath("0"); //TODO check with PB team
        data.setProductId(params.get("obj1.stringval10"));
        data.setPayerRelationship(params.get("obj1.stringval47"));
        data.setResidentialStatus(params.get("obj1.stringval76"));
        data.setTobacco(0);
        data.setAgentCode(params.get("obj1.stringval4"));
        data.setHeight((params.get("obj2.stringval55")) != null && !(params.get("obj2.stringval55")).trim().isEmpty() ? Integer.valueOf((params.get("obj2.stringval55")).trim()) : null);
        data.setAnnualIncome(null);
        data.setAlcohol(0);
        data.setCheckOtc("0");
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {
        	    
            	data.setTap(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap());
        	}
        
        data.setSuspectedCp("No"); //TODO
        data.setIsPep("NO");  //TODO
        data.setProductMultiplier(10);
        data.setWeight((params.get("obj2.stringval56")) != null && !(params.get("obj2.stringval56")).trim().isEmpty() ? Integer.valueOf((params.get("obj2.stringval56")).trim()) : null);
        data.setPhEducation(params.get("obj1.stringval141"));
        data.setAutoRenewalPaymode(params.get("obj5.stringval13"));
        data.setIsCombo("No");
        data.setPasaFlag(params.get("obj5.stringval23"));
        data.setDistributionChannel("Himalayan"); 
        data.setIsBackdated("No"); //TODO
        data.setPhAge(params.get("obj1.stringval26"));
        //data.setPhTasa(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());
        data.setPhTasa(0L); //TODO clarify
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {

        	    Object rawKey = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getSuc();
        	    if (rawKey != null && !String.valueOf(rawKey).trim().isEmpty() && !String.valueOf(rawKey).equalsIgnoreCase("null")) {
        	    	data.setSuc(String.valueOf(rawKey).trim());
        	    } 
        }
//        data.setSuc(params.get(String.valueOf(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getSuc())));
        data.setGender(params.get("obj1.stringval27"));
        data.setPhysicallyHandicapped("No");
        data.setDgh("No");
        data.setPreviouslyDeclined("");
        data.setHusbandTasa(0);
        data.setIndustry(params.get("obj3.stringval92"));
        data.setSiblingTasa(0);
        
        data.setCpId(new ArrayList<>());  //T0D0 get CP ID's list
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {

        	    // 1. Extract the raw value safely from the first list item
        	    Object tasaValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa();

        	    // 2. Perform null, empty, and literal "null" check for the TASA value
        	    if (tasaValue != null && !String.valueOf(tasaValue).trim().isEmpty() && !String.valueOf(tasaValue).equalsIgnoreCase("null")) {
        	        data.setTasa(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());
        	    }
        }
        data.setFamilyMembersAgeOfDeath(0);
        data.setOccupationOthers("");
        data.setPhOccupation(params.get("obj1.stringval143"));
        data.setProcessType("NewBusiness");
        data.setSimultaneousFlag("No");
        data.setBasicSa(params.get("obj1.stringval40"));		//TODO
        data.setPolicyNo(params.get("obj5.stringval24"));
        data.setIsStaff("No");
        data.setRiskScore(0);
        data.setChangeInWeight(0);
        data.setPreviouslyPostpone("");
        data.setMaritalStatus(params.get("obj1.stringval108"));
        data.setProductType(params.get("obj1.stringval31"));
        data.setReqCodes("100004,M018,M1196,999999");   //TODO - get codes from DCS api
        data.setCustomerId("");
        data.setMedicalFlag("Medical");
        data.setFatca("No");
        
        data.setAge((params.get("obj1.stringval26")) != null && !(params.get("obj1.stringval26")).trim().isEmpty() ? Integer.valueOf((params.get("obj1.stringval26")).trim()) : null);
        data.setProposerType("INDIVIDUAL"); //TODO Get confirmation
        data.setBmi(null);
        req.setDataDetails(data);
        
     // Call AUW API
        context.setAuwResult(auwClient.call(req, context));

        log.info("[{}] AUW complete | score={} status={}",
                context.getCorrelationId(),
                context.getAuwResult().getStatus(),
                context.getAuwResult().getStatusCode());
    }
    
    private void executeMrsStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("MRS_API")) {
            log.info("[{}] MRS_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        MrsRequest req = new MrsRequest();
        req.setRefId("22182");
        req.setPolicyNumber(params.get("obj1.stringval6"));
        req.setApplicationNo(params.get("obj1.stringval6"));
        req.setServiceId("MRS");
        req.setRequestSource(params.get("obj1.stringval1"));
        
        MrsRequest.DataDetails data = new MrsRequest.DataDetails();
        data.setProposerId("");
        data.setRmContact(params.get("obj1.stringval28"));
        data.setEducation(params.get("obj1.stringval141"));
        data.setOccupation(params.get("obj1.stringval143"));
        data.setProductid(params.get("obj1.stringval10"));
        data.setAgentName("SANJEEV KUMAR SHARMA"); //TODO get from table
        data.setChannel("Chief Agency Officer");   //TODO get from table
        data.setActionCode("n00");
        data.setCustomerCategory("");
        data.setPanNo("obj1.stringval126");
        data.setDrcRiskFlag("-999");
        data.setResidenceCountry(params.get("obj1.stringval76"));
        data.setSmoker(params.get("obj3.stringval1"));
        data.setResidentialStatus(params.get("obj1.stringval76"));
        data.setAgentCode(params.get("obj1.stringval4"));
        data.setState(params.get("obj1.stringval96"));
        data.setLatestPolDuration(null);
        data.setSubChannel(params.get("obj5.stringval126"));
        data.setAnnualIncome(null);
        data.setDghResponse("No");
        data.setPincode(params.get("obj1.stringval79"));
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {
        	
            	Object tapValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap();

	            if (tapValue != null && !String.valueOf(tapValue).trim().isEmpty() && !String.valueOf(tapValue).equalsIgnoreCase("null")) {
	            	data.setTap(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap());		//TotalAnnualPremium
	            }        
            }
        
        data.setProductMultiplier(null);
        data.setPreviousRateup("0");
        data.setProductName(params.get("obj1.stringval30"));
        String fullName = (params.get("obj1.stringval13") != null ? params.get("obj1.stringval13") : "") + " " +
                (params.get("obj1.stringval14") != null ? params.get("obj1.stringval14") : "") + " " +
                (params.get("obj1.stringval15") != null ? params.get("obj1.stringval15") : "");
        data.setProposerName(fullName.trim().replaceAll("\\s+", " "));
        
        data.setAutoRenewalPaymode(params.get("obj5.stringval13"));
        data.setTrl(0);				//TransUnion truerisk score
        data.setBranchCode("K26");   //TODO
        data.setRmEmail("test@balic.com");  //TODO
        data.setPasaFlag(params.get("obj5.stringval23"));
        data.setDob(params.get("obj1.stringval25"));
        data.setDistributionChannel("POLICY_BAZAAR");
        data.setRiderName(null);
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {

        	    // 1. Map Cisuc (CIB Success)
        	    Object cisucValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getCibsuc();
        	    if (cisucValue != null && !String.valueOf(cisucValue).trim().isEmpty() && !String.valueOf(cisucValue).equalsIgnoreCase("null")) {
        	        data.setCisuc(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getCibsuc());
        	    }
        	    Object sucValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getSuc();
        	    if (sucValue != null && !String.valueOf(sucValue).trim().isEmpty() && !String.valueOf(sucValue).equalsIgnoreCase("null")) {
        	        data.setSuc(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getSuc());
        	    }
        }
        data.setEmailId(params.get("obj1.stringval29"));
        data.setGender(params.get("obj1.stringval27"));
        data.setAgentEmail("");
        data.setCity(params.get("obj1.stringval85"));
        data.setMobileNo((params.get("obj1.stringval28")) != null && !(params.get("obj1.stringval28")).trim().isEmpty() ? Long.parseLong((params.get("obj1.stringval28")).trim()) : 0L);
//        data.setTotalAnnualPremium("99977"); //TODO get 
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {
        	
            	Double tapValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap();

	            if (tapValue != null && !String.valueOf(tapValue).trim().isEmpty() && !String.valueOf(tapValue).equalsIgnoreCase("null")) {
	            	data.setTotalAnnualPremium(String.valueOf(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTap()));		//TotalAnnualPremium
	            }        
            }
        data.setIndustry(params.get("obj3.stringval92"));
        data.setAgentClub("");
        
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {

        	    Object tasaValue = context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa();

        	    if (tasaValue != null && !String.valueOf(tasaValue).trim().isEmpty() && !String.valueOf(tasaValue).equalsIgnoreCase("null")) {
        	        data.setTasa(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());
        	    }
        }
        data.setOccupationOthers("");
        data.setPolicyNumber(params.get("obj1.stringval6"));
        data.setProcessType("New Business");
        data.setAddress(params.get("obj1.stringval147"));
        data.setIsStaff("No");
        data.setBoCode(null);
        data.setAgentMobile(null);
        data.setPassportSubmitted("No");
        data.setIsInsuranceConsultant("No");
        data.setProductType("ENDOW"); //TODO get details from PB team
        data.setLocation("PUNE");   //TODO  get details
        data.setNotifyTpa(1);
        data.setCustomerName(params.get("obj1.stringval13") + " " + params.get("obj1.stringval15"));
        data.setIsHighRisk("No"); //TODO
        data.setCustomerId(params.get("obj2.stringval82"));
        data.setAge((params.get("obj1.stringval26")) != null && !(params.get("obj1.stringval26")).trim().isEmpty() ? Integer.parseInt((params.get("obj1.stringval26")).trim()) : 0);
        req.setDataDetails(data);
        
     // Call MRS API
        context.setMrsResult(mrsClient.call(req, context));

        log.info("[{}] MRS complete | score={} status={}",
                context.getCorrelationId(),
                context.getMrsResult().getStatus(),
                context.getMrsResult().getStatusCode());
    }
    
    private void executeFesStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("FES_API")) {
            log.info("[{}] FES_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        FesRequest req = new FesRequest();
        req.setRequestSource(params.get("obj1.stringval1"));
        req.setRefId("1234");
        req.setPolicyNumber(params.get("obj5.stringval24"));
        req.setServiceId("FES");
        
        FesRequest.DataDetail detail = new FesRequest.DataDetail();
        detail.setInstallmentPremium(params.get("obj1.stringval37"));
        detail.setBillingFrequency("");       //TODO Billing Frequency of the current Proposal
        detail.setDeclaredIncome("0");         //TODO Declared Income as per the Proposal Form
        detail.setAge((params.get("obj1.stringval26")) != null && !(params.get("obj1.stringval26")).trim().isEmpty() ? (params.get("obj1.stringval26").trim()) : null);
        detail.setProductType("ULIP");            //TODO Product type like ULIP/TRAD/SAVINGS
        
        if (context.getCibilResult() != null && 
        	    context.getCibilResult()[0].getScore() != null && 
        	    !context.getCibilResult()[0].getScore().isEmpty()) {

        	    String score = context.getCibilResult()[0].getScore();

        	    if (score != null && !score.trim().isEmpty() && !score.equalsIgnoreCase("null")) {
        	        detail.setCibilScore(score);
        	    }
        }
        if (context.getUcsResult() != null && 
        	    context.getUcsResult().getTranxResponse() != null && 
        	    context.getUcsResult().getTranxResponse().getSinglelife() != null && 
        	    !context.getUcsResult().getTranxResponse().getSinglelife().isEmpty()) {

        	    String tasaValue = String.valueOf(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());
        	    String tapValue = String.valueOf(context.getUcsResult().getTranxResponse().getSinglelife().get(0).getTasa());

        	    if (tasaValue != null && !String.valueOf(tasaValue).trim().isEmpty() && !String.valueOf(tasaValue).equalsIgnoreCase("null")) {
        	        detail.setTasa(tasaValue);
        	    }
        	    if (tapValue != null && !String.valueOf(tapValue).trim().isEmpty() && !String.valueOf(tapValue).equalsIgnoreCase("null")) {
        	        detail.setTap(tapValue);
        	    }
        }
        //TODO THIS SECTION
        detail.setTotalPortfolioValue("0");   // Total Portfolio Value for surrogate income proofs (FD/MF)
        detail.setMonthlySip("0");            // Monthly SIP amount for surrogate income proofs
        detail.setPropertyType("");           // HOUSE/SHOP incase if property documents are submitted
        detail.setPropertyValue("0");         // Property value for House/Shop documents
        detail.setPropertyLoanType("");       // HOUSE/SHOP Loan Document if surrogate selected
        detail.setMonthlyEmi("0");            // Monthly Property Loan EMI
        detail.setLicTap("0");                // Total Annual Premium of existing LIC Premiums
        detail.setVehicleType("");            // CAR
        detail.setVehicleAgeYears("0");         // Vehicle Age
        detail.setExShowroomPrice("0");       // Vehicle Showroom price
        detail.setMonthlyCreditCardLimit("0");// Monthly Credit Card Limit
        detail.setNetProfit("");    
        detail.setCity(params.get("obj1.stringval85"));
        
        DataArrayDetail arrayDetail = new DataArrayDetail();
        arrayDetail.setItemType("");
        arrayDetail.setDatefrom("");
        arrayDetail.setDateto("");
        arrayDetail.setValue("");
        
        detail.setDataArrayDetails(Collections.singletonList(arrayDetail));
        req.setDataDetails(Collections.singletonList(detail));
                
     // Call AUW API
        context.setFesResult(fesClient.call(req, context));

        log.info("[{}] FES complete | score={} status={}",
                context.getCorrelationId(),
                context.getFesResult().getStatus(),
                context.getFesResult().getStatusCode());
    }
    
    private void executeEkycStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("EKYC_API")) {
            log.info("[{}] EKYC_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();
        
        EkycRequest req = new EkycRequest();
        req.setApplicationNo(params.get("obj1.stringval6"));
        
        
     // Call EKYC API
        context.setEkycResult(ekycClient.call(req, context));

        log.info("[{}] EKYC complete | score={} status={}",
                context.getCorrelationId(),
                context.getEkycResult().getErrorCode(),
                context.getEkycResult().getErrorMessage());
    }

    private void executeProposalStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("PROPOSAL_API")) {
            log.info("[{}] PROPOSAL_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        ProposalRequest proposal = new ProposalRequest();

// Top-level fields
        proposal.setActivefunds("");

// agentDetails
        ProposalRequest.AgentDetails agentDetails = new ProposalRequest.AgentDetails();
        agentDetails.setFscIcCode(params.get("obj2.stringval139"));
        agentDetails.setIcSmName("");
        agentDetails.setLeadCode("");
        agentDetails.setProposalType("");
        agentDetails.setRtlnRefCode("");
        agentDetails.setSpCode("");
        agentDetails.setSpName("");
        agentDetails.setSubIdCode("");
        proposal.setAgentDetails(agentDetails);

        proposal.setApplnNo(params.get("obj1.stringval6"));

// appointeeDetails
        ProposalRequest.AppointeeDetails appointeeDetails = new ProposalRequest.AppointeeDetails();
        appointeeDetails.setApptDOB(params.get("obj2.stringval20"));
        appointeeDetails.setApptFirstNm("");
        appointeeDetails.setApptFullName(params.get("obj2.stringval19"));
        appointeeDetails.setApptLastNm("");
        appointeeDetails.setApptMiddleNm("");
        appointeeDetails.setApptRtlnNominee(params.get("obj2.stringval21"));
        proposal.setAppointeeDetails(appointeeDetails);

        proposal.setBackdatedDate("");

// bankPayerDetails
        ProposalRequest.BankPayerDetails bankPayerDetails = new ProposalRequest.BankPayerDetails();
        bankPayerDetails.setAccountNo(params.get("obj2.stringval32"));
        bankPayerDetails.setAccountType(params.get("obj2.stringval35"));
        bankPayerDetails.setApplicantKnownMonths("");
        bankPayerDetails.setApplicantKnownYears("");
        bankPayerDetails.setApplicantRltdEmp("");
        bankPayerDetails.setBankName(params.get("obj2.stringval31"));
        bankPayerDetails.setBranchName(params.get("obj2.stringval31"));
        bankPayerDetails.setEiaNo(params.get("obj3.stringval135"));
        bankPayerDetails.setIfscCode(params.get("obj2.stringval36"));
        bankPayerDetails.setPayerAge("");
        bankPayerDetails.setPayerArea(params.get("obj1.stringval50"));
        bankPayerDetails.setPayerDistrict("");
        bankPayerDetails.setPayerDob(params.get("obj1.stringval48"));
        bankPayerDetails.setPayerFirstName(params.get("obj1.stringval46"));
        bankPayerDetails.setPayerFlatNo("");
        bankPayerDetails.setPayerGender(params.get("obj1.stringval49"));
        bankPayerDetails.setPayerLandmark("");
        bankPayerDetails.setPayerLastName(params.get("obj1.stringval46"));
        bankPayerDetails.setPayerPanNo("");
        bankPayerDetails.setPayerPremisesName("");
        bankPayerDetails.setPayerState("");
        bankPayerDetails.setPayerStreet("");
        bankPayerDetails.setPayerTown("");
        bankPayerDetails.setPayerType("");
        bankPayerDetails.setPaymentMode("");
        bankPayerDetails.setPremiumPaidBy(params.get("stringval45"));
        bankPayerDetails.setRtlnWtIp(params.get("obj1.stringval47"));
        proposal.setBankPayerDetails(bankPayerDetails);

// bondTargetFund (example for one element)
        ProposalRequest.FundsDetails bondTargetFund = new ProposalRequest.FundsDetails();
        bondTargetFund.setBiNumber("");
        bondTargetFund.setCreatedBy("");
        bondTargetFund.setCreatedDt("");
        bondTargetFund.setFundCode("");
        bondTargetFund.setFundId(0);
        bondTargetFund.setFundName("");
        bondTargetFund.setNbId(0);
        bondTargetFund.setPercentageInvested(0);
        bondTargetFund.setUpdatedBy("");
        bondTargetFund.setUpdatedDt("");
        proposal.setBondTargetFund(Collections.singletonList(bondTargetFund));

// childCareDetails (example for one element)
        ProposalRequest.ChildCareDetails childCareDetails = new ProposalRequest.ChildCareDetails();
        childCareDetails.setBeneficiaryName("");
        childCareDetails.setBt("");
        childCareDetails.setDob("");
        childCareDetails.setGender("");
        childCareDetails.setMonthlyIncome("");
        childCareDetails.setPlanOption("");
        childCareDetails.setPt("");
        childCareDetails.setRelation("");
        childCareDetails.setRiderSumAssured("");
        proposal.setChildCareDetails(Collections.singletonList(childCareDetails));

        proposal.setCreationDate("");
        proposal.setExistingFamilyPolicy("");
        proposal.setFamilyBenefit("");

// fatcaDetails
        ProposalRequest.FatcaDetails fatcaDetails = new ProposalRequest.FatcaDetails();
        fatcaDetails.setCountryAddress("");
        fatcaDetails.setCountryName("");
        fatcaDetails.setHoldMailInstruction("");
        fatcaDetails.setHoldMailInstructionDetails("");
        fatcaDetails.setIsdCode("");
        fatcaDetails.setLandlineNumber("");
        fatcaDetails.setMobileNumer("");
        fatcaDetails.setPowerOfAttorney("");
        fatcaDetails.setPowerOfAttorneyAddress("");
        fatcaDetails.setPowerOfAttorneyName("");
        fatcaDetails.setPowerOfAttorneyNumber("");
        fatcaDetails.setResidentOutsideIndia("");
        fatcaDetails.setStandardDetails("");
        fatcaDetails.setStandardInstruction("");
        fatcaDetails.setTaxResidentOutsideIndia("");
        fatcaDetails.setTelephoneNumberOutIndia("");
        fatcaDetails.setTinIssuingCountry("");
        fatcaDetails.setTinNumber("");
        proposal.setFatcaDetails(fatcaDetails);

// fundsDetails (example for one element)
        ProposalRequest.FundsDetails fundsDetails = new ProposalRequest.FundsDetails();
        fundsDetails.setBiNumber("");
        fundsDetails.setCreatedBy("");
        fundsDetails.setCreatedDt("");
        fundsDetails.setFundCode("");
        fundsDetails.setFundId(0);
        fundsDetails.setFundName("");
        fundsDetails.setNbId(0);
        fundsDetails.setPercentageInvested(0);
        fundsDetails.setUpdatedBy("");
        fundsDetails.setUpdatedDt("");
        proposal.setFundsDetails(Collections.singletonList(fundsDetails));

        proposal.setIccrOtpGenerateDt(params.get("obj4.stringval94"));
        proposal.setIccrOtpTimestamp(params.get("obj4.stringval91"));
        proposal.setIccrOtpValidateDt(params.get("obj4.stringval92"));
        proposal.setInvestStrategyCode("");

// ipCurrentAddress
        ProposalRequest.IpCurrentAddress ipCurrentAddress = new ProposalRequest.IpCurrentAddress();
        ipCurrentAddress.setAddressType("");
        ipCurrentAddress.setArea(params.get("obj1.stringval59"));
        ipCurrentAddress.setBuildingName(params.get("obj1.stringval57"));
        ipCurrentAddress.setBuildingNumber(params.get("obj1.stringval56"));
        ipCurrentAddress.setCity(params.get("obj1.stringval61"));
        ipCurrentAddress.setCo("");
        ipCurrentAddress.setCountry(params.get("obj1.stringval52"));
        ipCurrentAddress.setIpPhType("");
        ipCurrentAddress.setLandmark(params.get("obj1.stringval59"));
        ipCurrentAddress.setPerAddIsCommAdd(params.get("obj1.stringval64"));
        ipCurrentAddress.setPincode(params.get("obj1.stringval55"));
        ipCurrentAddress.setPoliceStation("");
        ipCurrentAddress.setState(params.get("obj1.stringval62"));
        ipCurrentAddress.setStreetName(params.get("obj1.stringval60"));
        ipCurrentAddress.setTown(params.get("obj1.stringval61"));
        proposal.setIpCurrentAddress(ipCurrentAddress);

// ipDetails
        ProposalRequest.IpDetails ipDetails = new ProposalRequest.IpDetails();
        ipDetails.setAge(params.get("obj1.stringval17"));
        ipDetails.setAlternateMobileNo(params.get("obj3.stringval116"));
        ipDetails.setAnnualIncome(params.get("obj1.stringval130"));
        ipDetails.setCountryOfBirth(params.get("obj1.stringval53"));
        ipDetails.setCountryOfResidence(params.get("obj1.stringval52"));
        ipDetails.setDob(params.get("obj1.stringval16"));
        ipDetails.setEducation(params.get("obj1.stringval129"));
        ipDetails.setEmailId(params.get("obj1.stringval20"));
        ipDetails.setEmployer("");
        ipDetails.setEmployerAddress("");
        ipDetails.setEmployerContactNo("");
        ipDetails.setEmployerWebsite("");
        ipDetails.setFacebookId("");
        ipDetails.setFatherName(params.get("obj1.stringval102"));
        ipDetails.setFirstName(params.get("obj1.stringval13"));
        ipDetails.setGender(params.get("obj1.stringval18"));
        ipDetails.setIpPhType("");
        ipDetails.setIpRelation("");
        ipDetails.setIsHandicappedAdverse("");
        ipDetails.setLastName(params.get("obj1.stringval15"));
        ipDetails.setLifeGoal("");
        ipDetails.setMaritalStatus(params.get("obj1.stringval99"));
        ipDetails.setMiddleName(params.get("obj1.stringval14"));
        ipDetails.setMobileNo(params.get("obj1.stringval19"));
        ipDetails.setMotherName("");
        ipDetails.setNationality(params.get("obj1.stringval51"));
        ipDetails.setNatureOfDuty("");
        ipDetails.setOccupation("");
        ipDetails.setPan("");
        ipDetails.setPhoneNo("");
        ipDetails.setPlaceOfBirth("");
        ipDetails.setProceedWithForm60("");
        ipDetails.setPurposeOfInsurance("");
        ipDetails.setResidenceStatus("");
        ipDetails.setSpouseName("");
        ipDetails.setSuffix("");
        ipDetails.setTitle("");
        proposal.setIpDetails(ipDetails);

// ipFemaleDGHDetails
        ProposalRequest.IpFemaleDGHDetails ipFemaleDGHDetails = new ProposalRequest.IpFemaleDGHDetails();
        ipFemaleDGHDetails.setAreYouPregnant("");
        ipFemaleDGHDetails.setDescription("");
        ipFemaleDGHDetails.setHaveAnyGynecologicalCompications("");
        ipFemaleDGHDetails.setTotalCoverageHusband("");
        proposal.setIpFemaleDGHDetails(ipFemaleDGHDetails);

// ipGoodHealthdetails
        ProposalRequest.IpGoodHealthdetails ipGoodHealthdetails = new ProposalRequest.IpGoodHealthdetails();
        ipGoodHealthdetails.setAnyInjuryDisorder("");
        ipGoodHealthdetails.setHaveEverDiagnosed("");
        ipGoodHealthdetails.setIsAsthma("");
        ipGoodHealthdetails.setIsBloodDisorder("");
        ipGoodHealthdetails.setIsBypassSurgery("");
        ipGoodHealthdetails.setIsCancer("");
        ipGoodHealthdetails.setIsChestPain("");
        ipGoodHealthdetails.setIsDiabetes("");
        ipGoodHealthdetails.setIsGenitourinary("");
        ipGoodHealthdetails.setIsHIVPositive("");
        ipGoodHealthdetails.setIsLiverDisorder("");
        ipGoodHealthdetails.setIsPancreatitis("");
        ipGoodHealthdetails.setIsPhysicalDeformity("");
        ipGoodHealthdetails.setIsStrock("");
        proposal.setIpGoodHealthdetails(ipGoodHealthdetails);

// ipInsuranceFamilyDetails
        ProposalRequest.IpInsuranceFamilyDetails ipInsuranceFamilyDetails = new ProposalRequest.IpInsuranceFamilyDetails();
        ipInsuranceFamilyDetails.setAnnualPremiumForDependents("");
        ipInsuranceFamilyDetails.setCountOfPolicies("");
// familyDetails (example for one element)
        ProposalRequest.FamilyMember familyMember = new ProposalRequest.FamilyMember();
        familyMember.setAge("");
        familyMember.setAgeAtDeath("");
        familyMember.setCauseOfDeath("");
        familyMember.setHealthStatus("");
        familyMember.setMemberName("");
        ipInsuranceFamilyDetails.setFamilyDetails(Collections.singletonList(familyMember));
        ipInsuranceFamilyDetails.setIsHistoryOFDiabetes("");
        ipInsuranceFamilyDetails.setIsLifeHealthInsuranceDeclined("");
        ipInsuranceFamilyDetails.setIsPolicaticalExposed("");
        ipInsuranceFamilyDetails.setNumberOfMemberDiagnosisTime("");
        ipInsuranceFamilyDetails.setPolicaticalExposedDetails("");
        ipInsuranceFamilyDetails.setTotalSumAssured("");
        proposal.setIpInsuranceFamilyDetails(ipInsuranceFamilyDetails);

// ipKycAmlDocDetails
        ProposalRequest.IpKycAmlDocDetails ipKycAmlDocDetails = new ProposalRequest.IpKycAmlDocDetails();
        ipKycAmlDocDetails.setAddressProof("");
        ipKycAmlDocDetails.setAgeProof("");
        ipKycAmlDocDetails.setIdentityProof("");
        ipKycAmlDocDetails.setIncomeProof("");
        ipKycAmlDocDetails.setOtherDocuments("");
        proposal.setIpKycAmlDocDetails(ipKycAmlDocDetails);

// ipLifeStyleDetails
        ProposalRequest.IpLifeStyleDetails ipLifeStyleDetails = new ProposalRequest.IpLifeStyleDetails();
        ipLifeStyleDetails.setAdventurousAvocation("");
        ipLifeStyleDetails.setCauseOfWeightChange("");
        ipLifeStyleDetails.setChangeInConsumption("");
        ipLifeStyleDetails.setConvictedCourtLaw("");
        ipLifeStyleDetails.setDateOfQuit("");
        ipLifeStyleDetails.setFrequencyOfConsumption("");
        ipLifeStyleDetails.setHeight("");
        ipLifeStyleDetails.setIsConsumetobacco("");
        ipLifeStyleDetails.setIsRegularConsumeAlcohol("");
        ipLifeStyleDetails.setIsbodyWeightChanged("");
        ipLifeStyleDetails.setNarcoticsTreatment("");
        ipLifeStyleDetails.setNarcoticsTreatmentDetails("");
        ipLifeStyleDetails.setQuanityPerDayOfTabbacco("");
        ipLifeStyleDetails.setQuantityOfConsumption("");
        ipLifeStyleDetails.setTobaccoProduct("");
        ipLifeStyleDetails.setWeight("");
        proposal.setIpLifeStyleDetails(ipLifeStyleDetails);

// ipPermanentAddress
        ProposalRequest.IpPermanentAddress ipPermanentAddress = new ProposalRequest.IpPermanentAddress();
        ipPermanentAddress.setAddressType("");
        ipPermanentAddress.setArea(params.get("obj1.stringval69"));
        ipPermanentAddress.setBuildingName("");
        ipPermanentAddress.setBuildingNumber("");
        ipPermanentAddress.setCity(params.get("obj1.stringval71"));
        ipPermanentAddress.setCo("");
        ipPermanentAddress.setCountry("");
        ipPermanentAddress.setIpPhType("");
        ipPermanentAddress.setLandmark(params.get("obj1.stringval69"));
        ipPermanentAddress.setPerAddIsCommAdd(params.get("obj1.stringval64"));
        ipPermanentAddress.setPincode(params.get("obj1.stringval65"));
        ipPermanentAddress.setPoliceStation("");
        ipPermanentAddress.setState("");
        ipPermanentAddress.setStreetName("");
        ipPermanentAddress.setTown("");
        proposal.setIpPermanentAddress(ipPermanentAddress);

// ipPosgsDGHDto
        ProposalRequest.IpPosgsDGHDto ipPosgsDGHDto = new ProposalRequest.IpPosgsDGHDto();
        ipPosgsDGHDto.setDgh16("");
        ipPosgsDGHDto.setDgh17("");
        ipPosgsDGHDto.setDgh18("");
        ipPosgsDGHDto.setDgh19("");
        ipPosgsDGHDto.setDgh20("");
        ipPosgsDGHDto.setDgh21("");
        proposal.setIpPosgsDGHDto(ipPosgsDGHDto);

        proposal.setIsPOS("");
        proposal.setIssueLater("");

// liquidTargetFund (example for one element)
        ProposalRequest.FundsDetails liquidTargetFund = new ProposalRequest.FundsDetails();
        liquidTargetFund.setBiNumber("");
        liquidTargetFund.setCreatedBy("");
        liquidTargetFund.setCreatedDt("");
        liquidTargetFund.setFundCode("");
        liquidTargetFund.setFundId(0);
        liquidTargetFund.setFundName("");
        liquidTargetFund.setNbId(0);
        liquidTargetFund.setPercentageInvested(0);
        liquidTargetFund.setUpdatedBy("");
        liquidTargetFund.setUpdatedDt("");
        proposal.setLiquidTargetFund(Collections.singletonList(liquidTargetFund));

        proposal.setNbId(0);

// nomineeDetails (example for one element)
        ProposalRequest.NomineeDetails nomineeDetails = new ProposalRequest.NomineeDetails();
        nomineeDetails.setApptDOB("");
        nomineeDetails.setApptFirstNm("");
        nomineeDetails.setApptFullName("");
        nomineeDetails.setApptLastNm("");
        nomineeDetails.setApptMiddleNm("");
        nomineeDetails.setApptRtlnNominee("");
        nomineeDetails.setFirstName("");
        nomineeDetails.setLastname("");
        nomineeDetails.setMiddleName("");
        nomineeDetails.setNomineeFullName("");
        nomineeDetails.setNomineeShare("");
        nomineeDetails.setNomineedob("");
        nomineeDetails.setRtlnNominee("");
        proposal.setNomineeDetails(Collections.singletonList(nomineeDetails));

        proposal.setOtpAcceptance("");

// phCurrentAddress
        ProposalRequest.PhAddress phCurrentAddress = new ProposalRequest.PhAddress();
        phCurrentAddress.setAddressType("");
        phCurrentAddress.setArea(params.get("obj1.stringval83"));
        phCurrentAddress.setBuildingName(params.get("obj1.stringval81"));
        phCurrentAddress.setBuildingNumber(params.get("obj1.stringval80"));
        phCurrentAddress.setCity(params.get("obj1.stringval85"));
        phCurrentAddress.setCo("");
        phCurrentAddress.setCountry(params.get("obj1.stringval76"));
        phCurrentAddress.setIpPhType("");
        phCurrentAddress.setLandmark(params.get("obj1.stringval83"));
        phCurrentAddress.setPerAddIsCommAdd("");
        phCurrentAddress.setPincode(params.get("obj1.stringval79"));
        phCurrentAddress.setPoliceStation("");
        phCurrentAddress.setState(params.get("obj1.stringval86"));
        phCurrentAddress.setStreetName(params.get("obj1.stringval82"));
        phCurrentAddress.setTown(params.get("obj1.stringval84"));
        proposal.setPhCurrentAddress(phCurrentAddress);

// phDetails
        ProposalRequest.PhDetails phDetails = new ProposalRequest.PhDetails();
        phDetails.setAge(params.get("obj1.stringval26"));
        phDetails.setAlternateMobileNo(params.get("obj3.stringval93"));
        phDetails.setAnnualIncome(params.get("obj1.stringval142"));
        phDetails.setCountryOfBirth(params.get("obj1.stringval77"));
        phDetails.setCountryOfResidence(params.get("obj1.stringval76"));
        phDetails.setDob(params.get("obj1.stringval25"));
        phDetails.setEducation(params.get("obj1.stringval141"));
        phDetails.setEmailId(params.get("obj1.stringval29"));
        phDetails.setEmployer(params.get("obj1.stringval146"));
        phDetails.setEmployerAddress(params.get("obj1.stringval147"));
        phDetails.setEmployerContactNo(params.get("obj1.stringval148"));
        phDetails.setEmployerWebsite("");
        phDetails.setFacebookId("");
        phDetails.setFatherName("");
        phDetails.setFirstName(params.get("obj1.stringval22"));
        phDetails.setGender(params.get("obj1.stringval27"));
        phDetails.setIpPhType("");
        phDetails.setIpRelation("");
        phDetails.setIsHandicappedAdverse("");
        phDetails.setLastName(params.get("obj1.stringval24"));
        phDetails.setMaritalStatus("");
        phDetails.setMiddleName(params.get("obj1.stringval23"));
        phDetails.setMobileNo(params.get("obj1.stringval28"));
        phDetails.setMotherName(params.get("obj3.stringval112"));
        phDetails.setNationality(params.get("obj1.stringval75"));
        phDetails.setNatureOfDuty("");
        phDetails.setOccupation(params.get("obj1.stringval143"));
        phDetails.setPan(params.get("obj1.stringval126"));
        phDetails.setPhoneNo("");
        phDetails.setPlaceOfBirth(params.get("obj1.stringval77"));
        phDetails.setProceedWithForm60(params.get("obj1.stringval127"));
        phDetails.setPurposeOfInsurance("");
        phDetails.setResidenceStatus("");
        phDetails.setSpouseName(params.get("obj1.stringval112"));
        phDetails.setSuffix("");
        phDetails.setTitle(params.get("obj1.stringval21"));
        proposal.setPhDetails(phDetails);

// phFemaleDGHDetails
        ProposalRequest.PhFemaleDGHDetails phFemaleDGHDetails = new ProposalRequest.PhFemaleDGHDetails();
        phFemaleDGHDetails.setAreYouPregnant("");
        phFemaleDGHDetails.setDescription("");
        phFemaleDGHDetails.setHaveAnyGynecologicalCompications("");
        phFemaleDGHDetails.setTotalCoverageHusband("");
        proposal.setPhFemaleDGHDetails(phFemaleDGHDetails);

// phGoodHealthdetails
        ProposalRequest.PhGoodHealthdetails phGoodHealthdetails = new ProposalRequest.PhGoodHealthdetails();
        phGoodHealthdetails.setAnyInjuryDisorder("");
        phGoodHealthdetails.setHaveEverDiagnosed("");
        phGoodHealthdetails.setIsAsthma("");
        phGoodHealthdetails.setIsBloodDisorder("");
        phGoodHealthdetails.setIsBypassSurgery("");
        phGoodHealthdetails.setIsCancer("");
        phGoodHealthdetails.setIsChestPain("");
        phGoodHealthdetails.setIsDiabetes("");
        phGoodHealthdetails.setIsGenitourinary("");
        phGoodHealthdetails.setIsHIVPositive("");
        phGoodHealthdetails.setIsLiverDisorder("");
        phGoodHealthdetails.setIsPancreatitis("");
        phGoodHealthdetails.setIsPhysicalDeformity("");
        phGoodHealthdetails.setIsStrock("");
        proposal.setPhGoodHealthdetails(phGoodHealthdetails);

// phInsuranceFamilyDetails
        ProposalRequest.PhInsuranceFamilyDetails phInsuranceFamilyDetails = new ProposalRequest.PhInsuranceFamilyDetails();
        phInsuranceFamilyDetails.setAnnualPremiumForDependents("");
        phInsuranceFamilyDetails.setCountOfPolicies("");
// familyDetails (example for one element)
        ProposalRequest.FamilyMember phFamilyMember = new ProposalRequest.FamilyMember();
        phFamilyMember.setAge("");
        phFamilyMember.setAgeAtDeath("");
        phFamilyMember.setCauseOfDeath("");
        phFamilyMember.setHealthStatus("");
        phFamilyMember.setMemberName("");
        phInsuranceFamilyDetails.setFamilyDetails(Collections.singletonList(phFamilyMember));
        phInsuranceFamilyDetails.setIsHistoryOFDiabetes("");
        phInsuranceFamilyDetails.setIsLifeHealthInsuranceDeclined("");
        phInsuranceFamilyDetails.setIsPolicaticalExposed("");
        phInsuranceFamilyDetails.setNumberOfMemberDiagnosisTime("");
        phInsuranceFamilyDetails.setPolicaticalExposedDetails("");
        phInsuranceFamilyDetails.setTotalSumAssured("");
        proposal.setPhInsuranceFamilyDetails(phInsuranceFamilyDetails);

// phKycAmlDocDetails
        ProposalRequest.PhKycAmlDocDetails phKycAmlDocDetails = new ProposalRequest.PhKycAmlDocDetails();
        phKycAmlDocDetails.setAddressProof("");
        phKycAmlDocDetails.setAgeProof("");
        phKycAmlDocDetails.setIdentityProof("");
        phKycAmlDocDetails.setIncomeProof("");
        phKycAmlDocDetails.setOtherDocuments("");
        proposal.setPhKycAmlDocDetails(phKycAmlDocDetails);

// phLifeStyleDetails
        ProposalRequest.PhLifeStyleDetails phLifeStyleDetails = new ProposalRequest.PhLifeStyleDetails();
        phLifeStyleDetails.setAdventurousAvocation("");
        phLifeStyleDetails.setCauseOfWeightChange("");
        phLifeStyleDetails.setChangeInConsumption("");
        phLifeStyleDetails.setConvictedCourtLaw("");
        phLifeStyleDetails.setDateOfQuit("");
        phLifeStyleDetails.setFrequencyOfConsumption("");
        phLifeStyleDetails.setHeight("");
        phLifeStyleDetails.setIsConsumetobacco("");
        phLifeStyleDetails.setIsRegularConsumeAlcohol("");
        phLifeStyleDetails.setIsbodyWeightChanged("");
        phLifeStyleDetails.setNarcoticsTreatment("");
        phLifeStyleDetails.setNarcoticsTreatmentDetails("");
        phLifeStyleDetails.setQuanityPerDayOfTabbacco("");
        phLifeStyleDetails.setQuantityOfConsumption("");
        phLifeStyleDetails.setTobaccoProduct("");
        phLifeStyleDetails.setWeight("");
        proposal.setPhLifeStyleDetails(phLifeStyleDetails);

// phPermanentAddress
        ProposalRequest.PhAddress phPermanentAddress = new ProposalRequest.PhAddress();
        phPermanentAddress.setAddressType("");
        phPermanentAddress.setArea(params.get("obj1.stringval93"));
        phPermanentAddress.setBuildingName(params.get("obj1.stringval91"));
        phPermanentAddress.setBuildingNumber(params.get("obj1.stringval90"));
        phPermanentAddress.setCity(params.get("obj1.stringval95"));
        phPermanentAddress.setCo("");
        phPermanentAddress.setCountry("");
        phPermanentAddress.setIpPhType("");
        phPermanentAddress.setLandmark(params.get("obj1.stringval93"));
        phPermanentAddress.setPerAddIsCommAdd(params.get("obj1.stringval88"));
        phPermanentAddress.setPincode(params.get("obj1.stringval89"));
        phPermanentAddress.setPoliceStation("");
        phPermanentAddress.setState(params.get("obj1.stringval96"));
        phPermanentAddress.setStreetName("");
        phPermanentAddress.setTown("");
        proposal.setPhPermanentAddress(phPermanentAddress);

// phPosgsDGHDto
        ProposalRequest.PhPosgsDGHDto phPosgsDGHDto = new ProposalRequest.PhPosgsDGHDto();
        phPosgsDGHDto.setDgh16("");
        phPosgsDGHDto.setDgh17("");
        phPosgsDGHDto.setDgh18("");
        phPosgsDGHDto.setDgh19("");
        phPosgsDGHDto.setDgh20("");
        phPosgsDGHDto.setDgh21("");
        proposal.setPhPosgsDGHDto(phPosgsDGHDto);

// premiumCollectionDetails
        ProposalRequest.PremiumCollectionDetails premiumCollectionDetails = new ProposalRequest.PremiumCollectionDetails();
        premiumCollectionDetails.setProposalDeposit("");
        proposal.setPremiumCollectionDetails(premiumCollectionDetails);

// premiumDetails
        ProposalRequest.PremiumDetails premiumDetails = new ProposalRequest.PremiumDetails();
        premiumDetails.setBenefitCode("");
        premiumDetails.setBiId(0);
        premiumDetails.setDefermentPeriod("");
        premiumDetails.setDeferredIncome("");
        premiumDetails.setEarlyIncome("");
        premiumDetails.setFrequency("");
        premiumDetails.setGoalProtectionBenefit("");
        premiumDetails.setIncreasingIncome("");
        premiumDetails.setInvestStrategy("");
        premiumDetails.setMode("");
        premiumDetails.setNbId("");
        premiumDetails.setOptedFund("");
        premiumDetails.setOptionVariant(params.get("obj1.stringval31"));
        premiumDetails.setPolicyTerm("");
        premiumDetails.setPremium(params.get("obj1.stringval37"));
        premiumDetails.setPremiumBackUp(0);
        premiumDetails.setPremiumTerm("");
        premiumDetails.setProductId(params.get("obj1.stringval10"));
        premiumDetails.setProductMultiplier(params.get("obj1.stringval38"));
        premiumDetails.setProductName(params.get("obj1.stringval30"));
        premiumDetails.setProductType("");
        premiumDetails.setProposalType(params.get("obj1.stringval11"));
        premiumDetails.setRop("");
        premiumDetails.setSpwPercentage("");
        premiumDetails.setSpwStartYear("");
        premiumDetails.setSumAssured(params.get("obj1.stringval41"));
        premiumDetails.setWealth("");
        proposal.setPremiumDetails(premiumDetails);

        proposal.setProductUin("");
        proposal.setProposalOtpGenerateDt("");
        proposal.setProposalOtpValidateDt("");
        proposal.setProposalOtptimestamp("");
        proposal.setRelationship("");

// riderDetails
        ProposalRequest.RiderDetails riderDetails = new ProposalRequest.RiderDetails();
        riderDetails.setAccidentalDeathBenefit("");
        riderDetails.setAccidentalPermanentTotal("");
        riderDetails.setAdbRppt("");
        riderDetails.setAdbRt("");
        riderDetails.setAprAdbRppt("");
        riderDetails.setAprAdbRt("");
        riderDetails.setAprAdbSA("");
        riderDetails.setAprAtpdRppt("");
        riderDetails.setAprAtpdRt("");
        riderDetails.setAprAtpdSA("");
        riderDetails.setAtpdRppt("");
        riderDetails.setAtpdRt("");
        riderDetails.setCiOption("");
        riderDetails.setCiRppt("");
        riderDetails.setCiRt("");
        riderDetails.setCriticalIllnessBenefit("");
        riderDetails.setFamilyIncomeBenefit("");
        riderDetails.setWop("");
        proposal.setRiderDetails(riderDetails);

// sourceFund (example for one element)
        ProposalRequest.FundsDetails sourceFund = new ProposalRequest.FundsDetails();
        sourceFund.setBiNumber("");
        sourceFund.setCreatedBy("");
        sourceFund.setCreatedDt("");
        sourceFund.setFundCode("");
        sourceFund.setFundId(0);
        sourceFund.setFundName("");
        sourceFund.setNbId(0);
        sourceFund.setPercentageInvested(0);
        sourceFund.setUpdatedBy("");
        sourceFund.setUpdatedDt("");
        proposal.setSourceFund(Collections.singletonList(sourceFund));

// topUpPremiumDetails
        ProposalRequest.TopUpPremiumDetails topUpPremiumDetails = new ProposalRequest.TopUpPremiumDetails();
        topUpPremiumDetails.setApplicationNo("");
        topUpPremiumDetails.setId(0);
        topUpPremiumDetails.setIsTopUp("");
        topUpPremiumDetails.setModuleName("");
        topUpPremiumDetails.setNbId(0);
        topUpPremiumDetails.setStatus("");
        topUpPremiumDetails.setTopUpMultiplier("");
        topUpPremiumDetails.setTopUpPremium("");
        topUpPremiumDetails.setTopUpPremiumRangeFrom("");
        topUpPremiumDetails.setTopUpPremiumRangeTo("");
        topUpPremiumDetails.setTopUpSumAssured("");
        proposal.setTopUpPremiumDetails(topUpPremiumDetails);

        // Call PROPOSAL API
        context.setProposalResult(proposalClient.call(proposal, context));

        log.info("[{}] PROPOSAL complete | score={} status={}",
                context.getCorrelationId(),
                context.getProposalResult().getStatus(),
                context.getProposalResult().getStatus());
    }

    private void executePennyDropStage(JourneyContext context, Set<String> succeeded) {
        if (succeeded.contains("PENNYDROP_API")) {
            log.info("[{}] PENNYDROP_API already succeeded — skipping", context.getCorrelationId());
            return;
        }

        Map<String, String> params = context.getRawParams();

        PennyDropRequest req = new PennyDropRequest();
        req.setRequestSource("POLICY_BAZAAR");
        req.setAccountHolder(params.get("obj1.stringval46"));
        req.setAccountNo(params.get("obj2.stringval32"));
        req.setIfscCode(params.get("obj2.stringval36"));


        // Call penny drop API
        context.setPennyDropResult(pennyDropClient.call(req, context));

        log.info("[{}] pennyDrop complete | score={} status={}",
                context.getCorrelationId(),
                context.getEkycResult().getErrorCode(),
                context.getEkycResult().getErrorMessage());
    }


    public static String convertDobFormat(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return null;
        }
        // Define the input and output patterns
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
 
        try {
            LocalDate date = LocalDate.parse(inputDate.trim(), inputFormatter);
            return date.format(outputFormatter);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Determines the value of v_rop_flag based on productId and riderCode.
     * Returns "Y" if the conditions are met, otherwise null.
     */

    public static String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder nameBuilder = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty() && !"null".equalsIgnoreCase(firstName)) {
            nameBuilder.append(firstName.trim());
        }
        if (middleName != null && !middleName.trim().isEmpty() && !"null".equalsIgnoreCase(middleName)) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty() && !"null".equalsIgnoreCase(lastName)) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(lastName.trim());
        }
        return nameBuilder.toString();
    }


}
