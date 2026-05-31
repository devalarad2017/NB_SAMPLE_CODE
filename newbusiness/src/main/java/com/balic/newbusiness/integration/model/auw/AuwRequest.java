package com.balic.newbusiness.integration.model.auw;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuwRequest {

	@JsonProperty("policy_no")
    private String policyNo;
    @JsonProperty("service_id")
    private String serviceId;
    @JsonProperty("data_details")
    private DataDetails dataDetails;
    @JsonProperty("ref_no")
    private String refNo;
    @JsonProperty("request_source")
    private String requestSource;
    @JsonProperty("pan_validation_status")
    private String panValidationStatus;
    @JsonProperty("edc_category")
    private String edcCategory;
    @JsonProperty("is_photo_ocr")
    private String isPhotoOcr; 			//Pass on “Yes” if in case OCR successfully identifies the human face else “No”
    @JsonProperty("existing_la")
    private String existingLa;			//Pass on “Yes” if the Lifeassured is an existing customer or else “No”
    @JsonProperty("existing_ph")
    private String existingPh;			//Pass on “Yes” if the proposer is an existing customer or else “No”
    @JsonProperty("check_otc")
    private String checkOtc;			//“1” if OTC to be verified. “0” if not required
    @JsonProperty("kyc_type")
    private String kycType;				//Manual/CKYC/EKYC/PARTNERKYC
    @JsonProperty("ph_nsdl_verified")
    private String phNsdlVerified;		//Yes/No/NA based on NSDL verification status of policyholder NA – To be passed on if LA=PH else based on verification status either pass Yes/No
    @JsonProperty("la_nsdl_verified")
    private String laNsdlVerified;		//Yes/No based on NSDL verification status for lifeassured
    @JsonProperty("payment_mode")
    private String paymentMode;			//p_in_obj_5	STRINGVAL13
    @JsonProperty("trbsa")
    private Integer trbsa;				//Term rider benefit Sumassured
    @JsonProperty("tibsa")
    private Integer tibsa;				//Terminal illness benefit Sumassured
    @JsonProperty("slbsa")
    private Integer slbsa;				//Start Life benefit Sumassured
    @JsonProperty("pdabsa")
    private Integer pdabsa;				//PDAB benefit Sumassured
    @JsonProperty("mgbsa")
    private Integer mgbsa;				//Mahil Gain benefit Sumassured
    @JsonProperty("hisa")
    private Integer hisa;				//Hospitalization Insurance benefit Sumassured
    @JsonProperty("cibsa")
    private Integer cibsa;				//Critical illness benefit Sumassured
    @JsonProperty("adbsa")
    private Integer adbsa;				//Accidental Death benefit Sumassured
    @JsonProperty("acisa")
    private Integer acisa;				//Accelerated CI benefit Sumassured
    @JsonProperty("tin_flag")
    private String tinFlag;				//"Yes" if TIN flag else "no"
    @JsonProperty("ph_education")
    private String phEducation;			//p_in_obj_1	STRINGVAL141
    @JsonProperty("ph_credit_score")
    private String phCreditScore;		//Cibil score of PH
    @JsonProperty("credit_score")
    private String creditScore;			//Cibil score for IP
    @JsonProperty("branch_visit")
    private String branchVisit;			//"Yes" incase of branch visit insisted at QC stage or else "no"
    @JsonProperty("basic_sa")
    private String basicSa;				//Basic Sumassured

    @Data
    public static class DataDetails {
        @JsonProperty("education")
        private String education;			//p_in_obj_1	STRINGVAL141
        @JsonProperty("occupation")			//p_in_obj_1	STRINGVAL143
        private String occupation;
        @JsonProperty("ph_relationship")
        private String phRelationship;		//Policy holder relationship with LA
        @JsonProperty("top_up")
        private String topUp;				//"Yes" if topup is opted else "No"
        @JsonProperty("channel")
        private String channel;				//p_in_obj_5	stringval126
        @JsonProperty("cp_id")
        private List<CpId> cpId;
        @JsonProperty("is_form_60")
        private String isForm60;			//p_in_obj_1	STRINGVAL123
        @JsonProperty("residence_country")
        private String residenceCountry;	//p_in_obj_1	STRINGVAL76
        @JsonProperty("annual_premium")
        private Integer annualPremium;
        @JsonProperty("age_of_death")
        private String ageOfDeath;			//p_in_list_5	STRINGVAL4
        @JsonProperty("product_id")
        private String productId;          //p_in_obj_1	STRINGVAL10
        @JsonProperty("payer_relationship")
        private String payerRelationship;	//Premium Payer relationship with LA
        @JsonProperty("residential_status")
        private String residentialStatus;   // p_in_obj_1	STRINGVAL76
        @JsonProperty("tobacco")
        private Integer tobacco;			//For Smoker pass on no of tobacco per day is being consumed
        @JsonProperty("agent_code")
        private String agentCode;			//p_in_obj_1	STRINGVAL4
        @JsonProperty("height")
        private Integer height;				//p_in_obj_2	STRINGVAL55
        @JsonProperty("annual_income")
        private Integer annualIncome;		
        @JsonProperty("alcohol")
        private Integer alcohol;			//Incase of Alcohol the consumption details should be shared (in ML)
        @JsonProperty("check_otc")
        private String checkOtc;			//“1” if OTC to be verified. “0” if not required
        @JsonProperty("tap")
        private Double tap;				//Total Annual Premium. (Current Proposal + Policies)
        @JsonProperty("suspected_cp")
        private String suspectedCp;			//In case if the customer is one among the suspected cp list then pass on "Yes" else "No"
        @JsonProperty("is_pep")
        private String isPep;				//Indication flag whether customer is politically exposed person or not
        @JsonProperty("product_multiplier")
        private Integer productMultiplier;	
        @JsonProperty("weight")
        private Integer weight;				//p_in_obj_2	STRINGVAL56
        @JsonProperty("ph_education")
        private String phEducation;			//p_in_obj_1	STRINGVAL141
        @JsonProperty("auto_renewal_paymode")
        private String autoRenewalPaymode;	//p_in_obj_5	STRINGVAL13
        @JsonProperty("is_combo")
        private String isCombo;				//Indication of whether current proposal is combo or not
        @JsonProperty("pasa_flag")
        private String pasaFlag;			//p_in_obj_5	stringval23
        @JsonProperty("distribution_channel")
        private String distributionChannel;	 //Distribution channel code should be passed on Agent API: salesChanel)
        @JsonProperty("is_backdated")
        private String isBackdated;			//Indication whether the proposal is backdated one or not
        @JsonProperty("ph_age")
        private String phAge;				//p_in_obj_1	STRINGVAL26
        @JsonProperty("ph_tasa")
        private Long phTasa;				//TASA of Policy Holder
        @JsonProperty("suc")
        private String suc;					//SUC value of UCS service
        @JsonProperty("gender")
        private String gender;				//p_in_obj_1	STRINGVAL27
        @JsonProperty("physically_handicapped")
        private String physicallyHandicapped; 
        @JsonProperty("dgh")
        private String dgh;					//"Yes" in case of any question marked as yes in DGH else "no"
        @JsonProperty("previously_declined")
        private String previouslyDeclined;	//Pass on "Yes" incase of previous policy is declined
        @JsonProperty("husband_tasa")
        private Integer husbandTasa;		//Husband TASA
        @JsonProperty("industry")
        private String industry;			//p_in_obj_3	STRINGVAL92
        @JsonProperty("sibling_tasa")
        private Integer siblingTasa;
        @JsonProperty("tasa")
        private Double tasa;					//Total Actual Sum assured.
        @JsonProperty("family_members_age_of_death")
        private Integer familyMembersAgeOfDeath;
        @JsonProperty("occupation_others")
        private String occupationOthers;
        @JsonProperty("ph_occupation")
        private String phOccupation;		//p_in_obj_1	STRINGVAL143
        @JsonProperty("process_type")
        private String processType;			//For New Business pass on “New Business” for PS and other the respective process name should be passed on (Eg : Revival, Occupation Change)
        @JsonProperty("simultaneous_flag")
        private String simultaneousFlag;	//Pass on "Yes" incase of simultaneous proposal else "No"
        @JsonProperty("basic_sa")
        private String basicSa;				//Basic Sumassured
        @JsonProperty("policy_no")
        private String policyNo;			//p_in_list_4	STRINGVAL1
        @JsonProperty("is_staff")
        private String isStaff;				//Is staff case or not
        @JsonProperty("risk_score")
        private Integer riskScore;			//Score arrived from Analytical tool of BALIC
        @JsonProperty("change_in_weight")
        private Integer changeInWeight;
        @JsonProperty("previously_postpone")
        private String previouslyPostpone;	//Pass on "Yes" incase of previous policy is postponed
        @JsonProperty("marital_status")
        private String maritalStatus;		//p_in_obj_1	STRINGVAL108
        @JsonProperty("product_type")
        private String productType;			//p_in_obj_1	STRINGVAL31
        @JsonProperty("req_codes")
        private String reqCodes;			//Pass on all requirement codes comma separated(DCS Requirement codes)
        @JsonProperty("customer_id")
        private String customerId;
        @JsonProperty("medical_flag")
        private String medicalFlag;			//Flag shared in MRS service response (Medical,NonMedical,TeleUW)
        @JsonProperty("fatca")
        private String fatca;				//"Yes" for FATCA else "no"
        @JsonProperty("age")
        private Integer age;				//p_in_obj_1	STRINGVAL26
        @JsonProperty("proposer_type")
        private String proposerType;
        @JsonProperty("bmi")
        private Double bmi;				//BMI of the policy holder
    }

    @Data
    public static class CpId {
        @JsonProperty("customer_id")
        private String customerId;
    }
}
