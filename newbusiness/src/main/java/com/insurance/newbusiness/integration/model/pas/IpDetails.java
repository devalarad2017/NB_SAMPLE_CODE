package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpDetails {

    private List<Object> additionalAddressDetails;
    private Address currentAddress;
    private Address permanentAddress;
    private EducationAndOccupationDetails ipEducationAndOccupationDetails;
    private PepDetails ipPEPDetails;
    private IpPersonalDetailsWrapper ippersonalDetails;
    private String kycType;
}
