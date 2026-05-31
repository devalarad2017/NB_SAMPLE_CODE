package com.balic.newbusiness.integration.model.mrs;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MrsRequest {

	@JsonProperty("ref_ID")
    private String refId;
    @JsonProperty("policy_number")
    private String policyNumber;           //p_in_obj_1	STRINGVAL6
    @JsonProperty("application_no")			//p_in_obj_1	STRINGVAL6
    private String applicationNo;
    @JsonProperty("service_ID")
    private String serviceId;
    @JsonProperty("data_details")
    private DataDetails dataDetails;
    @JsonProperty("request_source")
    private String requestSource;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataDetails {
        @JsonProperty("proposer_id")
        private String proposerId;			//PROPOSER CustomerID
        @JsonProperty("rm_contact")
        private String rmContact;			//p_in_obj_1	STRINGVAL28
        @JsonProperty("education")
        private String education;			//p_in_obj_1	STRINGVAL141
        @JsonProperty("occupation")
        private String occupation;			//p_in_obj_1	STRINGVAL143
        @JsonProperty("productid")
        private String productid;			//p_in_obj_1	STRINGVAL10
        @JsonProperty("agent_name")
        private String agentName;
        @JsonProperty("channel")
        private String channel;
        @JsonProperty("action_code")
        private String actionCode;
        @JsonProperty("customer_category")
        private String customerCategory;			//In case of Axis bank cases then customer category should be passed on else pass on null
        @JsonProperty("pan_no")
        private String panNo;						//p_in_obj_1	STRINGVAL126
        @JsonProperty("drc_risk_flag")
        private String drcRiskFlag;
        @JsonProperty("residence_country")
        private String residenceCountry;			//p_in_obj_1	STRINGVAL76
        @JsonProperty("smoker")
        private String smoker;						 //p_in_obj_3	STRINGVAL1
        @JsonProperty("residential_status")
        private String residentialStatus;			//p_in_obj_1	STRINGVAL76
        @JsonProperty("agent_code")
        private String agentCode;					//p_in_obj_1	STRINGVAL4
        @JsonProperty("state")
        private String state;						//p_in_obj_1	STRINGVAL96
        @JsonProperty("latest_pol_duration")
        private String latestPolDuration;			//In case of existing policy then pass on the duration in years. If not pass on blank
        @JsonProperty("sub_channel")
        private String subChannel;					//p_in_obj_5	stringval126
        @JsonProperty("annual_income")
        private String annualIncome;				//Annual income declared by customer
        @JsonProperty("dgh_response")
        private String dghResponse;					//No if in case any of the DGH question is marked as No else Yes.
        @JsonProperty("pincode")
        private String pincode;						//p_in_obj_1	STRINGVAL79
        @JsonProperty("tap")
        private Double tap;						//TotalAnnualPremium which is part of response of UCS service
        @JsonProperty("product_multiplier")
        private String productMultiplier;
        @JsonProperty("previous_rateup")
        private String previousRateup;				//Pass 0 if not ratedup for any policy of the customer or else 1
        @JsonProperty("product_name")
        private String productName;					//p_in_obj_1	STRINGVAL30
        @JsonProperty("proposer_name")
        private String proposerName;				//p_in_obj_1	STRINGVAL13 + p_in_obj_1	STRINGVAL14  + p_in_obj_1	STRINGVAL15
        @JsonProperty("auto_renewal_paymode")
        private String autoRenewalPaymode;			//p_in_obj_5	STRINGVAL13
        @JsonProperty("trl")
        private Integer trl;						//TransUnion truerisk score
        @JsonProperty("branch_code")
        private String branchCode;
        @JsonProperty("rm_email")
        private String rmEmail;
        @JsonProperty("pasa_flag")
        private String pasaFlag;					//p_in_obj_5	stringval23
        @JsonProperty("dob")
        private String dob;							//p_in_obj_1	STRINGVAL25
        @JsonProperty("distribution_channel")
        private String distributionChannel;			//Distribution channel code should be passed on Agent API: salesChanel)
        @JsonProperty("rider_name")
        private String riderName;					
        @JsonProperty("cisuc")						//CISUC value captured from underwriting computation service response
        private Double cisuc;
        @JsonProperty("suc")						//SUC value captured from underwriting computation service response
        private Double suc;
        @JsonProperty("email_id")
        private String emailId;						//p_in_obj_1	STRINGVAL29
        @JsonProperty("gender")			
        private String gender;						//p_in_obj_1	STRINGVAL27
        @JsonProperty("agent_email")
        private String agentEmail;					
        @JsonProperty("city")
        private String city;						//p_in_obj_1	STRINGVAL85
        @JsonProperty("mobile_no")
        private Long mobileNo;						//p_in_obj_1	STRINGVAL28
        @JsonProperty("total_annual_premium")
        private String totalAnnualPremium;			//TotalAnnualPremium which is part of response of UCS service
        @JsonProperty("industry")
        private String industry;					//p_in_obj_3	STRINGVAL92
        @JsonProperty("agent_club")
        private String agentClub;		
        @JsonProperty("tasa")
        private Double tasa;						//TASA value captured from underwriting computation service response
        @JsonProperty("occupation_others")
        private String occupationOthers;			//The free text value in case of others is choosen as occupation
        @JsonProperty("policy_number")
        private String policyNumber;				//p_in_obj_1	STRINGVAL6
        @JsonProperty("process_type")
        private String processType;					//For New Business pass on “New Business” for PS and other the respective process name should be passed on (Eg : Revival, Occupation Change)
        @JsonProperty("address")
        private String address;						//p_in_obj_1	STRINGVAL147
        @JsonProperty("is_staff")
        private String isStaff;						//Yes if the application is related to staff or else No
        @JsonProperty("bo_code")
        private String boCode;
        @JsonProperty("agent_mobile")
        private String agentMobile;
        @JsonProperty("passport_submitted")
        private String passportSubmitted;			//Yes if passport copy submitted or else No
        @JsonProperty("is_insurance_consultant")
        private String isInsuranceConsultant;		//Yes if application is IC own policy. Or Else No
        @JsonProperty("product_type")
        private String productType;
        @JsonProperty("location")
        private String location;
        @JsonProperty("notify_tpa")
        private Integer notifyTpa;					//0- To see only medicals 1- To trigger Notification to TPA incase of Medical
        @JsonProperty("customer_name")
        private String customerName;
        @JsonProperty("is_high_risk")
        private String isHighRisk;					//Yes if the life is high risk case or else No
        @JsonProperty("customer_id")
        private String customerId;					//p_in_obj_2	stringval82
        @JsonProperty("age")
        private Integer age;						//p_in_obj_1	STRINGVAL26
    }
}
