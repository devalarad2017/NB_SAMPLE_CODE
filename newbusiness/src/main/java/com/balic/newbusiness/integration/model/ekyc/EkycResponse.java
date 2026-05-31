package com.balic.newbusiness.integration.model.ekyc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EkycResponse {

	private String residentName;
    private String dateOfBirth;
    private String gender;
    private String commAddrLine1;
    private String commAddrLine2;
    private String applicationNo;
    private String mailId;
    private String mobileNo;
    private String source;
    private long uniqueId;
    private String aadhaarDob;
    private String aadhaarResident;
    private String aadhaarGender;
    private String aadhaarPhone;
    private String aadhaarEmail;
    private String aadhaarCareof;
    private String aadhaarLandmark;
    private String aadhaarLocality;
    private String aadhaarVtc;
    private String aadhaarDist;
    private String aadhaarHouseno;
    private String aadhaarStreet;
    private String aadhaarPo;
    private String aadhaarSubdist;
    private String aadhaarState;
    private String aadhaarCountry;
    private String aadhaarPin;
    private String createDate;
    private String nameMatchedFlag;
    private String dobMatchedFlag;
    private String aadhaarAuthFlag;
    private String errorCode;
    private String errorMessage;
    
}
