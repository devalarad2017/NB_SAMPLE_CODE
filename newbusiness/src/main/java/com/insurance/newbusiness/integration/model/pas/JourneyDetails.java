package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JourneyDetails {

    private String journeyType;
    private String status;
    private String otcRuleIndicator;
    private String stpRuleIndicator;
    private String fullPaymentDone;
    private String medicalRequirementIndicator;
    private String mandateIndicator;
    private String skipDocQC;
    private String otpIndicator;
    private String digitalPIVCIndicator;
    private String existingPolicyNumber;
    private String leadNo;
    private String pasaIndicator;
    private String quoteNo;
    private String loginUserCode;
    private String physicalCopyOpted;
    private Boolean digitalPIVCTriggerRequired;
    private String biPfOTPDate;
    private String digitalPIVCTriggerDate;
    private String ckycId;
    private String iccrAcceptFlag;
    private String proposalAcceptFlag;
    private Boolean bopFlag;
    private String isBSOApplicable;
    private String medicalType;
    private String drcCategory;
    private Boolean shortJourney;
    private Boolean pivcIndicator;
    private List<String> pivcTypes;
    private String customerSegment;
    private Boolean psfDefence;
    private String darpanId;
    private String bimaSugamId;
    private Boolean ivcFlag;
    private String npoFlag;
    private String islifeAssuredApplyingAbroad;
    private Boolean uwfirst;
    private Boolean bimaSugam;
}
