package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayerDetails {

    private String annualIncome;
    private String existingPremium;
    private Boolean ippayer;
    private String pan;
    private Address payerAddress;
    private PayerPersonalDetailsWrapper payerPersonalDetails;
    private Boolean phpayer;
    private String relationshipToIP;
    private Boolean phPayerSame;
    private String kycType;
}
