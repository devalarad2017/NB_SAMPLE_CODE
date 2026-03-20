package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicPolicyInsured {

    private String gender;
    private String partyReferenceId;
    private String policyInsuredFirstName;
    private String policyInsuredLastName;
    private String policyInsuredLegalIdentifierCode;
    private String policyInsuredLegalIdentifierValue;
    private String policyInsuredMiddleName;
    private String salutation;
    private String suffix;
    private String policyInsuredDateOfBirth;
}
