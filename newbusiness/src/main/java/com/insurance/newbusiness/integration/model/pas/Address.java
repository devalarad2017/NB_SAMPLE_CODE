package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    private String addressType;
    private String cityOrVillage;
    private String co;
    private String country;
    private String district;
    private String flag;
    private String flatOrDoorNo;
    private String landmark;
    private String nameOfPremises;
    private String pinCode;
    private String place;
    private String policeStation;
    private String postOrAreaOrNagar;
    private String roadOrStreetOrLane;
    private String state;
    private String townOrSuburbOrTaluka;
    private String addressSameAs;
    private String residingSince;
    private String addressProofDocumentType;
}
