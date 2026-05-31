package com.balic.newbusiness.integration.model.pas;


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
public class PasInitialResponse {

    private Object header;
    private Response response;
    private List<Object> faults;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Response {
        private long applicationId;
        private String proposalNumber;
        private boolean backdatedPolicyAllowed;
        private List<ChapterDetail> chapterDetails;
        private PolicyCheckIn policyCheckIn;
        private String status;
	}
	
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ChapterDetail {
	        private String type;
	        private String templateCode;
	    }

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class PolicyCheckIn {
	        private ImportantDates importantDates;
	        private ProductSelection productSelection;
	        private PolicySelection policySelection;
	        private String source;
	        private List<BasicPolicyInsured> basicPolicyInsured;
	        private boolean appGeneratedByNb;
	    }

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class ImportantDates {
	        private String policyCheckInDate;
	        private String policyIssueDate;
	        private String policyReceivedDate;
	        private String policySignedDate;
	    }

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class ProductSelection {
	        private String baseCoverageCode;
	        private String productPlan;
	        private String productType;
	        private String policyIssueState;
	        private String branchCode;
	    }

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class PolicySelection {
	        private boolean kartaInsurable;
	        private String employeeID;
	        private String policyType;
	    }

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	    public static class BasicPolicyInsured {
	        private String partyReferenceId;
	        private String salutation;
	        private String gender;
	        private String policyInsuredDateOfBirth;
	        private String policyInsuredFirstName;
	        private String policyInsuredMiddleName;
	        private String policyInsuredLastName;
	        private String policyInsuredLegalIdentifierCode;
	        private String policyInsuredLegalIdentifierValue;
	    }
}
