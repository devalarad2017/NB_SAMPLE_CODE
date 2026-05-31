package com.balic.newbusiness.pas;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.balic.newbusiness.integration.model.pas.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.integration.model.dcs.DcsResponse.TransactionDetail;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.mapping.MappingService;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * PasApiClient — submits the final application to PAS.
 *
 * PAS request is the richest in the journey — it combines:
 *   1. Partner's raw params (mapped via property file: default-PAS_API.properties)
 *   2. Response fields from ALL prior APIs (set manually — clean, explicit code)
 *   3. Context fields (correlationId, partnerCode)
 *
 * WHY MANUAL ASSIGNMENT FOR RESPONSE FIELDS:
 *   Response fields come from typed Java objects (EligibilityResponse, EdcResponse etc.)
 *   Property file mapping only works for raw string params (stringvalN).
 *   Manual assignment is compile-safe, IDE navigable, and easy to read.
 *   This is intentional — not a shortcut.
 */
@Service
public class PasApiClient {

    private static final Logger log      = LoggerFactory.getLogger(PasApiClient.class);
    private static final String STAGE    = "PAS_SUBMISSION_STAGE";
    private static final String API_NAME = "PAS_API";

    @Value("${api.endpoints.pas}")
    private String pasUrl;

    @Autowired private RestTemplate           restTemplate;
    @Autowired private MappingService         mappingService;
    @Autowired private JourneyTrackingService trackingService;
    @Autowired
    private ObjectMapper objectMapper;

//    @Retryable(value = {RuntimeException.class}, maxAttempts = 3,
//               backoff = @Backoff(delay = 3000, multiplier = 2))
//    public String submitAndGetApplicationNumber(JourneyContext context) {
//        long start = System.currentTimeMillis();
//        log.info("[{}] Submitting to PAS | url={}", context.getCorrelationId(), pasUrl);
//
//        try {
//            PasRequest pasRequest = buildPasRequest(context);
////            log.info("PAS_API request : {}",  String.valueOf(objectMapper.writeValueAsString(pasRequest)));
//
//            String token = GenerateNginTokenOSB();
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
//            if(token != null) {
//                headers.add("Authorization", token);
//            }
//            HttpEntity<?> entityGlobal = new HttpEntity<>(pasRequest, headers);
//            
//            log.info("PAS_API request : {}",  String.valueOf(objectMapper.writeValueAsString(entityGlobal)));
//
//            ResponseEntity<PasResponse> res= restTemplate.exchange(pasUrl, HttpMethod.POST, entityGlobal, PasResponse.class);
//            PasResponse response = res.getBody();
//            log.info("PAS_API response : {}", String.valueOf(objectMapper.writeValueAsString(response)));
//            
////            PasResponse response = restTemplate.postForObject(
////                    pasUrl, pasRequest, PasResponse.class);
//
//            long duration = System.currentTimeMillis() - start;
//
//            String applicationNumber = (response != null && response.getAppResponse() != null 
//            	    && response.getAppResponse().getResponse() != null) 
//            	    ? String.valueOf(response.getAppResponse().getResponse().getApplicationId()) 
//            	    : null;
//
//            if (applicationNumber == null || applicationNumber.trim().isEmpty()) {
//                throw new RuntimeException("PAS returned empty applicationNumber");
//            }
//
////            trackingService.logApiCall(context, STAGE, API_NAME,
////                    pasRequest, response, "SUCCESS", null, null, duration);
//
//            log.info("[{}] PAS SUCCESS | applicationNumber={} | {}ms",
//                    context.getCorrelationId(), applicationNumber, duration);
//
//            return applicationNumber;
//
//        } catch (Exception ex) {
//            long duration = System.currentTimeMillis() - start;
//            trackingService.logApiCall(context, STAGE, API_NAME,
//                    null, null, "FAILED", "PAS_ERROR", ex.getMessage(), duration);
//            log.error("[{}] PAS FAILED | {}ms | {}",
//                    context.getCorrelationId(), duration, ex.getMessage());
//            throw new ApiCallException(API_NAME, ex.getMessage(), ex);
//        }
//    }
    
    @Retryable(value = {RuntimeException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2))
 public String submitAndGetApplicationNumber(JourneyContext context) throws Exception {
     long start = System.currentTimeMillis();
     log.info("[{}] PAS Intial Push  (App Generation) | url={}", context.getCorrelationId(), pasUrl);

     try {
         PasRequest pasRequest = buildPasRequest(context);
         
         pasRequest.getRequest().setApplicationId(0L); //TODO Temporary set to 0 for initial push
       
       String token = GenerateNginTokenOSB();

       HttpHeaders headers = new HttpHeaders();
       headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
       if(token != null) {
           headers.add("Authorization", token);
       }
       HttpEntity<?> entityGlobal = new HttpEntity<>(pasRequest, headers);
       
       log.info("PAS_API intial request : {}",  String.valueOf(objectMapper.writeValueAsString(pasRequest)));

       // ── Initial Push: Generate application number ──────────────────────────
       ResponseEntity<PasInitialResponse> firstResponse= restTemplate.exchange(pasUrl, HttpMethod.POST, entityGlobal, PasInitialResponse.class);
       PasInitialResponse initialResponse = firstResponse.getBody();
      
       log.info("PAS_API initial response : {}", String.valueOf(objectMapper.writeValueAsString(initialResponse)));
 //      return "success";

       
//         PasResponse firstResponse = restTemplate.postForObject(
//                 pasUrl, pasRequest, PasResponse.class);

         String applicationId = (firstResponse != null && firstResponse.getBody() != null
         	    && firstResponse.getBody().getResponse() != null)
         	    ? String.valueOf(firstResponse.getBody().getResponse().getApplicationId()) 
         	    : null;

         String proposalNumber = (firstResponse != null && firstResponse.getBody() != null
                 && firstResponse.getBody().getResponse() != null)
                 ? String.valueOf(firstResponse.getBody().getResponse().getProposalNumber())
                 : null;

         if (applicationId == null) {
             throw new RuntimeException("PAS Initial Push returned null applicationId");
         }

         long intialPushDuration = System.currentTimeMillis() - start;
         
         trackingService.logApiCall(context, STAGE, API_NAME + "_INITIAL_PUSH",
                 pasRequest, firstResponse, "SUCCESS", null, null, intialPushDuration);
         
         log.info("[{}] Initial Push PAS SUCCESS | applicationId={} | {}ms",
                 context.getCorrelationId(), applicationId, intialPushDuration);

 // ── Final Push: Submit application with generated applicationId ───────
         long finalPushStart = System.currentTimeMillis();
         log.info("[{}] Final PAS Push(App Submission) | applicationId={}",
                 context.getCorrelationId(), applicationId);

         pasRequest.getRequest().setApplicationId(Long.parseLong(applicationId));
         pasRequest.getRequest().setProposalNumber(proposalNumber);

         HttpEntity<?> entityGlobalFinal = new HttpEntity<>(pasRequest, headers);

         log.info("PAS_API final request : {}",  String.valueOf(objectMapper.writeValueAsString(pasRequest)));

         ResponseEntity<PasResponse> secondResponse= restTemplate.exchange(pasUrl, HttpMethod.POST, entityGlobalFinal, PasResponse.class);

         log.info("PAS_API final response : {}", String.valueOf(objectMapper.writeValueAsString(secondResponse.getBody())));

//         PasResponse secondResponse = restTemplate.postForObject(
//                 pasUrl, pasRequest, PasResponse.class);

         long finalPushDuration = System.currentTimeMillis() - finalPushStart;
         trackingService.logApiCall(context, STAGE, API_NAME + "_FINAL_PUSH",
                 pasRequest, secondResponse, "SUCCESS", null, null, finalPushDuration);
         log.info("[{}] Final PAS Push SUCCESS | applicationId={} | {}ms",
                 context.getCorrelationId(), applicationId, finalPushDuration);

         return String.valueOf(applicationId);

     } catch (Exception ex) {
         long duration = System.currentTimeMillis() - start;
         trackingService.logApiCall(context, STAGE, API_NAME,
                 null, null, "FAILED", "PAS_ERROR", ex.getMessage(), duration);
         log.error("[{}] PAS FAILED | {}ms | {}",
                 context.getCorrelationId(), duration, ex.getMessage());
         throw ex;
     }
 }


    
		@Value("${api.endpoints.generateNginToken}")
		private String generateNginTokenUrl;
	
