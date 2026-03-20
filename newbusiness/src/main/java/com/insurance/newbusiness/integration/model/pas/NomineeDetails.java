package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NomineeDetails {

    private String age;
    private String firstName;
    private String lastName;
    private String middleName;
    private String mobileNo;
    private String percentageOfThisNominee;
    private Object percentageShare;
    private String relationshipToLA;
    private String relationshipToinsure;
    private String salutation;
    private String title;
    private String dob;
}
