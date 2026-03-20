package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailsDTO {

    private Integer basicOrMain;
    private Integer bt;
    private Integer coverage;
    private List<Object> fundDetails;
    private String gmb;
    private String increaseInLifeCover;
    private Double multiplier;
    private String optionOrVariant;
    private String pensionMode;
    private String pensionOption;
    private Integer ppt;
    private String premFrequency;
    private String premiumAmount;
    private String premiumApportionment;
    private String productName;
    private String productType;
    private List<Object> riderDetails;
    private Integer spouseAge;
    private PremiumBreakupDetails premiumBreakupDetails;
    private MarkUpDetailsDTO markUpDetailsDTO;
    private String cashBonus;
    private Integer incomePeriod;
    private String incomePayoutFrequency;
    private String incomePayoutDate;
    private Integer defermentPeriod;
    private String extendedLifeCover;
    private String maturityOption;
    private String incomeStartYear;
    private String premiumPaidBy;
    private Double jointLifePremiumPayingTerm;
    private Double mainCoverageBasicSumAssured;
    private Double mainCoverageBasicGMB;
    private String baseCovPremiumAmount;
    private String spwOpted;
    private Integer spwPercentageOfFundValue;
    private String spwStartPolicyYear;
    private String frequencyOfSPW;
    private Double jointLifeSumAssured;
    private String incomePayoutFrequencyType;
    private Boolean accumulationIncomeBenefits;
    private List<Object> productOptions;
    private Boolean gpbOpted;
    private String baseCoverPremiumWithoutTaxAndRebate;
    private Double instaCashbackPercentage;
    private Double baseIncomeAmount;
    private Double incomeBoosterAmount;
    private String settlementOptions;
    private Double firstEGP;
    private Double subsequentEGP;
    private Double totalRGP;
    private String annuityType;
    private String payoutFrequency;
    private String returnOfPurchasePrice;
    private String enableJointAnnuitant;
    private String fundSourceType;
    private String annuityOption;
    private String jointPercentage;
    private String enableDisableAnnuity;
    private Double plannedAnnuity;
    private Double purchasePrice;
    private Double annualAnnuity;
    private Double plannedBasePremium;
    private String certainPeriod;
    private Double ropProportionPercentage;
    private String specialLoadingAmount;
    private String wealthMaturityBenfit;
}
