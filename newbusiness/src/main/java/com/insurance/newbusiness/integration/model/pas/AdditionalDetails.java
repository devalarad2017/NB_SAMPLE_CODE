package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdditionalDetails {

    private String existingCustomer;
    private String firstTimeBuyer;
    private String partnerDiscount;
    private String worksiteDiscount;
    private String vaccinationDiscount;
    private String premiumHolidays;
    private String maturityBenefit;
    private String isNRIGSTWaiverJourney;
    private String rateClass;
    private String isStaffAgent;
    private String onlineDirect;
    private String enhancedSumAssured;
    private String accountAggregatorDisc;
    private String isOnlineSales;
    private String isIncomePayoutDateApplicable;
    private String primeDiscount;
    private String pranNumber;
    private String craName;
    private String companyName;
    private String group;
    private String corporate;
    private String isSpecialLoading;
    private String isISNP;
}
