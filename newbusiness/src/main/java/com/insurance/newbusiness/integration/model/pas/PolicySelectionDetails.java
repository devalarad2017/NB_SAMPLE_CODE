package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicySelectionDetails {

    private String employeeID;
    private Boolean kartaInsurable;
    private String kartaReason;
    private String policyType;
    private Boolean isEmployee;
}