        public String GenerateNginTokenOSB() {
            
        	HttpHeaders headers = new HttpHeaders();
        	headers.setContentType(MediaType.APPLICATION_JSON);
        	String jsonBody = "{\"serverEnv\":\"uat2\"}";
        	
            log.info("GenerateNginTokenOSB_API request : {}", jsonBody);

            HttpEntity<String> generateNginTokenRequest = new HttpEntity<>(jsonBody, headers);
            String token = null;

            try {
                long start = System.currentTimeMillis();
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(generateNginTokenUrl, generateNginTokenRequest, Map.class);
                if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                    
                	log.info("GenerateNginTokenOSB_API response : {}",  String.valueOf(objectMapper.writeValueAsString(responseEntity.getBody())));

                    token = (String) responseEntity.getBody().get("token");
                    
                    System.out.println("Extracted Token: " + token);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return token; 
        }

    /**
     * Builds PasRequest in two steps:
     *
     * Step 1: Map partner's raw stringvalN params via property file
     *         (mapping/default-PAS_API.properties or partner-specific file)
     *         This fills: firstName, lastName, dob, panNumber, sumAssured, etc.
     *
     * Step 2: Set response fields from prior APIs manually.
     *         These are typed, compile-safe assignments.
     *         Each section is clearly labelled with the source API.
     */
    private PasRequest buildPasRequest(JourneyContext context) {

        // Step 1: Map raw partner params → PasRequest fields via property file
//        PasRequest req = mappingService.resolveAs(
//                context.getPartnerCode(), "PAS_API",
//                context.getRawParams(), PasRequest.class);

    	Map<String, String> params = context.getRawParams();

    	final DateTimeFormatter dateParser = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);


    	PasRequest pasRequest = new PasRequest();
    	Map<String, String> raw = context.getRawParams();

    	// Build header
        PasHeader header = new PasHeader();
        header.setCorrelationId(context.getCorrelationId());
        ProcessVars processVars = new ProcessVars();
        header.setProcessVars(processVars);
        pasRequest.setHeader(header);

        // Step 2: Set context fields
//        req.setCorrelationId(context.getCorrelationId()); //appNo
//        req.setPartnerCode(context.getPartnerCode());

        // Step 4: Set fields from UCS API response
//        if (context.getEdcResult() != null) {
//            req.setCreditScore(context.getEdcResult().getCreditScore());
//            req.setCreditBureau(context.getEdcResult().getCreditBureau());
//        }
        //set fields for each api call

//        return req;

        String stringVal6 = params.get("obj1.stringval6");
        Long applicationId = (stringVal6 != null && !stringVal6.isEmpty())
                             ? Long.parseLong(stringVal6) : null;

        ChapterDetail chapterDetail = new ChapterDetail();
	    chapterDetail.setTemplateCode("string");
	    chapterDetail.setType("NBF_TEMPLATE");

	    List<ChapterDetail> chapterDetails = Collections.singletonList(chapterDetail);


//        PasRequestBody request = new PasRequestBody();
//        String stringVal6 = params.get("obj1.stringval6");
//
//        if (stringVal6 != null && !stringVal6.isEmpty()) {
//            request.setApplicationId(Long.parseLong(stringVal6));
//        }
//        request.setProposalNumber(params.get("obj1.stringval1"));
//        request.setStatus(null);

         BasicPolicyInsured insured = new BasicPolicyInsured();

         if (params.get("obj1.stringval18").equalsIgnoreCase("M")) {
        	 insured.setGender("MALE");
         } else  if (params.get("obj1.stringval18").equalsIgnoreCase("F")) {
        	 insured.setGender("FEMALE");
         }

	     insured.setPartyReferenceId(null);
	     insured.setPolicyInsuredFirstName(params.get("obj1.stringval13"));
	     insured.setPolicyInsuredLastName(params.get("obj1.stringval15"));
	     insured.setPolicyInsuredLegalIdentifierCode(params.get("obj2.stringval102")); //  TODO
//	     insured.setPolicyInsuredLegalIdentifierValue(params.get("obj1.stringval117"));
	     if (params.get("obj2.stringval102").equalsIgnoreCase("AADHAR_REFERENCE_CODE")) {
	    	 insured.setPolicyInsuredLegalIdentifierValue(params.get("obj1.stringval117"));
	     } else if (params.get("obj2.stringval102").equalsIgnoreCase("PAN")) {
	    	 insured.setPolicyInsuredLegalIdentifierValue(params.get("obj1.stringval122"));
	     }

	     insured.setPolicyInsuredMiddleName(params.get("obj1.stringval14"));
	     insured.setSalutation(params.get("obj1.stringval12"));
	     insured.setSuffix(null);
	     insured.setPolicyInsuredDateOfBirth(convertDateForPAS(params.get("obj1.stringval16")));

	     List<BasicPolicyInsured> insuredList = Collections.singletonList(insured);

        	// 2. Build ImportantDates
        ImportantDates importantDates = new ImportantDates();
	        importantDates.setPolicyCheckInDate(null);
	        importantDates.setPolicyIssueDate(dateParser.format(OffsetDateTime.now()));
	        importantDates.setPolicySignedDate(dateParser.format(OffsetDateTime.now()));
	        importantDates.setPolicyReceivedDate(dateParser.format(OffsetDateTime.now()));
	        importantDates.setPolicyYearDate(null);
	        importantDates.setRiskEffectiveDate(null);

	        ProductSelectionDetails productSelection = new ProductSelectionDetails();
	        productSelection.setBaseCoverageCode("L171A01");
	        productSelection.setBranchCode("P00");
	        productSelection.setPolicyIssueState("MH");
	        productSelection.setProductPlan(params.get("obj1.stringval10"));

        	//Define the List of Integrations
        	List<IntegrationDetail> integrations = new ArrayList<>();

        	// 1. CIBIL
        	if(context.getCibilResult()!=null) {
        		IntegrationResults cibilResults = new IntegrationResults();

        		if (context.getCibilResult() != null && context.getCibilResult()[0].getScore() != null &&
        			    !context.getCibilResult()[0].getScore().isEmpty()) {
                	cibilResults.setCibilTuSc3(context.getCibilResult()[0].getScore());

        		}

            	IntegrationDetail cibilDetail = new IntegrationDetail();
            	cibilDetail.setIntegrationName("CIBIL");
            	cibilDetail.setIntegrationStatus(true);
            	cibilDetail.setIntegrationResults(cibilResults);
            	integrations.add(cibilDetail);
        	}

        	// 2. UCS
        	if(context.getUcsResult()!=null) {
        		IntegrationResults ucsResults = new IntegrationResults();

        		if (context.getUcsResult() != null && context.getUcsResult().getTranxResponse() != null &&
        				context.getUcsResult().getTranxResponse().getCustomers() != null &&
        			    !context.getUcsResult().getTranxResponse().getCustomers().isEmpty()) {

        			    ucsResults.setSuc(context.getUcsResult().getTranxResponse().getCustomers().get(0).getSuc());
        			    ucsResults.setPdabSuc(null);
                    	ucsResults.setFibSuc(null);
                    	ucsResults.setLifeSuc(context.getUcsResult().getTranxResponse().getCustomers().get(0).getLifeSuc());
                    	ucsResults.setHiSuc(null);
                    	ucsResults.setAdbSuc(context.getUcsResult().getTranxResponse().getCustomers().get(0).getAdbsuc());
                    	ucsResults.setTasa(context.getUcsResult().getTranxResponse().getCustomers().get(0).getTasa());
                    	ucsResults.setCibSuc(context.getUcsResult().getTranxResponse().getCustomers().get(0).getCibsuc());
                    	ucsResults.setWopSuc(null);
                    	ucsResults.setTibSuc(null);
                    	ucsResults.setTap(context.getUcsResult().getTranxResponse().getCustomers().get(0).getTap());
                    	ucsResults.setAdbTasa(context.getUcsResult().getTranxResponse().getCustomers().get(0).getAdbtasa());
                    	ucsResults.setAptpdTasa(context.getUcsResult().getTranxResponse().getCustomers().get(0).getAptpdtasa());
        			}

            	IntegrationDetail ucsDetail = new IntegrationDetail();
            	ucsDetail.setIntegrationName("UCS");
            	ucsDetail.setIntegrationStatus(true);
            	ucsDetail.setIntegrationResults(ucsResults);
            	integrations.add(ucsDetail);
        	}

        	// 3. FES
        	if(context.getFesResult()!=null) {
        		IntegrationResults fesResults = new IntegrationResults();
            	fesResults.setFixedDepositOrMutualFund("0");
            	fesResults.setMonthlySip("0");
            	fesResults.setMonthlyCreditCard("0");
            	fesResults.setPropertyLoan("0");
            	fesResults.setPremiumPayingCapacity("500000");
            	fesResults.setFinancialEligibilityBasedOnCibil("0");

            	IntegrationDetail fesDetail = new IntegrationDetail();
            	fesDetail.setIntegrationName("FES");
            	fesDetail.setIntegrationStatus(true);
            	fesDetail.setIntegrationResults(fesResults);
            	integrations.add(fesDetail);
        	}

        	// 4. MRS
        	if(context.getMrsResult()!=null) {
    			IntegrationResults mrsResults = new IntegrationResults();

        		if(context.getMrsResult().getMedicalDetailList()!=null && !context.getMrsResult().getMedicalDetailList().isEmpty() && context.getMrsResult().getMedicalDetailList().get(0).getMedicalFlag()!=null) {
                	mrsResults.setNonMedical(context.getMrsResult().getMedicalDetailList().get(0).getMedicalFlag());
        		}

            	IntegrationDetail mrsDetail = new IntegrationDetail();
            	mrsDetail.setIntegrationName("MRS");
            	mrsDetail.setIntegrationStatus(true);
            	mrsDetail.setIntegrationResults(mrsResults);
            	integrations.add(mrsDetail);
        	}

        	// 5. DCS
        	if(context.getDcsResult()!=null) {

        		Map<String, String> tranxDetailMap = new HashMap<>();
        		if(context.getDcsResult() != null &&
        			    context.getDcsResult().getTranxDetailList() != null &&
        			    !context.getDcsResult().getTranxDetailList().isEmpty()) {

        			List<TransactionDetail> list = context.getDcsResult().getTranxDetailList();
        		    for (TransactionDetail item : list) {
        		        if (item != null && item.getId() != null) {
        		            try {
        		                String jsonStr = objectMapper.writeValueAsString(item);
        		                tranxDetailMap.put(item.getId(), jsonStr);
        		            } catch (JsonProcessingException e) {
        		                // Handle serialization error for this specific item
        		            }
        		        }
        		    }
        		}
        		String d032 = tranxDetailMap.get("D032");
	        	String m253 = tranxDetailMap.get("M253");
	        	String m1177 = tranxDetailMap.get("M1177");
	        	String M1122 = tranxDetailMap.get("M1122");
	        	String M73 = tranxDetailMap.get("M73");

        		IntegrationResults dcsResults = new IntegrationResults();
            	dcsResults.setAddressProofOfDrivingLicense(m1177);
            	dcsResults.setPanCardOfAssured(m253);
//            	dcsResults.setFullLengthPhotograph(d032);
            	dcsResults.setIdentityProofOfLifeAssured(M1122);
//            	dcsResults.setCopyOfCheque(M73);
            	if(context.getEkycResult()!=null) {
                	dcsResults.setEkycPhAadhaarPhoto("{\"id\":\"\",\"role\":\"PH\",\"category\":\"KYC\",\"filename\":\"EKYC PH Aadhaar Photo.JPG\",\"doctype\":\"EKYC PH\",\"docSubmissionMode\":\"Digital\",\"description\":\"EKYC PH Aadhaar Photo\",\"shortdescription\":\"EKYC PH Aadhaar Photo\"}");
            	}
            	dcsResults.setBi("{\"id\":\"BI\",\"role\":\"Life Assured\",\"category\":\"BI\",\"filename\":\"BI.pdf\",\"doctype\":\"BI\",\"docSubmissionMode\":\"DIGITAL\",\"description\":\"BI.pdf\",\"shortdescription\":\"Benefit Illustration\"}");
            	dcsResults.setProposalForm("{\"id\":\"PF\",\"role\":\"Life Assured\",\"category\":\"PROP\",\"filename\":\"Proposal Form.pdf\",\"doctype\":\"Proposal form\",\"docSubmissionMode\":\"DIGITAL\",\"description\":\"Proposal Form.pdf\",\"shortdescription\":\"Proposal Form\"}");
            	dcsResults.setDynamicQuestionaire("{\"id\":\"DQSPDFTEMPLATE\",\"role\":\"Life Assured\",\"category\":\"Questionaire\",\"filename\":\"Dynamic Questionaire.PDF\",\"doctype\":\"Questionaire\",\"docSubmissionMode\":\"Digital\",\"description\":\"DQSPDFTEMPLATE\",\"shortdescription\":\"DQSPDFTEMPLATE\"}");
            	dcsResults.setReceipt("{\"id\":\"RECEIPT\",\"role\":\"Life Assured\",\"category\":\"RECEIPT\",\"filename\":\"RECEIPT.PDF\",\"doctype\":\"RECEIPT\",\"docSubmissionMode\":\"Digital\",\"description\":\"RECEIPT\",\"shortdescription\":\"RECEIPT\"}");

            	IntegrationDetail dcsDetail = new IntegrationDetail();
            	dcsDetail.setIntegrationName("DCS");
            	dcsDetail.setIntegrationStatus(true);
            	dcsDetail.setIntegrationResults(dcsResults);
            	integrations.add(dcsDetail);
        		}

        	// 6. AUW
        	if(context.getAuwResult()!=null) {
            	IntegrationResults awsResults = new IntegrationResults();
        		if(context.getAuwResult().getTranxResponse()!=null && context.getAuwResult().getTranxResponse().getStpFlag()!=null &&
        				!context.getAuwResult().getTranxResponse().getStpFlag().isEmpty() &&
        				context.getAuwResult().getTranxResponse().getStpFlag().get(0).getFlag()!=null &&
        				context.getAuwResult().getTranxResponse().getStpFlag().get(0).getFlag().isEmpty()) {

                	awsResults.setAuwResults(context.getAuwResult().getTranxResponse().getStpFlag().get(0).getFlag());
        		}
        		awsResults.setAuwResults("NONSTP");

            	IntegrationDetail awsDetail = new IntegrationDetail();
            	awsDetail.setIntegrationName("AWS");
            	awsDetail.setIntegrationStatus(true);
            	awsDetail.setIntegrationResults(awsResults);
            	integrations.add(awsDetail);
        	}

        	// 7. PAN
        	PanDetail panDetail = new PanDetail();
        	panDetail.setPan("ECBPS1617E");
        	panDetail.setPanStatus("E");
        	panDetail.setNameStatus("Y");
        	panDetail.setFatherNameStatus(null);
        	panDetail.setDobStatus("Y");
        	panDetail.setSeedingStatus("Y");

        	NewPanStatus newPanStatus = new NewPanStatus();
        	newPanStatus.setIpPanStatus(panDetail);
        	newPanStatus.setPhPanStatus(panDetail);
        	newPanStatus.setPayerPanStatus(panDetail);

        	IntegrationResults panResults = new IntegrationResults();
        	panResults.setNewPanStatus(newPanStatus);
        	panResults.setIp("Not_verified");
        	panResults.setPh("No");
        	panResults.setPayer("Not_verified");

        	IntegrationDetail panDetailEntry = new IntegrationDetail();
        	panDetailEntry.setIntegrationName("PAN Verification Status");
        	panDetailEntry.setIntegrationStatus(true);
        	panDetailEntry.setIntegrationResults(panResults);
        	integrations.add(panDetailEntry);

        	// 8. EDC
        	if(context.getEdcResult()!=null) {
        		IntegrationResults edcResults = new IntegrationResults();
            	edcResults.setAppNo(context.getEdcResult().getAppNo());
            	edcResults.setEdcScore(context.getEdcResult().getEdcScore());
            	edcResults.setEdcAlert(context.getEdcResult().getEdcAlert());
            	edcResults.setAlertReason(context.getEdcResult().getAlertReason());
            	edcResults.setErrorCode(context.getEdcResult().getErrorCode());
            	edcResults.setRemark(context.getEdcResult().getRemark());
            	edcResults.setTmpStmp(context.getEdcResult().getTmpStmp());
            	edcResults.setModelAlert(context.getEdcResult().getModelAlert());
            	edcResults.setPersistencyScore(context.getEdcResult().getPersistencyScore());
            	edcResults.setPersitencyAlert(context.getEdcResult().getPersitencyAlert());
            	edcResults.setPersistencyRemarks(context.getEdcResult().getPersistencyRemarks());
            	edcResults.setProductReco(context.getEdcResult().getProductReco());
            	edcResults.setTicketSizeReco(context.getEdcResult().getTicketSizeReco());
            	edcResults.setQualityScore(context.getEdcResult().getQualityScore());
            	edcResults.setQualityAlert(context.getEdcResult().getQualityAlert());
            	edcResults.setQualityRemarks(context.getEdcResult().getQualityRemarks());
            	edcResults.setIncomeSegment(context.getEdcResult().getIncomeSegment());
            	edcResults.setDigitalProfileScore(context.getEdcResult().getDigitalProfileScore());
            	edcResults.setDigitalProfileAlert(context.getEdcResult().getDigitalProfileAlert());
            	edcResults.setBureauMatch(context.getEdcResult().getBureauMatch());
            	edcResults.setBureauScore(context.getEdcResult().getBureauScore());
            	edcResults.setBureauProfile(context.getEdcResult().getBureauProfile());
            	edcResults.setPlaceeholder1(context.getEdcResult().getPlaceeholder1());
            	edcResults.setPlaceholder2(context.getEdcResult().getPlaceholder2());
            	edcResults.setPlaceholder3(context.getEdcResult().getPlaceholder3());
            	edcResults.setPlaceholder4(context.getEdcResult().getPlaceholder4());
            	edcResults.setIsHighRisk(context.getEdcResult().getIsHighRisk());

            	IntegrationDetail edcDetail = new IntegrationDetail();
            	edcDetail.setIntegrationName("EDC");
            	edcDetail.setIntegrationStatus(true);
            	edcDetail.setIntegrationResults(edcResults);
            	integrations.add(edcDetail);
        	}

        	BankDetailsDTO bankDetailsDTO = new BankDetailsDTO();
        	bankDetailsDTO.setAccountHolderName(params.get("obj1.stringval149"));
        	bankDetailsDTO.setAccountNo(params.get("obj2.stringval32"));
        	bankDetailsDTO.setAccountType(params.get("obj2.stringval35"));
        	bankDetailsDTO.setBankName(params.get("obj2.stringval31"));
        	bankDetailsDTO.setBranchName(params.get("obj2.stringval31"));
        	bankDetailsDTO.setIfscCode(params.get("obj2.stringval36"));
        	bankDetailsDTO.setModeOfPayment(params.get("obj2.stringval23"));
        String pennydrop = "";
        if(context.getPennyDropResult()!=null && context.getPennyDropResult().getStatus() != null && context.getPennyDropResult().getStatus().isEmpty()) {
            pennydrop = context.getPennyDropResult().getStatus();
        }
            if(pennydrop.equalsIgnoreCase("SUCCESS")) {
                bankDetailsDTO.setIsPennDropSuccessful("Yes");
            }else {
                bankDetailsDTO.setIsPennDropSuccessful("No");
            }
        	bankDetailsDTO.setIsNameVerified("No");

        	BiSummary biSummary = new BiSummary();

        	// IP ---- ippersonalDetails
        	PhoneNumber ipAlternateMobileNumber = new PhoneNumber();
        	ipAlternateMobileNumber.setCountryCode(null);
        	ipAlternateMobileNumber.setNumber(params.get("obj3.stringval16"));
//        	ipAlternateMobileNumber.setType(dateParser.format(OffsetDateTime.now()));
        	ipAlternateMobileNumber.setType("");


        	EmailId ipEmail = new EmailId();
        	if(context.getEkycResult()!=null && context.getEkycResult().getCommAddrLine2() != null && context.getEkycResult().getCommAddrLine2().isEmpty()) {
        		ipEmail.setAddress(context.getEkycResult().getCommAddrLine2());
            }else {
            	ipEmail.setAddress(params.get("obj1.stringval20"));
            }
        	SocialMediaId ipFaceBook = new SocialMediaId();
        	ipFaceBook.setId(null);

        	SocialMediaId ipLinkedInId = new SocialMediaId();
        	ipLinkedInId.setId(null);

        	PhoneNumber ipMobile = new PhoneNumber();
        	ipMobile.setCountryCode(null);
        	if(context.getEkycResult()!=null && context.getEkycResult().getMobileNo() != null && context.getEkycResult().getMobileNo().isEmpty()) {
        		ipMobile.setNumber(context.getEkycResult().getMobileNo());
            }else {
            	ipMobile.setNumber(params.get("obj1.stringval19"));
            }
        	ipMobile.setType(null);

        	SocialMediaId ipSkype = new SocialMediaId();
        	ipSkype.setId(null);

        	PhoneNumber ipTelNo = new PhoneNumber();
        	ipTelNo.setCountryCode(null);
        	ipTelNo.setNumber(params.get("obj1.stringval63"));
        	ipTelNo.setStdCode(null);
        	ipTelNo.setType(null);

        	SocialMediaId ipTwitterId = new SocialMediaId();
        	ipTwitterId.setId(null);

        	PhoneNumber ipWhatsApp = new PhoneNumber();
        	ipWhatsApp.setCountryCode(null);
        	ipWhatsApp.setNumber(null);
        	ipWhatsApp.setType(null);

        	ContactDetails ipContact = new ContactDetails();
        	ipContact.setEmailId(ipEmail);
        	ipContact.setAlternateMobileNumber(ipAlternateMobileNumber);
        	ipContact.setFaceBook(ipFaceBook);
        	ipContact.setLinkedInId(ipLinkedInId);
        	ipContact.setMobileNumber(ipMobile);
        	ipContact.setSkype(ipSkype);
        	ipContact.setTelNo(ipTelNo);
        	ipContact.setTwitterId(ipTwitterId);
        	ipContact.setWhatsApp(ipWhatsApp);

        	PersonDetails ipPersonDetails = new PersonDetails();
            ipPersonDetails.setAge((params.get("obj1.stringval17")) != null && !(params.get("obj1.stringval17")).isEmpty() ? Integer.parseInt((params.get("obj1.stringval17"))) : null);

            ipPersonDetails.setCountryOfBirth(null);
            ipPersonDetails.setCountryOfResidence(params.get("obj1.stringval52"));
            ipPersonDetails.setFatherName(params.get("obj1.stringval102"));
            ipPersonDetails.setFirstName(params.get("obj1.stringval13"));

           // ipPersonDetails.setGender(params.get("obj1.stringval18"));

            if(context.getEkycResult()!=null && context.getEkycResult().getGender() != null && context.getEkycResult().getGender().isEmpty()) {
            	if (context.getEkycResult().getGender().equalsIgnoreCase("M")) {
                	ipPersonDetails.setGender("MALE");
                } else  if (context.getEkycResult().getGender().equalsIgnoreCase("F")) {
                	ipPersonDetails.setGender("FEMALE");
                }
            }else {
            	if (params.get("obj1.stringval18").equalsIgnoreCase("M")) {
                	ipPersonDetails.setGender("MALE");
                } else  if (params.get("obj1.stringval18").equalsIgnoreCase("F")) {
                	ipPersonDetails.setGender("FEMALE");
                }
            }

            ipPersonDetails.setLastName(params.get("obj1.stringval15"));
            ipPersonDetails.setMiddleName(params.get("obj1.stringval14"));
            //ipPersonDetails.setMaritalStatus(params.get("obj1.stringval99"));

            if ("M".equals(params.get("obj1.stringval99"))) {
            	ipPersonDetails.setMaritalStatus("Married");
            } else {
            	ipPersonDetails.setMaritalStatus(params.get("obj1.stringval99"));
            }

            ipPersonDetails.setMotherName(params.get("obj3.stringval110"));
            ipPersonDetails.setNameOfSpouse(params.get("obj1.stringval103"));
            ipPersonDetails.setNationality(params.get("obj1.stringval51"));
            ipPersonDetails.setPlaceOfBirth(params.get("obj1.stringval53"));
            ipPersonDetails.setSalutation(params.get("obj1.stringval12"));
            ipPersonDetails.setSuffix(null);
            ipPersonDetails.setPan(params.get("obj1.stringval122"));
//            ipPersonDetails.setIdProofDoc("PAN");
//            ipPersonDetails.setIdProofValue(params.get("obj1.stringval122"));

	   	     if (params.get("obj2.stringval102").equalsIgnoreCase("AADHAR_REFERENCE_CODE")) {
		            ipPersonDetails.setIdProofDoc("AADHAR_REFERENCE_CODE");
		            ipPersonDetails.setIdProofValue(params.get("obj1.stringval117"));

		     } else if (params.get("obj2.stringval102").equalsIgnoreCase("PAN")) {
		            ipPersonDetails.setIdProofDoc("PAN");
		            ipPersonDetails.setIdProofValue(params.get("obj1.stringval122"));
		     }

	   	     if(context.getEkycResult()!=null && context.getEkycResult().getDateOfBirth() != null && context.getEkycResult().getDateOfBirth().isEmpty()) {
	   	    	ipPersonDetails.setDateOfBirth(context.getEkycResult().getDateOfBirth());
	   	     }else {
	             ipPersonDetails.setDateOfBirth(convertDateForPAS(params.get("obj1.stringval16")));
	   	     }

            BiIpPersonalDetails biIpPersonalDetails = new BiIpPersonalDetails();
            biIpPersonalDetails.setIpContactDetails(ipContact);
            biIpPersonalDetails.setIpPersonalDetails(ipPersonDetails);
            biSummary.setIppersonalDetails(biIpPersonalDetails);

            InvestDetails investDetails = new InvestDetails();
            investDetails.setAmountInstallmentpremium(params.get("obj1.stringval37"));
            investDetails.setBenefitCode(params.get("list1[0].stringval1"));

            if (context != null && context.getBiResult() != null) {
            	investDetails.setBiNumber(context.getBiResult().getQuotationId());
            }
//            investDetails.setBiNumber(params.get("obj3.stringval3"));

            List<FundOptedFor> fundOptedFor = new ArrayList<>();
            FundOptedFor fundList = new FundOptedFor();
            fundList.setFundCode(params.get("list3[0].stringval1"));
            fundList.setFundName(params.get("list3[0].stringval1"));
            fundList.setPercentageInvested(params.get("list3[0].stringval2"));
            fundOptedFor.add(fundList);

            investDetails.setFundOptedFor(fundOptedFor);
            investDetails.setInvestmentStrategy(params.get("obj1.stringval42"));
            investDetails.setFrequency(params.get("obj1.stringval39"));
            investDetails.setPolicyTerm("");
            investDetails.setPremiumPaymentTerm(params.get("obj1.stringval36"));
            investDetails.setViewBIPDF(params.get("obj2.stringval109"));
            investDetails.setFamilyBenefitPolicyNumber("");
            investDetails.setFamilyBenefitFlag("");
            investDetails.setFamilyBenefitRelationship("");
            investDetails.setLifeBenefitOption("");
            investDetails.setMaturityBenefitOption("");
            investDetails.setBiDate("");
            investDetails.setBiReceivedDate("");

            biSummary.setNvestDetails(investDetails);

            BiPhPersonalDetails biPhPersonalDetails = new BiPhPersonalDetails();

            // PH ----- phPersonalDetails
            PhoneNumber phAlternateMobileNumber = new PhoneNumber();
            phAlternateMobileNumber.setCountryCode(null);
            phAlternateMobileNumber.setNumber(params.get("obj3.stringval93"));
            phAlternateMobileNumber.setType("");

        	EmailId phEmail = new EmailId();
        	phEmail.setAddress(params.get("obj1.stringval29"));

        	SocialMediaId phFaceBook = new SocialMediaId();
        	phFaceBook.setId(null);

        	SocialMediaId phLinkedInId = new SocialMediaId();
        	phLinkedInId.setId(null);

        	PhoneNumber phMobile = new PhoneNumber();
        	ipMobile.setCountryCode(null);
        	ipMobile.setNumber(params.get("obj1.stringval28"));
        	ipMobile.setType(null);

        	SocialMediaId phSkype = new SocialMediaId();
        	phSkype.setId(null);

        	PhoneNumber phTelNo = new PhoneNumber();
        	phTelNo.setCountryCode(null);
        	phTelNo.setNumber(params.get("obj1.stringval87"));
        	phTelNo.setStdCode(null);
        	phTelNo.setType(null);

        	SocialMediaId phTwitterId = new SocialMediaId();
        	phTwitterId.setId(null);

        	PhoneNumber phWhatsApp = new PhoneNumber();
        	phWhatsApp.setCountryCode(null);
        	phWhatsApp.setNumber(null);
        	phWhatsApp.setType(null);

        	ContactDetails phContact = new ContactDetails();
        	phContact.setEmailId(ipEmail);
        	phContact.setAlternateMobileNumber(phAlternateMobileNumber);
        	phContact.setFaceBook(phFaceBook);
        	phContact.setLinkedInId(phFaceBook);
        	phContact.setMobileNumber(phMobile);
        	phContact.setSkype(phSkype);
        	phContact.setTelNo(phTelNo);
        	phContact.setTwitterId(phTwitterId);
        	phContact.setWhatsApp(phWhatsApp);

        	PersonDetails phPersonalDetails = new PersonDetails();
            phPersonalDetails.setAge((params.get("obj1.stringval26")) != null && !(params.get("obj1.stringval26")).isEmpty() ? Integer.parseInt((params.get("obj1.stringval26"))) : null);
            phPersonalDetails.setCountryOfBirth(null);
            phPersonalDetails.setCountryOfResidence(params.get("obj1.stringval76"));
            phPersonalDetails.setFatherName(params.get("obj1.stringval111"));
            phPersonalDetails.setFirstName(params.get("obj1.stringval22"));

            // phPersonalDetails.setGender(params.get("obj1.stringval27"));
            if(context.getEkycResult()!=null && context.getEkycResult().getGender() != null && context.getEkycResult().getGender().isEmpty()) {
            	if (context.getEkycResult().getGender().equalsIgnoreCase("M")) {
                	ipPersonDetails.setGender("MALE");
                } else  if (context.getEkycResult().getGender().equalsIgnoreCase("F")) {
                	ipPersonDetails.setGender("FEMALE");
                }
            }else {
                if (params.get("obj1.stringval27").equalsIgnoreCase("M")) {
                	phPersonalDetails.setGender("MALE");
                } else  if (params.get("obj1.stringval27").equalsIgnoreCase("F")) {
                	phPersonalDetails.setGender("FEMALE");
                }
            }

            phPersonalDetails.setLastName(params.get("obj1.stringval24"));
            phPersonalDetails.setMiddleName(params.get("obj1.stringval23"));
            //phPersonalDetails.setMaritalStatus(params.get("obj1.stringval108"));

            if ("M".equals(params.get("obj1.stringval108"))) {
            	phPersonalDetails.setMaritalStatus("Married");
            } else {
            	phPersonalDetails.setMaritalStatus(params.get("obj1.stringval108"));
            }


            phPersonalDetails.setMotherName(params.get("obj3.stringval112"));
            phPersonalDetails.setNameOfSpouse(params.get("obj1.stringval112"));
            phPersonalDetails.setNationality(params.get("obj1.stringval75"));
            phPersonalDetails.setPlaceOfBirth(params.get("obj1.stringval77"));
            phPersonalDetails.setSalutation(params.get("obj1.stringval21"));
            phPersonalDetails.setSuffix(null);
            phPersonalDetails.setPan(params.get("obj1.stringval122"));
//            phPersonalDetails.setIdProofDoc("PAN");
//            phPersonalDetails.setIdProofValue(params.get("obj1.stringval122"));

	   	     if (params.get("obj2.stringval107").equalsIgnoreCase("AADHAR_REFERENCE_CODE")) {
	   	    	 phPersonalDetails.setIdProofDoc("AADHAR_REFERENCE_CODE");
	   	    	 phPersonalDetails.setIdProofValue(params.get("obj3.stringval96"));

		     } else if (params.get("obj2.stringval107").equalsIgnoreCase("PAN")) {
		    	 phPersonalDetails.setIdProofDoc("PAN");
		    	 phPersonalDetails.setIdProofValue(params.get("obj1.stringval126"));
		     }

	   	  if(context.getEkycResult()!=null && context.getEkycResult().getDateOfBirth() != null && context.getEkycResult().getDateOfBirth().isEmpty()) {
	   	    	ipPersonDetails.setDateOfBirth(context.getEkycResult().getDateOfBirth());
	   	     }else {
	   	    	 phPersonalDetails.setDateOfBirth(convertDateForPAS(params.get("obj1.stringval25")));
	   	     }

            biPhPersonalDetails.setPhContactDetails(phContact);
            biPhPersonalDetails.setPhPersonalDetails(phPersonalDetails);
            biSummary.setPhPersonalDetails(biPhPersonalDetails);

            // ---------------- declarationUnderFATCADTO mapping not done ------------------

            EiaDTO eiaDTO = new EiaDTO();
            eiaDTO.setApplicationNumber(params.get("obj1.stringval6"));
            eiaDTO.setEiaAccountNumber(params.get("obj3.stringval135"));
            eiaDTO.setEiaFlag((params.get("obj1.stringval119")) != null && !(params.get("obj1.stringval119")).trim().isEmpty() ? Boolean.parseBoolean((params.get("obj1.stringval119"))) : null);

            String eiaAccountNo = params.get("obj3.stringval135");
            String irAccountType = (eiaAccountNo != null && !eiaAccountNo.trim().isEmpty()) ? "Existing" : "New";

            eiaDTO.setIrAccountType(irAccountType);
            eiaDTO.setIrNameType(params.get("obj3.stringval114"));


            IpFamilyDetailsDTO ipFamilyDetailsDTO = new IpFamilyDetailsDTO();
            List<FamilyStatusList> familyStatusLists = new ArrayList<>();
            int howManyFamilyMemberAgedBelow50 = 0;

            // Extract all indices present in the params map for list5.WeoRecStrings150User
            List<Integer> indices = params.keySet().stream()
                    .filter(k -> k.startsWith("list5[") && k.contains(".stringval1"))
                    .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            for (Integer i : indices) {
                String baseKey = "list5[" + i + "]";
                String relationship = params.get(baseKey + ".stringval1");
                String ageStr = params.get(baseKey + ".stringval2");
                String healthStatus = params.get(baseKey + ".stringval3");
                String causeOfDeath = params.get(baseKey + ".stringval5");

                FamilyStatusList familyMember = new FamilyStatusList();
                familyMember.setRelationship(relationship);
                familyMember.setAge(ageStr);
                familyMember.setHealthStatus(healthStatus);
                familyMember.setCauseOfDeath(causeOfDeath);

                if (ageStr != null && !ageStr.trim().isEmpty()) {
                    try {
                        int ageVal = Integer.parseInt(ageStr.trim());
                        if (ageVal < 50) {
                            howManyFamilyMemberAgedBelow50++;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid ages
                    }
                }

                familyStatusLists.add(familyMember);
            }

            ipFamilyDetailsDTO.setFamilyAdverseHistory("false");
            ipFamilyDetailsDTO.setFamilyStatusList(familyStatusLists);
            ipFamilyDetailsDTO.setHowManyFamilyMemberAgedBelow50(howManyFamilyMemberAgedBelow50);
            ipFamilyDetailsDTO.setSendBIPropoasalFormForReview(null);

            HabbitDetailsDTO habbitDetailsDTO = new HabbitDetailsDTO();
            habbitDetailsDTO.setBmi(null);
            habbitDetailsDTO.setHeight((params.get("obj2.stringval46")) != null && !(params.get("obj2.stringval46")).isEmpty() ? Double.parseDouble((params.get("obj2.stringval46"))) : null);
            habbitDetailsDTO.setWeight((params.get("obj2.stringval47")) != null && !(params.get("obj2.stringval47")).isEmpty() ? Double.parseDouble((params.get("obj2.stringval47"))) : null);
            habbitDetailsDTO.setIsAlcohol(false);
            habbitDetailsDTO.setIsChangeInWeight(false);
            habbitDetailsDTO.setIsDGH(false);
            habbitDetailsDTO.setIsPEP(false);
            habbitDetailsDTO.setIsSmoker(false);
            habbitDetailsDTO.setIsTobacco(false);


            IccrDTO iccrDTO = new IccrDTO();
            iccrDTO.setAddress(null);
            iccrDTO.setAge((params.get("obj1.stringval17")) != null && !(params.get("obj1.stringval17")).isEmpty() ? Integer.parseInt((params.get("obj1.stringval17"))) : null);
            iccrDTO.setAgentDetails(null);
            iccrDTO.setChannel(null);
            iccrDTO.setFscOrIcCode(params.get("obj1.stringval4"));
            iccrDTO.setFscOrIcName(params.get("obj2.stringval136"));
            iccrDTO.setFscOrIcClub(null);
            iccrDTO.setGuidelines(null);
            iccrDTO.setHandicappedDescription(params.get("obj3.stringval140"));
            iccrDTO.setHowlongYouKnowLA(params.get("obj3.stringval138"));
            iccrDTO.setId(null);
            iccrDTO.setIncome(params.get("obj1.stringval130"));
            iccrDTO.setIsHandicaped(iccrDTO.getHandicappedDescription() != null && !iccrDTO.getHandicappedDescription().equals("null") ? "true" : "false");
            iccrDTO.setIsRelatedOrEmpOfBalic(params.get("obj3.stringval139"));
            iccrDTO.setLeadByCode(params.get("obj3.stringval101"));

            String firstName = params.get("obj1.stringval13");
            String middleName = params.get("obj1.stringval14");
            String lastName = params.get("obj1.stringval15");
            StringBuilder nameBuilder = new StringBuilder();
            if (firstName != null && !firstName.trim().isEmpty() && !firstName.equalsIgnoreCase("null")) {
                nameBuilder.append(firstName.trim());
            }
            if (middleName != null && !middleName.trim().isEmpty() && !middleName.equalsIgnoreCase("null")) {
                if (nameBuilder.length() > 0) nameBuilder.append(" ");
                nameBuilder.append(middleName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty() && !lastName.equalsIgnoreCase("null")) {
                if (nameBuilder.length() > 0) nameBuilder.append(" ");
                nameBuilder.append(lastName.trim());
            }
            iccrDTO.setNameOfLA(nameBuilder.toString());
            iccrDTO.setOccupation(null);
            iccrDTO.setOthers(null);
            iccrDTO.setProposalAcceptance(null);
            iccrDTO.setProposedInsured(null);
            iccrDTO.setRelationshipRefCode(null);
            iccrDTO.setRiskAssociated(null);
            iccrDTO.setSpCode(params.get("obj3.stringval103"));
            iccrDTO.setSpName(null);
            iccrDTO.setSubIdCode(params.get("obj3.stringval104"));
            iccrDTO.setSumAssured(params.get("obj1.stringval40"));
            iccrDTO.setValidDataEntered(null);
            iccrDTO.setEmpCode(null);
            iccrDTO.setRmContact(null);
            iccrDTO.setRmEmail(null);
            iccrDTO.setRmName(null);
            iccrDTO.setDistributionChannel(null);
            iccrDTO.setPartnerId(null);
            iccrDTO.setAccountType(null);
            iccrDTO.setTrackId(null);
            iccrDTO.setSubVertical(null);
            iccrDTO.setBsoCode(null);
            iccrDTO.setDohCode(null);
            iccrDTO.setBsoName(null);
            iccrDTO.setStmCode(null);
            iccrDTO.setIsPOS(null);
            iccrDTO.setIsBsoBypassed(null);
            iccrDTO.setBsoChannel(null);
            iccrDTO.setDuid(null);
            iccrDTO.setIssueLater(null);
            iccrDTO.setCisNumber(null);
            iccrDTO.setCommissionType(null);
            iccrDTO.setIssuanceGridApplicable(null);
            iccrDTO.setByPassFlag(null);
            iccrDTO.setAgentsClub(null);
            iccrDTO.setVertical(null);
            iccrDTO.setIsCommissionPayable(null);

            IpDetails ipDetails = new IpDetails();

            ipDetails.setAdditionalAddressDetails(null);

            Address currentAddress = new Address();
            currentAddress.setAddressType("Current");
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarSubdist() != null && context.getEkycResult().getAadhaarSubdist().isEmpty()) {
            	currentAddress.setCityOrVillage(context.getEkycResult().getAadhaarSubdist());
            }else {
                currentAddress.setCityOrVillage(params.get("obj1.stringval61"));
            }
            currentAddress.setCo(null);
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarCountry() != null && context.getEkycResult().getAadhaarCountry().isEmpty()) {
            	currentAddress.setCountry(context.getEkycResult().getAadhaarCountry());
            }else {
                currentAddress.setCountry(params.get("obj2.stringval149"));
            }
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarDist() != null && context.getEkycResult().getAadhaarDist().isEmpty()) {
            	currentAddress.setDistrict(context.getEkycResult().getAadhaarDist());
            }else {
            	currentAddress.setDistrict(params.get("obj1.stringval61"));
            }
            currentAddress.setFlag(null);
            if(context.getEkycResult()!=null && context.getEkycResult().getCommAddrLine1() != null && context.getEkycResult().getCommAddrLine1().isEmpty()) {
            	currentAddress.setFlatOrDoorNo(context.getEkycResult().getCommAddrLine1());
            }else {
                currentAddress.setFlatOrDoorNo(params.get("obj1.stringval56"));
            }
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarLandmark() != null && context.getEkycResult().getAadhaarLandmark().isEmpty()) {
            	currentAddress.setLandmark(context.getEkycResult().getAadhaarLandmark());
            }else {
            	currentAddress.setLandmark(params.get("obj1.stringval59"));
            }
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarCareof() != null && context.getEkycResult().getAadhaarCareof().isEmpty()) {
            	currentAddress.setNameOfPremises(context.getEkycResult().getAadhaarCareof());
            }else {
            	currentAddress.setNameOfPremises(params.get("obj1.stringval57"));
            }
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarPin() != null && context.getEkycResult().getAadhaarPin().isEmpty()) {
            	currentAddress.setPinCode(context.getEkycResult().getAadhaarPin());
            }else {
                currentAddress.setPinCode(params.get("obj1.stringval55"));
            }
            currentAddress.setPlace(params.get("obj1.stringval60"));
            currentAddress.setPoliceStation(null);
            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarPo() != null && context.getEkycResult().getAadhaarPo().isEmpty()) {
            	currentAddress.setPostOrAreaOrNagar(context.getEkycResult().getAadhaarPo());
            }else {
            	currentAddress.setPostOrAreaOrNagar(null);
            }
            if(context.getEkycResult()!=null && context.getEkycResult().getCommAddrLine2() != null && context.getEkycResult().getCommAddrLine2().isEmpty()) {
            	currentAddress.setRoadOrStreetOrLane(context.getEkycResult().getCommAddrLine2());
            }else {
            	currentAddress.setRoadOrStreetOrLane(params.get("obj1.stringval58"));
            }

            if(context.getEkycResult()!=null && context.getEkycResult().getAadhaarState() != null && context.getEkycResult().getAadhaarState().isEmpty()) {
            	currentAddress.setState(context.getEkycResult().getAadhaarState());
            }else {
                currentAddress.setState(params.get("obj1.stringval62"));

            }
            currentAddress.setTownOrSuburbOrTaluka(null);
            currentAddress.setAddressSameAs(null);
            currentAddress.setResidingSince(null);
            currentAddress.setAddressProofDocumentType(null);

            Address permanentAddress = new Address();
            permanentAddress.setAddressType("Permanent");
            permanentAddress.setCityOrVillage(params.get("obj1.stringval71"));
            permanentAddress.setCo(null);
            permanentAddress.setCountry(params.get("obj2.stringval149"));
            permanentAddress.setDistrict(params.get("obj1.stringval71"));
            permanentAddress.setFlag(null);
            permanentAddress.setFlatOrDoorNo(params.get("obj1.stringval66"));
            permanentAddress.setLandmark(params.get("obj1.stringval69"));
            permanentAddress.setNameOfPremises(params.get("obj1.stringval67"));
            permanentAddress.setPinCode(params.get("obj1.stringval65"));
            permanentAddress.setPlace(params.get("obj1.stringval70"));
            permanentAddress.setPoliceStation(null);
            permanentAddress.setPostOrAreaOrNagar(null);
            permanentAddress.setRoadOrStreetOrLane(params.get("obj1.stringval68"));
            permanentAddress.setState(params.get("obj1.stringval62"));
            permanentAddress.setTownOrSuburbOrTaluka(null);

            String sameAsValue = params.get("obj1.stringval64");
            String sameAsAddressIP;
            if (sameAsValue != null && sameAsValue.equalsIgnoreCase("Y")) {
                sameAsAddressIP = "Current";
            } else {
                sameAsAddressIP = null;
            }

            permanentAddress.setAddressSameAs(sameAsAddressIP);
            permanentAddress.setResidingSince(null);
            permanentAddress.setAddressProofDocumentType(null);

            EducationAndOccupationDetails ipEducationAndOccupationDetails = new EducationAndOccupationDetails();

            Education education = new Education();
            education.setEducation(params.get("obj1.stringval129"));
            education.setEducationDetails(null);

            OccupationDetails occupationDetails = new OccupationDetails();
            occupationDetails.setAnnualIncome(params.get("obj1.stringval130"));
            occupationDetails.setBusinessDetails(null);
            occupationDetails.setEmployerAddress(params.get("obj1.stringval135"));

            PhoneNumber employerContactNumber = new PhoneNumber();
            employerContactNumber.setCountryCode(null);
            employerContactNumber.setNumber(params.get("obj1.stringval136"));
            employerContactNumber.setType(null);

            occupationDetails.setEmployerContactNumber(employerContactNumber);
            occupationDetails.setEmployerName(params.get("obj1.stringval134"));
            occupationDetails.setGroupCompanyName(null);
            occupationDetails.setIndustry(params.get("obj3.stringval98"));
            occupationDetails.setNatureOfDuties(params.get("obj1.stringval133"));
            occupationDetails.setOccupation(params.get("obj1.stringval131"));
            occupationDetails.setProfession(null);
            occupationDetails.setRelationShipToEmployee(null);
            occupationDetails.setWebsiteDetails(null);
            occupationDetails.setEmpCode(null);
            occupationDetails.setIncomeProof(null);
            occupationDetails.setIndustryType(null);
            occupationDetails.setExactNatureOfDuties(params.get("obj3.stringval99"));

            ipEducationAndOccupationDetails.setEducation(education);
            ipEducationAndOccupationDetails.setOccupationDetails(occupationDetails);

            PepDetails ipPEPDetails = new PepDetails();
            ipPEPDetails.setRelationShip(null);
            ipPEPDetails.setSelfOrAssociate(null);
            ipPEPDetails.setTypeOfPep(null);

            String isPoliticallyExposed =  params.get("obj1.stringval137");
            String pepDetail;
            if ("true".equalsIgnoreCase(isPoliticallyExposed)) {
                pepDetail = params.get("obj1.stringval138");
            } else {
                pepDetail = null;
            }
            ipPEPDetails.setDetail(pepDetail);
            ipPEPDetails.setIsPoliticallyExposed((params.get("obj4.stringval16")) != null && !(params.get("obj4.stringval16")).isEmpty() ? Boolean.valueOf(params.get("obj4.stringval16")) : null);

            IpPersonalDetailsWrapper ippersonalDetails = new IpPersonalDetailsWrapper();

            PersonDetails ipBasicDetails = new PersonDetails();
            ipBasicDetails.setAge((params.get("obj1.stringval17")) != null && !(params.get("obj1.stringval17")).isEmpty() ? Integer.parseInt((params.get("obj1.stringval17"))) : null);
            ipBasicDetails.setCountryOfBirth(null);
            ipBasicDetails.setCountryOfResidence(params.get("obj1.stringval52"));
            ipBasicDetails.setFatherName(params.get("obj1.stringval111"));
            ipBasicDetails.setFirstName(params.get("obj1.stringval13"));
            if (params.get("obj1.stringval18").equalsIgnoreCase("M")) {
            	ipBasicDetails.setGender("MALE");
            } else  if (params.get("obj1.stringval18").equalsIgnoreCase("F")) {
            	ipBasicDetails.setGender("FEMALE");
            }

            ipBasicDetails.setLastName(params.get("obj1.stringval15"));
            ipBasicDetails.setMiddleName(params.get("obj1.stringval14"));
