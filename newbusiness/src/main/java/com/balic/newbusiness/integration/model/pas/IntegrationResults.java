package com.balic.newbusiness.integration.model.pas;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationResults {

	// CIBIL
    @JsonProperty("CIBILTUSC3")
    private String cibilTuSc3;
    
    // UCS
    @JsonProperty("SUC")
    private Double suc;
    @JsonProperty("PDABSUC")
    private Double pdabSuc;
    @JsonProperty("FIBSUC")
    private Double fibSuc;
    @JsonProperty("LifeSUC")
    private Double lifeSuc;
    @JsonProperty("HISUC")
    private Double hiSuc;
    @JsonProperty("ADBSUC")
    private Double adbSuc;
    @JsonProperty("TASA")
    private Double tasa;
    @JsonProperty("CIBSUC")
    private Double cibSuc;
    @JsonProperty("WOPSUC")
    private Double wopSuc;
    @JsonProperty("TIBSUC")
    private Double tibSuc;
    @JsonProperty("TAP")
    private Double tap;
    @JsonProperty("ADBTASA")
    private Double adbTasa;
    @JsonProperty("APTPDTASA")
    private Double aptpdTasa;

    // --- Financial & Eligibility FES ---
    @JsonProperty("Fixed Deposit or Mutual Fund")
    private String fixedDepositOrMutualFund;
    @JsonProperty("Monthly SIP")
    private String monthlySip;
    @JsonProperty("Monthly Credit Card")
    private String monthlyCreditCard;
    @JsonProperty("Property Loan")
    private String propertyLoan;
    @JsonProperty("Premium Paying Capacity")
    private String premiumPayingCapacity;
    @JsonProperty("Financial Eligibility based on CIBIL")
    private String financialEligibilityBasedOnCibil;

    // --- MRS ---
    private String nonMedical;
    
    // AWS
    private String auwResults;
    
    // --- DCS ---
    @JsonProperty("Address Proof of Assured_Driving license")
    private String addressProofOfDrivingLicense;
    @JsonProperty("PanCard of Assured_PAN Card")
    private String panCardOfAssured;
    @JsonProperty("Full length Photograph")
    private String fullLengthPhotograph;
    @JsonProperty("IDENTITY PROOF OF LIFE ASSURED")
    private String identityProofOfLifeAssured;
    @JsonProperty("Copy OF Cheque")
    private String copyOfCheque;
    private String bi;
    @JsonProperty("Proposal form")
    private String proposalForm;
    @JsonProperty("Dynamic_Questionaire")
    private String dynamicQuestionaire;
    @JsonProperty("RECEIPT")
    private String receipt;
    @JsonProperty("EKYC PH_Aadhaar Photo")
    private String ekycPhAadhaarPhoto;
    @JsonProperty("EKYC IH_Aadhaar Photo")
    private String ekycIhAadhaarPhoto;

    // --- PAN Verification Status ---
    @JsonProperty("new_pan_status")
    private NewPanStatus newPanStatus;
    private String ip;
    private String ph;
    private String payer;

    // --- EDC ---
    private String appNo;
    private String edcScore;
    private String edcAlert;
    private String alertReason;
    private String errorCode;
    private String remark;
    private String tmpStmp;
    private String modelAlert;
    private String persistencyScore;
    private String persitencyAlert;
    private String persistencyRemarks;
    private String productReco;
    private String ticketSizeReco;
    private String qualityScore;
    private String qualityAlert;
    private String qualityRemarks;
    private String incomeSegment;
    private String digitalProfileScore;
    private String digitalProfileAlert;
    private String bureauMatch;
    private String bureauScore;
    private String bureauProfile;
    private String placeeholder1;
    private String placeholder2;
    private String placeholder3;
    private String placeholder4;
    private String isHighRisk;

    // --- IIB_QUEST ---
    private String questResultMatch;
    private String questCSVResponse;

}
