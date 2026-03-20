package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpPersonalDetailsWrapper {

    private PersonDetails ipBasicDetails;
    private ContactDetails contactDetails;
    private String preferredModeOfCommunication;
    private String proceedWithForm60;
    private String purposeOfInsurance;
    private String residentialStatus;
    private String cpId;
    private String lifeGoal;
}
