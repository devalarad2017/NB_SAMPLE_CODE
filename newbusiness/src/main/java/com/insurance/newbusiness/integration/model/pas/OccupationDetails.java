package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OccupationDetails {

    private String annualIncome;
    private String businessDetails;
    private String employerAddress;
    private PhoneNumber employerContactNumber;
    private String employerName;
    private String groupCompanyName;
    private String industry;
    private String natureOfDuties;
    private String occupation;
    private String profession;
    private String relationShipToEmployee;
    private String websiteDetails;
    private String empCode;
    private String incomeProof;
    private String industryType;
    private String exactNatureOfDuties;
}
