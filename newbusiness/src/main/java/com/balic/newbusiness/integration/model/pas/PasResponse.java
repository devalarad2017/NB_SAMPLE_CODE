package com.balic.newbusiness.integration.model.pas;

import java.sql.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * PasResponse — received from PAS API after application submission.
 *
 * applicationNumber is the key output — stored in journey_execution
 * and sent to partner via reverse feed.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasResponse {
	
	    private Object header;
	    private Response response;
	    private List<Object> faults;

		@Data
		@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class Response {
	        private boolean backdatedPolicyAllowed;
	        private List<Object> chapterDetails;
	        private PolicyCheckIn policyCheckIn;
	        private String status;

	    }

		@Data
		@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class PolicyCheckIn {
	        private String message;
	        private boolean appGeneratedByNb;

	    }

//	private boolean appGeneratedByNb;
//    private AppResponse appResponse;
//    private int nbId;
//    private String pasSystem;
//
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class AppResponse {
//	    private List<Fault> faults;
//	    private Header header;
//	    private Response response;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class Fault {
//	    private String category;
//	    private List<ErrorDetail> errors;
//	    private String field;
//	    private String label;
//	    private String type;
//	    private String validator;
//	    private boolean waived;
//	    private String waivedBy;
//	    private Date waiverDate;
//	    private List<String> waiverPermissions;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class ErrorDetail {
//	    private String code;
//	    private String message;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class Header {
//	    private String containerId;
//	    private String correlationId;
//	    private String processTaskId;
//	    private ProcessVars processVars;
//	    private String result;
//	    private String telemetryId;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class ProcessVars {
//	    private boolean MDRT;
//	    private boolean PrefferedIC;
//	    private Date PremReciptDate;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class Response {
//	    private int applicationId;
//	    private List<ChapterDetail> chapterDetails;
//	    private PolicyCheckIn policyCheckIn;
//	    private String proposalNumber;
//	    private String status;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class ChapterDetail {
//	    private String templateCode;
//	    private String type;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class PolicyCheckIn {
//	    private AdditionalInfoMap additionalInfoMap;
//	    private List<BasicPolicyInsured> basicPolicyInsured;
//	    private ImportantDates importantDates;
//	    private String message;
//	    private ProductSelection productSelection;
//	    private String source;
//	}
//	
//	@Data
//	@JsonIgnoreProperties(ignoreUnknown = true)
//	public static class AdditionalInfoMap {
//		
//	}
//	
//	
//	@Data
//	public static class BasicPolicyInsured {
//	    private String gender;
//	    private String partyReferenceId;
//	    private String policyInsuredDateOfBirth;
//	    private String policyInsuredFirstName;
//	    private String policyInsuredLastName;
//	    private String policyInsuredLegalIdentifierCode;
//	    private String policyInsuredLegalIdentifierValue;
//	    private String policyInsuredMiddleName;
//	    private String salutation;
//	    private String suffix;
//	}
//	
//	@Data
//	public static class ImportantDates {
//	    private String policyCheckInDate;
//	    private String policyIssueDate;
//	    private String policyReceivedDate;
//	    private String policySignedDate;
//	    private String policyYearDate;
//	    private String riskEffectiveDate;
//	}
//	
//	@Data
//	public static class ProductSelection {
//	    private String baseCoverageCode;
//	    private String branchCode;
//	    private String policyIssueState;
//	    private String productPlan;
//	}
}
