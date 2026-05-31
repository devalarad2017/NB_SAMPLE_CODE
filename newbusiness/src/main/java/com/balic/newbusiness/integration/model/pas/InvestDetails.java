package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvestDetails {

    private String amountInstallmentpremium;
    private String benefitCode;
    private String biNumber;
    private List<FundOptedFor> fundOptedFor;
    private String investmentStrategy;
    private String frequency;
    private String policyTerm;
    private String premiumPaymentTerm;
    private String viewBIPDF;
    private String familyBenefitPolicyNumber;
    private String familyBenefitFlag;
    private String familyBenefitRelationship;
    private String lifeBenefitOption;
    private Double lumpSumPercentage;
    private String maturityBenefitOption;
    private String biDate;
    private String biReceivedDate;
}
