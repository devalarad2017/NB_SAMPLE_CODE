package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
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
