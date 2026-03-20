package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointeeDetails {

    private Integer age;
    private String firstName;
    private String lastName;
    private String middleName;
    private String mobileNumber;
    private String relationshipToNominee;
    private String dob;
}
