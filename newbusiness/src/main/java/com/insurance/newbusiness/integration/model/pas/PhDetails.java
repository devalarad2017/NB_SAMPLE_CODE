package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PhDetails {

    private List<Object> additionalAddressDetails;
    private Address currentAddress;
    private Boolean ipph;
    private Address permanentAddress;
    private EducationAndOccupationDetails phEducationAndOccupationDetails;
    private PepDetails phPEPDetails;
    private PhPersonalDetailsWrapper phPersonalDetails;
    private String relationshipToIP;
    private String kycType;
}