//            ipBasicDetails.setMaritalStatus(params.get("obj1.stringval99"));  // TODO LOV
            String maritalStatusParam = params.get("obj1.stringval99");
            if ("M".equals(maritalStatusParam)) {
                ipBasicDetails.setMaritalStatus("Married");
            } else {
                ipBasicDetails.setMaritalStatus(maritalStatusParam);
            }
            ipBasicDetails.setMotherName(params.get("obj3.stringval110"));
            ipBasicDetails.setNameOfSpouse(params.get("obj1.stringval103"));
            ipBasicDetails.setNationality(params.get("obj1.stringval51"));
            ipBasicDetails.setPlaceOfBirth(params.get("obj1.stringval53"));
            ipBasicDetails.setSalutation(params.get("obj1.stringval12"));
            ipBasicDetails.setSuffix(null);
            ipBasicDetails.setPan(params.get("obj1.stringval122"));
//            ipBasicDetails.setIdProofDoc("PAN");
//            ipBasicDetails.setIdProofValue(params.get("obj1.stringval122"));

	   	     if (params.get("obj2.stringval102").equalsIgnoreCase("AADHAR_REFERENCE_CODE")) {
	   	    	ipBasicDetails.setIdProofDoc("AADHAR_REFERENCE_CODE");
	   	    	ipBasicDetails.setIdProofValue(params.get("obj1.stringval117"));

		     } else if (params.get("obj2.stringval102").equalsIgnoreCase("PAN")) {
		    	 ipBasicDetails.setIdProofDoc("PAN");
		    	 ipBasicDetails.setIdProofValue(params.get("obj1.stringval122"));
		     }

            ipBasicDetails.setDateOfBirth(convertDateForPAS(params.get("obj1.stringval16")));

            ContactDetails contactDetailsPD = new ContactDetails();

            PhoneNumber ipPDAlternateMobileNumber = new PhoneNumber();
            ipPDAlternateMobileNumber.setCountryCode(null);
            ipPDAlternateMobileNumber.setNumber(params.get("obj3.stringval16"));
            ipPDAlternateMobileNumber.setType("");

        	EmailId ipPDEmail = new EmailId();
        	ipPDEmail.setAddress(params.get("obj1.stringval20"));

        	SocialMediaId ipPDFaceBook = new SocialMediaId();
        	ipPDFaceBook.setId(null);

        	SocialMediaId ipPDLinkedInId = new SocialMediaId();
        	ipPDLinkedInId.setId(null);

        	PhoneNumber ipPDMobile = new PhoneNumber();
        	ipPDMobile.setCountryCode(null);
        	ipPDMobile.setNumber(params.get("obj1.stringval19"));
        	ipPDMobile.setType(null);

        	SocialMediaId ipPDSkype = new SocialMediaId();
        	ipPDSkype.setId(null);

        	PhoneNumber ipPDTelNo = new PhoneNumber();
        	ipPDTelNo.setCountryCode(null);
        	ipPDTelNo.setNumber(params.get("obj1.stringval63"));
        	ipPDTelNo.setStdCode(null);
        	ipPDTelNo.setType(null);

        	SocialMediaId ipPDTwitterId = new SocialMediaId();
        	ipPDTwitterId.setId(null);

        	PhoneNumber ipPDWhatsApp = new PhoneNumber();
        	ipPDWhatsApp.setCountryCode(null);
        	ipPDWhatsApp.setNumber(null);
        	ipPDWhatsApp.setType(null);

        	contactDetailsPD.setEmailId(ipPDEmail);
        	contactDetailsPD.setAlternateMobileNumber(ipPDAlternateMobileNumber);
        	contactDetailsPD.setFaceBook(ipPDFaceBook);
        	contactDetailsPD.setLinkedInId(ipPDLinkedInId);
        	contactDetailsPD.setMobileNumber(ipPDMobile);
        	contactDetailsPD.setSkype(ipPDSkype);
        	contactDetailsPD.setTelNo(ipPDTelNo);
        	contactDetailsPD.setTwitterId(ipPDTwitterId);
        	contactDetailsPD.setWhatsApp(ipPDWhatsApp);

        	ippersonalDetails.setIpBasicDetails(ipBasicDetails);
        	ippersonalDetails.setContactDetails(contactDetailsPD);
