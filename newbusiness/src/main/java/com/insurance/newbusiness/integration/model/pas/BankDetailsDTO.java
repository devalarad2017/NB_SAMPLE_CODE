package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankDetailsDTO {

    private String accountHolderName;
    private String accountNo;
    private String accountType;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String modeOfPayment;
    private String isPennDropSuccessful;
    private String pennyDropTransactionDate;
    private String isNameVerified;
    private String pennyDropResponse;
    private String pennyDropAcconuntHolderName;
    private String onlineMandateApplicable;
    private String reason;
}
