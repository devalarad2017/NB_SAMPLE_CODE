package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeclarationUnderFATCADTO {

    private String address;
    private String countryOfResidence;
    private Boolean documentSubmitted;
    private String documentType;
    private String name;
    private Boolean residentOfOtherCountry;
    private Boolean taxResidentOfACountry;
    private String tinNo;
    private String tinNoCountry;
    private String role;
    private String mobileNoOutsideIndia;
    private String landLineNoOutsideIndia;
    private String accountDetailsOutsideIndia;
    private String powerOfAttorneyName;
    private String powerOfAttorneyAddress;
    private String powerOfAttorneyContactNo;
    private String holdMailAddress;
}