//        	ippersonalDetails.setPreferredModeOfCommunication(params.get("obj1.stringval105")); //TODO LOV's
        	ippersonalDetails.setPreferredModeOfCommunication(null);

        	String stringval123 = params.get("obj1.stringval123");
        	String proceedWithForm60;
        	if ("Y".equalsIgnoreCase(stringval123)) {
        	    proceedWithForm60 = "Yes";
        	} else {
        	    proceedWithForm60 = "No";
        	}
        	ippersonalDetails.setProceedWithForm60(proceedWithForm60);
        	ippersonalDetails.setPurposeOfInsurance(params.get("obj1.stringval43"));
        	ippersonalDetails.setResidentialStatus(params.get("obj1.stringval54"));
        	ippersonalDetails.setCpId(null);
        	ippersonalDetails.setLifeGoal(params.get("obj3.stringval120"));

        	ipDetails.setCurrentAddress(currentAddress);
        	ipDetails.setPermanentAddress(permanentAddress);
        	ipDetails.setIpEducationAndOccupationDetails(ipEducationAndOccupationDetails);
        	ipDetails.setIpPEPDetails(ipPEPDetails);
        	ipDetails.setIppersonalDetails(ippersonalDetails);
        	ipDetails.setKycType(null);

        	KycAmlAndOtherProofsDTO kycamlAndOtherProofsDTO = new KycAmlAndOtherProofsDTO();
        	kycamlAndOtherProofsDTO.setAnnualIncome(Long.valueOf(params.get("obj1.stringval130")));
        	kycamlAndOtherProofsDTO.setDocumentProvided(params.get("obj1.stringval120"));
        	kycamlAndOtherProofsDTO.setForm60(null);
        	kycamlAndOtherProofsDTO.setGstin(null);
        	kycamlAndOtherProofsDTO.setPan(null);
        	kycamlAndOtherProofsDTO.setUniqueKYCIdentifier(null);
        	kycamlAndOtherProofsDTO.setRcuFlag(null);
        	kycamlAndOtherProofsDTO.setFpuFlag(null);
        	kycamlAndOtherProofsDTO.setCustomerConsentFlag(null);
        	kycamlAndOtherProofsDTO.setBackDate(null);

            List<NomineeAndAppointeeDetailsDTO> nomineeAndAppointeeDetailsDTOs = new ArrayList<>();

            List<Integer> indices1 = params.keySet().stream()
                    .filter(k -> k.startsWith("list8[") && k.contains(".stringval1"))
                    .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            for (Integer i : indices1) {
                String baseKey = "list8[" + i + "]";

        	    NomineeAndAppointeeDetailsDTO dto = new NomineeAndAppointeeDetailsDTO();

        	    NomineeDetails nominee = new NomineeDetails();
        	    nominee.setFirstName(params.get(baseKey + ".stringval1"));
        	    nominee.setDob(params.get(baseKey + ".stringval3"));
        	    nominee.setRelationshipToLA(params.get(baseKey + ".stringval4"));
        	    nominee.setPercentageShare(params.get(baseKey + ".stringval5"));
        	    nominee.setAge(null);
        	    nominee.setLastName(null);
        	    nominee.setMiddleName(null);
        	    nominee.setMobileNo(null);
        	    nominee.setPercentageOfThisNominee(null);
        	    nominee.setRelationshipToinsure(null);
        	    nominee.setSalutation(null);
        	    nominee.setTitle(null);

        	    AppointeeDetails appointee = new AppointeeDetails();

        	    dto.setNomineeDetails(nominee);
        	    dto.setAppointeeDetails(appointee);

        	    nomineeAndAppointeeDetailsDTOs.add(dto);
        	}

        	OtherInsDetailsDTO otherInsDetailsDTO = new OtherInsDetailsDTO();

        	otherInsDetailsDTO.setAnnualPremiumPaid(null);
        	otherInsDetailsDTO.setAnyPrevPolicyPOOrDC(null);
        	otherInsDetailsDTO.setCount(null);

        	if (params.get("obj1.stringval44") != null) {
              	if (params.get("obj1.stringval44").equalsIgnoreCase("N")) {
               		otherInsDetailsDTO.setExisting(false);
              	} else if (params.get("obj1.stringval44").equalsIgnoreCase("Y")) {
               		otherInsDetailsDTO.setExisting(true);
              	}
        	}
        	otherInsDetailsDTO.setRole(null);
        	otherInsDetailsDTO.setSumAssured(params.get("obj1.stringval41"));

	   	    List<OtherInsDetailsDTO> otherInsDetailsList = Collections.singletonList(otherInsDetailsDTO);

		   	 Address phCurrentAddress = new Address();
	         phCurrentAddress.setAddressType("Current");
	         phCurrentAddress.setCityOrVillage(params.get("obj1.stringval85"));
	         phCurrentAddress.setCo(null);
	         phCurrentAddress.setCountry(params.get("obj3.stringval57"));
	         phCurrentAddress.setDistrict(params.get("obj1.stringval85"));
	         phCurrentAddress.setFlag(null);
	         phCurrentAddress.setFlatOrDoorNo(params.get("obj1.stringval80"));
	         phCurrentAddress.setLandmark(params.get("obj1.stringval83"));
	         phCurrentAddress.setNameOfPremises(params.get("obj1.stringval81"));
	         phCurrentAddress.setPinCode(params.get("obj1.stringval79"));
	         phCurrentAddress.setPlace(params.get("obj1.stringval84"));
	         phCurrentAddress.setPoliceStation(null);
	         phCurrentAddress.setPostOrAreaOrNagar(null);
	         phCurrentAddress.setRoadOrStreetOrLane(params.get("obj1.stringval82"));
	         phCurrentAddress.setState(params.get("obj1.stringval86"));
	         phCurrentAddress.setTownOrSuburbOrTaluka(null);
	         phCurrentAddress.setAddressSameAs(null);
	         phCurrentAddress.setResidingSince(null);
	         phCurrentAddress.setAddressProofDocumentType(null);

	   	    PayerDetails payerDetails = new PayerDetails();
	   	    String premiumPaidBy = params.get("obj1.stringval45");
		   	String stringval9 = params.get("obj1.stringval9");
		   	boolean ipph = (stringval9 != null && stringval9.equalsIgnoreCase("true"));
		   	boolean ippayer = false;
		   	boolean phpayer = false;
		   	if ("P".equalsIgnoreCase(premiumPaidBy)) {
		   	    phpayer = true;
		   	} else if ("PI".equalsIgnoreCase(premiumPaidBy)) {
		   	    ippayer = true;
		   	} else if ("O".equalsIgnoreCase(premiumPaidBy)) {
		   	}

		   	String stringval127 = params.get("obj1.stringval127");
            String phProceedWithForm60;
            if ("Y".equalsIgnoreCase(stringval127)) {
                phProceedWithForm60 = "Yes";
            } else {
                phProceedWithForm60 = "No";
            }

		   	PepDetails phPEPDetails = new PepDetails();
		   	phPEPDetails.setRelationShip(null);
		   	phPEPDetails.setSelfOrAssociate(null);
		   	phPEPDetails.setTypeOfPep(null);
            ipPEPDetails.setDetail(params.get("obj4.stringval17"));
            ipPEPDetails.setIsPoliticallyExposed(Boolean.valueOf(params.get("obj4.stringval16")));

		   	if (ippayer || ipph) {    // Logic for IP Payer or IPPH is True
		   		payerDetails.setAnnualIncome(null);
		   		payerDetails.setExistingPremium(null);
		   		payerDetails.setIppayer(ippayer);
		   		payerDetails.setPan(params.get("obj3.stringval94"));
		   		payerDetails.setPayerAddress(currentAddress);
	            PayerPersonalDetailsWrapper payerPersonalDetails = new PayerPersonalDetailsWrapper();
	            payerPersonalDetails.setBasicPersonDetails(ipBasicDetails);
	            payerPersonalDetails.setContactDetails(ipContact);
//				payerPersonalDetails.setPreferredModeOfCommunication(params.get("obj1.stringval105"));   //TODO LOV's
				payerPersonalDetails.setPreferredModeOfCommunication(null);  //TODO

				payerPersonalDetails.setProceedWithForm60(proceedWithForm60);
				payerPersonalDetails.setPurposeOfInsurance(params.get("obj1.stringval43"));
				payerPersonalDetails.setResidentialStatus(params.get("obj1.stringval54"));
				payerPersonalDetails.setCpId(null);
				payerDetails.setPayerPersonalDetails(payerPersonalDetails);
				payerDetails.setPhpayer(phpayer);
				payerDetails.setRelationshipToIP(params.get("obj1.stringval47"));
				payerDetails.setPhPayerSame(null);
				payerDetails.setKycType(null);
		   	}
		   	else if (phpayer) {
		   	    // Logic for PH Payer
		   		payerDetails.setAnnualIncome(null);
		   		payerDetails.setExistingPremium(null);
		   		payerDetails.setIppayer(ippayer);
		   		payerDetails.setPan(params.get("obj3.stringval94"));
		   		payerDetails.setPayerAddress(phCurrentAddress);
	            PayerPersonalDetailsWrapper payerPersonalDetails = new PayerPersonalDetailsWrapper();
	            payerPersonalDetails.setBasicPersonDetails(phPersonalDetails);
	            payerPersonalDetails.setContactDetails(phContact);
//				payerPersonalDetails.setPreferredModeOfCommunication(params.get("obj1.stringval105"));  //TODO LOV's
				payerPersonalDetails.setPreferredModeOfCommunication(null);

				payerPersonalDetails.setProceedWithForm60(phProceedWithForm60);
				payerPersonalDetails.setPurposeOfInsurance(null);
				payerPersonalDetails.setResidentialStatus(params.get("obj1.stringval78"));
				payerPersonalDetails.setCpId(null);
				payerDetails.setPayerPersonalDetails(payerPersonalDetails);
				payerDetails.setPhpayer(phpayer);
				payerDetails.setRelationshipToIP(params.get("obj1.stringval47"));
				payerDetails.setPhPayerSame(null);
				payerDetails.setKycType(null);

		   	}
		   	else if ("O".equalsIgnoreCase(premiumPaidBy)) {  // Logic for Other Payer

		   		PersonDetails oPersonalDetails = new PersonDetails();
	        	oPersonalDetails.setAge(null);
	            oPersonalDetails.setCountryOfBirth(null);
	            oPersonalDetails.setCountryOfResidence(null);
	            oPersonalDetails.setFatherName(null);
	            oPersonalDetails.setFirstName(params.get("obj1.stringval46"));

	            //oPersonalDetails.setGender(params.get("obj1.stringval49"));
	            if (params.get("obj1.stringval49").equalsIgnoreCase("M")) {
	            	oPersonalDetails.setGender("MALE");
	            } else  if (params.get("obj1.stringval49").equalsIgnoreCase("F")) {
	            	oPersonalDetails.setGender("FEMALE");
	            }

	            oPersonalDetails.setLastName(null);
	            oPersonalDetails.setMiddleName(null);
	            oPersonalDetails.setMaritalStatus(null);
	            oPersonalDetails.setMotherName(null);
	            oPersonalDetails.setNameOfSpouse(null);
	            oPersonalDetails.setNationality(null);
	            oPersonalDetails.setPlaceOfBirth(null);
	            oPersonalDetails.setSalutation(null);
	            oPersonalDetails.setSuffix(null);
	            oPersonalDetails.setPan(null);

                if (params.get("obj2.stringval102").equalsIgnoreCase("AADHAR_REFERENCE_CODE")) {
                    oPersonalDetails.setIdProofDoc("AADHAR_REFERENCE_CODE");
                    oPersonalDetails.setIdProofValue(params.get("obj1.stringval117"));

                } else if (params.get("obj2.stringval102").equalsIgnoreCase("PAN")) {
                    oPersonalDetails.setIdProofDoc("PAN");
                    oPersonalDetails.setIdProofValue(params.get("obj1.stringval122"));
                }

	            oPersonalDetails.setDateOfBirth(convertDateForPAS(params.get("obj1.stringval16")));

		   		payerDetails.setAnnualIncome(null);
		   		payerDetails.setExistingPremium(null);
		   		payerDetails.setIppayer(ippayer);
		   		payerDetails.setPan(params.get("obj3.stringval94"));
		   		payerDetails.setPayerAddress(currentAddress);
	            PayerPersonalDetailsWrapper payerPersonalDetails = new PayerPersonalDetailsWrapper();
	            payerPersonalDetails.setBasicPersonDetails(oPersonalDetails);
	            payerPersonalDetails.setContactDetails(null);
				payerPersonalDetails.setPreferredModeOfCommunication(null);
				payerPersonalDetails.setProceedWithForm60("No");
				payerPersonalDetails.setPurposeOfInsurance(null);
				payerPersonalDetails.setResidentialStatus(null);
				payerPersonalDetails.setCpId(null);
				payerDetails.setPayerPersonalDetails(payerPersonalDetails);
				payerDetails.setPhpayer(phpayer);
				payerDetails.setRelationshipToIP(params.get("obj1.stringval47"));
				payerDetails.setPhPayerSame(null);
				payerDetails.setKycType(null);
		   	}
		   	else {
		   		payerDetails = null;
		   	}

		   	Address phPermanentAddress = new Address();
            phPermanentAddress.setAddressType("Permanent");
            phPermanentAddress.setCityOrVillage(params.get("obj1.stringval95"));
            phPermanentAddress.setCo(null);
            phPermanentAddress.setCountry(null);
            phPermanentAddress.setDistrict(params.get("obj1.stringval95"));
            phPermanentAddress.setFlag(null);
            phPermanentAddress.setFlatOrDoorNo(params.get("obj1.stringval90"));
            phPermanentAddress.setLandmark(params.get("obj1.stringval93"));
            phPermanentAddress.setNameOfPremises(params.get("obj1.stringval91"));
            phPermanentAddress.setPinCode(params.get("obj1.stringval89"));
            phPermanentAddress.setPlace(params.get("obj1.stringval94"));
            phPermanentAddress.setPoliceStation(null);
            phPermanentAddress.setPostOrAreaOrNagar(null);
            phPermanentAddress.setRoadOrStreetOrLane(params.get("obj1.stringval92"));
            phPermanentAddress.setState(params.get("obj1.stringval96"));
            phPermanentAddress.setTownOrSuburbOrTaluka(null);

            EducationAndOccupationDetails phEducationAndOccupationDetails = new EducationAndOccupationDetails();

            Education phEducation = new Education();
            phEducation.setEducation(params.get("obj1.stringval141"));
            phEducation.setEducationDetails(null);

            OccupationDetails phOccupationDetails = new OccupationDetails();
            phOccupationDetails.setAnnualIncome(params.get("obj1.stringval142"));
            phOccupationDetails.setBusinessDetails(null);
            phOccupationDetails.setEmployerAddress(params.get("obj1.stringval147"));

            PhoneNumber phEmployerContactNumber = new PhoneNumber();
            phEmployerContactNumber.setCountryCode(null);
            phEmployerContactNumber.setNumber(params.get("obj1.stringval148"));
            phEmployerContactNumber.setType(null);

            phOccupationDetails.setEmployerContactNumber(phEmployerContactNumber);
            phOccupationDetails.setEmployerName(params.get("obj1.stringval146"));
            phOccupationDetails.setGroupCompanyName(null);
            phOccupationDetails.setIndustry(params.get("obj3.stringval92"));
            phOccupationDetails.setNatureOfDuties(params.get("obj1.stringval145"));
            phOccupationDetails.setOccupation(params.get("obj1.stringval143"));
            phOccupationDetails.setProfession(null);
            phOccupationDetails.setRelationShipToEmployee(params.get("obj3.stringval132"));
            phOccupationDetails.setWebsiteDetails(null);
            phOccupationDetails.setEmpCode(null);
            phOccupationDetails.setIncomeProof(null);
            phOccupationDetails.setIndustryType(null);
            phOccupationDetails.setExactNatureOfDuties(params.get("obj3.stringval147"));

            phEducationAndOccupationDetails.setEducation(phEducation);
            phEducationAndOccupationDetails.setOccupationDetails(phOccupationDetails);

		   	PhDetails phDetails = new PhDetails();

		   	if (ipph) {
		   	    // Case: IP and PH are the same person
		   		phDetails.setRelationshipToIP("SELF");
			   	phDetails.setCurrentAddress(currentAddress);
			   	phDetails.setIpph(ipph);
			   	phDetails.setPermanentAddress(permanentAddress);
			   	phDetails.setPhEducationAndOccupationDetails(ipEducationAndOccupationDetails);
			   	phDetails.setPhPEPDetails(ipPEPDetails);

			   	PhPersonalDetailsWrapper phPersonalDetailsWrapper = new PhPersonalDetailsWrapper();
			   	phPersonalDetailsWrapper.setBasicPersonDetails(ipPersonDetails);
			   	phPersonalDetailsWrapper.setContactDetails(ipContact);
			   	phPersonalDetailsWrapper.setPreferredModeOfCommunication(params.get("obj1.stringval105"));
			   	phPersonalDetailsWrapper.setProceedWithForm60(proceedWithForm60);
			   	phPersonalDetailsWrapper.setPurposeOfInsurance(params.get("obj1.stringval43"));
			   	phPersonalDetailsWrapper.setResidentialStatus(params.get("obj1.stringval54"));
			   	phPersonalDetailsWrapper.setCpId(null);

			   	phDetails.setPhPersonalDetails(phPersonalDetailsWrapper);
		   	}
		   	else {
		   	// Case: IP and PH are different people
		   		phDetails.setRelationshipToIP("");
		   	    phDetails.setAdditionalAddressDetails(null);
			   	phDetails.setCurrentAddress(phCurrentAddress);
			   	phDetails.setIpph(ipph);
			   	phDetails.setPermanentAddress(phPermanentAddress);
			   	phDetails.setPhEducationAndOccupationDetails(phEducationAndOccupationDetails);
			   	phDetails.setPhPEPDetails(phPEPDetails);

			   	PhPersonalDetailsWrapper phPersonalDetailsWrapper = new PhPersonalDetailsWrapper();
			   	phPersonalDetailsWrapper.setBasicPersonDetails(phPersonalDetails);
			   	phPersonalDetailsWrapper.setContactDetails(phContact);
			   	phPersonalDetailsWrapper.setPreferredModeOfCommunication(null);
			   	phPersonalDetailsWrapper.setProceedWithForm60(phProceedWithForm60);
			   	phPersonalDetailsWrapper.setPurposeOfInsurance(null);
			   	phPersonalDetailsWrapper.setResidentialStatus(params.get("obj1.stringval78"));
			   	phPersonalDetailsWrapper.setCpId(null);

			   	phDetails.setPhPersonalDetails(phPersonalDetailsWrapper);
		   	}

		   	PolicySelectionDetails policySelection = new PolicySelectionDetails();
		   	policySelection.setEmployeeID(params.get("obj3.stringval117"));
		   	policySelection.setKartaInsurable(false);
		   	policySelection.setKartaReason(null);
		   	policySelection.setPolicyType(params.get("obj1.stringval11"));
		   	policySelection.setIsEmployee((params.get("obj3.stringval113")) != null && !(params.get("obj3.stringval113")).isEmpty() ? Boolean.valueOf(params.get("obj3.stringval113")) : null);

		   	PremCollDetails premCollDetails = new PremCollDetails();
		   	premCollDetails.setProposalDeposit(params.get("obj2.stringval23"));
		   	premCollDetails.setRenewalPaymentMethod(params.get("obj2.stringval24"));
		   	premCollDetails.setAmountInWord(null);
		   	premCollDetails.setChequeNo(params.get("obj2.stringval30"));
		   	premCollDetails.setSisoFlag(null);
		   	premCollDetails.setDate(convertDateForPAS(params.get("obj2.stringval29")));

		   	ProductDetailsDTO productDetailsDTO = new ProductDetailsDTO();
		   	productDetailsDTO.setBasicOrMain(0);
		   	productDetailsDTO.setBt((params.get("list1[0].stringval3")) != null && !(params.get("list1[0].stringval3")).isEmpty() ? Integer.valueOf(params.get("list1[0].stringval3")) : null);
		   	productDetailsDTO.setCoverage((params.get("list1[0].stringval2")) != null && !(params.get("list1[0].stringval2")).isEmpty() ? Integer.valueOf(params.get("list1[0].stringval2")) : null);
		   	productDetailsDTO.setFundDetails(fundOptedFor);

		   	if (context != null && context.getBiResult() != null) {
		   		productDetailsDTO.setGmb(String.valueOf(context.getBiResult().getGmb()));
	        }

		   	productDetailsDTO.setIncreaseInLifeCover(null);
		   	productDetailsDTO.setMultiplier((params.get("obj1.stringval38")) != null && !(params.get("obj1.stringval38")).isEmpty() ? Double.valueOf(params.get("obj1.stringval38")) : null);
		   	productDetailsDTO.setOptionOrVariant(params.get("obj1.stringval31"));
		   	productDetailsDTO.setPensionMode(null);
		   	productDetailsDTO.setPensionOption(params.get("obj2.stringval13"));
		   	//TODO PPT Mapping
		   	//productDetailsDTO.setPpt((params.get("list1.WeoRecStrings150User(0).stringval4")) != null && !(params.get("list1.WeoRecStrings150User(0).stringval4")).isEmpty() ? Integer.valueOf(params.get("list1.WeoRecStrings150User(0).stringval4")) : 0);
		   	productDetailsDTO.setPpt(0); //TODO
		   	productDetailsDTO.setPremFrequency(params.get("obj1.stringval39"));
		   	productDetailsDTO.setPremiumAmount(params.get("obj1.stringval37"));
		   	productDetailsDTO.setPremiumApportionment(null);
		   	productDetailsDTO.setProductName(params.get("obj1.stringval30"));
		   	productDetailsDTO.setProductType("ULIP");

		   	List<RiderDetails> riderDetails = new ArrayList<>();
		   	//TODO set Riders Details
