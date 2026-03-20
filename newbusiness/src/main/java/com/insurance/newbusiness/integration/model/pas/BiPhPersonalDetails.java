package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiPhPersonalDetails {

    private ContactDetails phContactDetails;
    private PersonDetails phPersonalDetails;
}
