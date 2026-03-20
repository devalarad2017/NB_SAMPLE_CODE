package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PepDetails {

    private String relationShip;
    private String selfOrAssociate;
    private String typeOfPep;
    private String detail;
    private Boolean isPoliticallyExposed;
}
