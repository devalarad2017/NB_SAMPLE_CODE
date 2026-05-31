package com.balic.newbusiness.integration.model.dcs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcsRequest {
	
	@JsonProperty("ref_ID")
	private String refId;
	@JsonProperty("policy_number")
	private String policyNumber;
	@JsonProperty("application_no")
	private String applicationNo;
	@JsonProperty("service_ID")
	private String serviceId;
	@JsonProperty("request_source")
	private String requestSource;
	@JsonProperty("data_details")
	private List<DataDetails> dataDetails;
	
	@Data
	public static class DataDetails {
	@JsonProperty("sla_proposer_payer")
	private String slaProposerPayer;
	@JsonProperty("prop_sign_duration")
	private String propSignDuration;
	@JsonProperty("jl_kyc_type")
	private String jlKycType;
	@JsonProperty("jl_age")
	private String jlAge;
	@JsonProperty("is_proposer_payer")
	private String isProposerPayer;
	@JsonProperty("occupation")
	private String occupation;
	@JsonProperty("pp_kyc_type")
	private String ppKycType;
	@JsonProperty("channel")
	private String channel;
	@JsonProperty("action_code")
	private String actionCode;
	@JsonProperty("plvc_indicator")
	private String plvcIndicator;
	@JsonProperty("is_form_60")
	private String isForm60;
	@JsonProperty("drc_risk_flag")
	private String drcRiskFlag;
	@JsonProperty("is_pos")
	private String isPos;
	@JsonProperty("is_offline_case")
	private String isOfflineCase;
	@JsonProperty("pp_pan_verified")
	private String ppPanVerified;
	@JsonProperty("product_id")
	private String productId;
	@JsonProperty("residential_status")
	private String residentialStatus;
	@JsonProperty("agent_code")
	private String agentCode;
	@JsonProperty("journey_type")
	private String journeyType;
	@JsonProperty("sub_channel")
	private String subChannel;
	@JsonProperty("tap")
	private Double tap;
	@JsonProperty("phIndustry")
	private String phIndustry;
	@JsonProperty("is_wop")
	private String isWop;
	@JsonProperty("auto_renewal_paymode")
	private String autoRenewalPaymode;
	@JsonProperty("mhr_flag")
	private String mhrFlag;
	@JsonProperty("existing_ph")
	private String existingPh;
	@JsonProperty("pasa_flag")
	private String pasaFlag;
	@JsonProperty("prem_pay_mode")
	private String premPayMode;
	@JsonProperty("risks_core")
	private Double risksCore;
	@JsonProperty("is_signature")
	private String isSignature;
	@JsonProperty("distribution_channel")
	private String distributionChannel;
	@JsonProperty("is_jl_pan_verified")
	private String isJlPanVerified;
	@JsonProperty("existing_la")
	private String existingLa;
	@JsonProperty("ip_kyc_type")
	private String ipKycType;
	@JsonProperty("ph_pan_verified")
	private String phPanVerified;
	@JsonProperty("is_laproposer")
	private String isLaproposer;
	@JsonProperty("is_penny_drop_successful")
	private String isPennyDropSuccessful;
	@JsonProperty("fund_transfer")
	private List<FundTransfer> fundTransfer;
	@JsonProperty("credit_score")
	private Integer creditScore;
	@JsonProperty("is_physical_handicap")
	private String isPhysicalHandicap;
	@JsonProperty("is_ic_available")
	private String isIcAvailable;
	@JsonProperty("industry")
	private String industry;
	@JsonProperty("ispep")
	private String ispep;
	@JsonProperty("tasa")
	private Double tasa;
	@JsonProperty("ph_is_form_60")
	private String phIsForm60;
	@JsonProperty("premium")
	private String premium;
	@JsonProperty("occupation_others")
	private String occupationOthers;
	@JsonProperty("kyc_type")
	private String kycType;
	@JsonProperty("bill_freq")
	private String billFreq;
	@JsonProperty("pay_or_relation")
	private String payOrRelation;
	@JsonProperty("ip_pan_verified")
	private String ipPanVerified;
	@JsonProperty("process_type")
	private String processType;
	@JsonProperty("ph_relation")
	private String phRelation;
	@JsonProperty("is_duplicate_contact")
	private String isDuplicateContact;
	@JsonProperty("is_joint_life")
	private String isJointLife;
	@JsonProperty("ismwp")
	private String ismwp;
	@JsonProperty("proposal_type")
	private String proposalType;
	@JsonProperty("pp_is_form_60")
	private String ppIsForm60;
	@JsonProperty("ph_kyc_type")
	private String phKycType;
	@JsonProperty("product_type")
	private String productType;
	@JsonProperty("bop_flag")
	private String bopFlag;
	@JsonProperty("la_age")
	private String laAge;
	@JsonProperty("latest_policy_duration_flag")
	private String latestPolicyDurationFlag;
	}
	
	@Data
	public static class FundTransfer {
		@JsonProperty("ft_sub_type")
		private String ftSubType;
	}

}
