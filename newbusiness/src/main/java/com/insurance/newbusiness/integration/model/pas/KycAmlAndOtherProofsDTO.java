package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KycAmlAndOtherProofsDTO {

    private Long annualIncome;
    private String documentProvided;
    private String form60;
    private String gstin;
    private String pan;
    private String uniqueKYCIdentifier;
    private Boolean rcuFlag;
    private Boolean fpuFlag;
    private Boolean customerConsentFlag;
    private String backDate;
}
