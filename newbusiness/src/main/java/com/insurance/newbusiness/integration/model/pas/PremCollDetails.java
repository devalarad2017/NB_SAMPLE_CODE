package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PremCollDetails {

    private String proposalDeposit;
    private String renewalPaymentMethod;
    private String amountInWord;
    private String chequeNo;
    private String sisoFlag;
    private String date;
}
