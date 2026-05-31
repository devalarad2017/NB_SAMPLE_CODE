package com.balic.newbusiness.integration.model.fes;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FesRequest {
	
	@JsonProperty("request_source")
    private String requestSource;

    @JsonProperty("ref_ID")
    private String refId;

    @JsonProperty("policy_number")
    private String policyNumber;

    @JsonProperty("service_ID")
    private String serviceId;

    @JsonProperty("data_details")
    private List<DataDetail> dataDetails;

    @Data
    public static class DataDetail {
    	 @JsonProperty("installment_premium")
    	    private String installmentPremium;

    	    @JsonProperty("billing_frequency")
    	    private String billingFrequency;

    	    @JsonProperty("declared_income")
    	    private String declaredIncome;
    	    
    	    @JsonProperty("cibil_score")
    	    private String cibilScore;

    	    private String age;

    	    @JsonProperty("product_type")
    	    private String productType;

    	    private String tap;
    	    private String tasa;

    	    @JsonProperty("total_portfolio_value")
    	    private String totalPortfolioValue;

    	    @JsonProperty("monthly_sip")
    	    private String monthlySip;

    	    @JsonProperty("property_type")
    	    private String propertyType;

    	    @JsonProperty("property_value")
    	    private String propertyValue;

    	    @JsonProperty("property_loan_type")
    	    private String propertyLoanType;

    	    @JsonProperty("monthly_emi")
    	    private String monthlyEmi;

    	    @JsonProperty("lic_tap")
    	    private String licTap;

    	    @JsonProperty("vehicle_type")
    	    private String vehicleType;

    	    @JsonProperty("vehicle_age_years")
    	    private String vehicleAgeYears;

    	    @JsonProperty("ex_showroom_price")
    	    private String exShowroomPrice;

    	    @JsonProperty("monthly_creditcard_limit")
    	    private String monthlyCreditCardLimit;

    	    @JsonProperty("net_profit")
    	    private String netProfit;

    	    private String city;
    	    
    	    @JsonProperty("data_array_details")
    	    private List<DataArrayDetail> dataArrayDetails;
    }

    @Data
    public static class DataArrayDetail {
        @JsonProperty("item_type")
        private String itemType;
        @JsonProperty("datefrom")
        private String datefrom;
        @JsonProperty("dateto")
        private String dateto;
        @JsonProperty("value")
        private String value;
    }

}
