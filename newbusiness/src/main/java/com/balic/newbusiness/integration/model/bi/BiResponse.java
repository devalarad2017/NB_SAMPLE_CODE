package com.balic.newbusiness.integration.model.bi;


import java.util.List;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiResponse {

	@JsonProperty("balic_Quoteid")
    private String balicQuoteid;

    private double emr;

    @JsonProperty("mat_ben4per")
    private String matBen4per;

    @JsonProperty("salary_discount")
    private double salaryDiscount;

    @JsonProperty("insuranceAll_discount")
    private double insuranceAllDiscount;

    @JsonProperty("exist_cust_discount")
    private double existCustDiscount;

    @JsonProperty("autopay_discount")
    private double autopayDiscount;

    @JsonProperty("online_discount")
    private double onlineDiscount;

    @JsonProperty("staff_discount")
    private double staffDiscount;

    @JsonProperty("siso_discount")
    private double sisoDiscount;

    private String productUIN;
    private String medicalFlag;

    @JsonProperty("monthly_income")
    private double monthlyIncome;

    private double lumpsum;
    private int months;

    @JsonProperty("worksite_markup")
    private double worksiteMarkup;

    @JsonProperty("mb_installment")
    private double mbInstallment;

    @JsonProperty("prime_discount")
    private double primeDiscount;

    @JsonProperty("db_SumAssured")
    private int dbSumAssured;
    
    private int sumAssured;

    @JsonProperty("db_SumAssured_JL")
    private int dbSumAssuredJl;

    @JsonProperty("modal_Prem_With_Rider")
    private double modalPremWithRider;

    @JsonProperty("vaccine_discount")
    private double vaccineDiscount;

    @JsonProperty("discount_premium")
    private double discountPremium;

    private int val4per;
    private int val8per;

    @JsonProperty("death_ben4")
    private int deathBen4;

    @JsonProperty("death_ben8")
    private int deathBen8;

    @JsonProperty("tot_matben4")
    private int totMatben4;

    @JsonProperty("tot_matben8")
    private int totMatben8;

    @JsonProperty("instalment_income")
    private int instalmentIncome;

    private String message;
    private String transactionId;
    private String status;
    private List<ErrorMessage> errorMessage;
    private String step;
    private double tax;

    @JsonProperty("modal_PREM")
    private double modalPrem;
    
    private int gmb;

    @JsonProperty("nri_Loading")
    private double nriLoading;

    @JsonProperty("modal_prem_tax_disc")
    private double modalPremTaxDisc;

    @JsonProperty("installment_Income")
    private int installmentIncomeActual;

    @JsonProperty("guar_PO_INSTALLMENT")
    private int guarPoInstallment;

    @JsonProperty("modal_prem2year_tax")
    private double modalPrem2yearTax;

    private String quotationId;

    @JsonProperty("modal_prem_tax")
    private double modalPremTax;

    private String annualPremium;
    
    @Data
    public static class ErrorMessage {
        private String key;
        private Object value;
    }
}