//		   	RiderDetails riderList = new RiderDetails();
//		   	riderList.setBenefitTerm(params.get("list2.WeoRecStrings150User(0).stringval3"));
//		   	riderList.setPremiumTerm(params.get("list2.WeoRecStrings150User(0).stringval4"));
//		   	riderList.setRider(params.get("list2.WeoRecStrings150User(0).stringval1"));
//		   	riderList.setSumAssured(params.get("list2.WeoRecStrings150User(0).stringval2"));
//		   	riderDetails.add(riderList);

		   	productDetailsDTO.setRiderDetails(riderDetails);
		   	productDetailsDTO.setSpouseAge(null);

		   	TopUpDetailsDTO topUpDetailsDTO = new TopUpDetailsDTO();
		   	topUpDetailsDTO.setTopUpMultiplier(params.get("obj2.stringval27"));
		   	topUpDetailsDTO.setTopUpPremium(params.get("obj2.stringval28"));
		   	topUpDetailsDTO.setTopUpSumAssured(params.get("obj2.stringval26"));

		   	if ((topUpDetailsDTO.getTopUpPremium() != null && !topUpDetailsDTO.getTopUpPremium().equals("null")) ||
		   		    (topUpDetailsDTO.getTopUpMultiplier() != null && !topUpDetailsDTO.getTopUpMultiplier().equals("null")) ||
		   		    (topUpDetailsDTO.getTopUpSumAssured() != null && !topUpDetailsDTO.getTopUpSumAssured().equals("null"))) {
		   		topUpDetailsDTO.setIsTopUpRequested(true);
		   	}else {
		   		topUpDetailsDTO.setIsTopUpRequested(false);
		   	}

		   	WitnessDetailsDTO witnessDetailsDTO =  new WitnessDetailsDTO();
		   	VernacularDeclaration verniacDeclaration = new VernacularDeclaration();
		   	verniacDeclaration.setCustomerPreferredLanguage(params.get("obj1.stringval114"));
		   	witnessDetailsDTO.setVerniacDeclaration(verniacDeclaration);

		   	// -------------- questionnaireDetails ---------------

               List<Integer> indices6 = params.keySet().stream()
                   .filter(k -> k.startsWith("list6[") && k.contains(".stringval1"))
                   .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                   .distinct()
                   .sorted()
                   .collect(Collectors.toList());

                QuestionnaireDetails questionnaireDetails = new QuestionnaireDetails();
		   		LifeStyleDetails lifestyle = new LifeStyleDetails();
		   		DghDetails dgh = new DghDetails();

		   		lifestyle.setLs01(params.get("obj2.stringval46"));
		   		lifestyle.setLs02(params.get("obj2.stringval47"));
		   		lifestyle.setLs03(params.get("obj2.stringval48"));

		   		for (Integer i : indices6) {
		   		    String baseKey = "list6[" + i + "]";
		   		    String questionId = params.get(baseKey + ".stringval2");
		   		    String val = params.get(baseKey + ".stringval13");

		   		    boolean isYes = (val != null && val.equalsIgnoreCase("Y"));

		   		    // --- Role Mapping ---
		   		    if ("1".equals(questionId)) {
		   		    	questionnaireDetails.setRole("IP");
		   		    } else if ("2".equals(questionId)) {
		   		    	questionnaireDetails.setRole("PH");
		   		    }

		   		 if ("32".equals(questionId)) {
		   	        lifestyle.setLs04(isYes);
		   	    } else if ("33".equals(questionId)) {
		   	        lifestyle.setLs05(isYes);
		   	    } else if ("34".equals(questionId)) {
		   	        lifestyle.setLs06(isYes);
		   	    } else if ("35".equals(questionId)) {
		   	        lifestyle.setLs07(isYes);
		   	    } else if ("39".equals(questionId)) {
		   	        lifestyle.setLs08(isYes);
		   	    }

		   		 if ("14".equals(questionId)) {
		   	        dgh.setDghf01(isYes);
		   	        if (isYes) dgh.setDghf05(isYes);
		   	    } else if ("15".equals(questionId)) {
		   	        dgh.setDghf02(isYes);
		   	        if (isYes) dgh.setDghf05(isYes);
		   	    } else if ("30".equals(questionId)) {
		   	        dgh.setDgh03(isYes);
		   	    } else if ("31".equals(questionId)) {
		   	        dgh.setDgh04(isYes);
		   	    }
		   	}
		   		questionnaireDetails.setLifeStyle(lifestyle);
		   		questionnaireDetails.setDgh(dgh);

		   		List<QuestionnaireDetails> questionnaireDetailsList = new ArrayList<>();
		   		questionnaireDetailsList.add(questionnaireDetails);


		   	String vendor = params.get("obj1.stringval1");
		   	JourneyDetails journeyDetails = new JourneyDetails();
		   	journeyDetails.setJourneyType("GENERAL");
		   	journeyDetails.setOtcRuleIndicator("N");
		   	journeyDetails.setPasaIndicator("N");
		   	journeyDetails.setStpRuleIndicator("N");
		   	journeyDetails.setSkipDocQC("N");

            PasaDetails pasaDetails = new PasaDetails();
            if (context.getEdcResult() != null && context.getEdcResult().getFollowupAction() != null &&
                    !context.getEdcResult().getFollowupAction().isEmpty()) {
                pasaDetails.setActionCode(context.getEdcResult().getFollowupAction());
            }
            pasaDetails.setInitialPasaCategory("T_NO_PASA");
            pasaDetails.setFinalPasaCategory("NO_PASA");

        	// Build PolicyCheckIn
        	PolicyCheckIn policyCheckIn = new PolicyCheckIn();
        	policyCheckIn.setSource(params.get("obj1.stringval1"));			//params.get("obj1.stringval1")
        	policyCheckIn.setBasicPolicyInsured(insuredList);
        	policyCheckIn.setImportantDates(importantDates);
        	policyCheckIn.setProductSelection(productSelection);
        	policyCheckIn.setIntegrations(integrations);
        	policyCheckIn.setBankDetailsDTO(bankDetailsDTO);
        	policyCheckIn.setBiSummary(biSummary);
        	policyCheckIn.setEiadto(eiaDTO);
        	policyCheckIn.setIpFamilyDetailsDTO(ipFamilyDetailsDTO);
        	policyCheckIn.setHabbitDetailsDTO(habbitDetailsDTO);
        	policyCheckIn.setIccrdto(iccrDTO);
        	policyCheckIn.setIpDetails(ipDetails);
        	policyCheckIn.setKycamlAndOtherProofsDTO(kycamlAndOtherProofsDTO);
        	policyCheckIn.setNomineeAndAppointeeDetailsDTO(nomineeAndAppointeeDetailsDTOs);
        	policyCheckIn.setOtherInsDetailsDTO(otherInsDetailsList);
        	policyCheckIn.setPayerDetails(payerDetails);
        	policyCheckIn.setPhDetails(phDetails);
        	policyCheckIn.setPolicySelection(policySelection);
        	policyCheckIn.setPremCollDetails(premCollDetails);
        	policyCheckIn.setProductDetailsDTO(productDetailsDTO);
        	policyCheckIn.setTopUpDetailsDTO(topUpDetailsDTO);
        	policyCheckIn.setWitnessDetailsDTO(witnessDetailsDTO);
        	policyCheckIn.setQuestionnaireDetails(questionnaireDetailsList);
        	policyCheckIn.setJourneyDetails(journeyDetails);
            policyCheckIn.setPasaDetails(pasaDetails);


        	// Build PasRequestBody
        	PasRequestBody request = new PasRequestBody();
        	request.setApplicationId(applicationId);
        	request.setProposalNumber(null);
        	request.setStatus("DRAFT");
        	request.setChapterDetails(chapterDetails);
        	request.setPolicyCheckIn(policyCheckIn);

        	pasRequest.setHeader(header);
        	pasRequest.setRequest(request);

		return pasRequest;

    }
    
    int extractIndex(String key) {
    	Pattern pattern = Pattern.compile("\\((\\d+)\\)");
    	Matcher matcher = pattern.matcher(key);
    	if(matcher.find()) {
    		return Integer.parseInt(matcher.group(1));
    	}
    	throw new IllegalArgumentException("Invalid key format : " + key);
    }
    
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    public static String convertDateForPAS(String inputDate) {
        if (inputDate == null || inputDate.trim().isEmpty()) {
            return null;
        }
        
        // Parses "dd/MM/yyyy"
        LocalDate localDate = LocalDate.parse(inputDate.trim(), INPUT_FORMATTER);
        
        // Attaches Midnight and +0530 Offset
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(INDIA_ZONE);
        
        // Returns "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        return zonedDateTime.format(OUTPUT_FORMATTER);
    }
    	 
}
