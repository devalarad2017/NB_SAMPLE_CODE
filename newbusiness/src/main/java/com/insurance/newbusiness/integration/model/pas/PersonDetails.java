package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonDetails {

    private Object age;
    private String countryOfBirth;
    private String countryOfResidence;
    private String fatherName;
    private String firstName;
    private String gender;
    private String lastName;
    private String maidenNameOfFemale;
    private String maritalStatus;
    private String middleName;
    private String motherName;
    private String nameOfSpouse;
    private String nationality;
    private String placeOfBirth;
    private String salutation;
    private String suffix;
    private String pan;
    private String idProofDoc;
    private String idProofValue;
    private String dateOfBirth;
    private String spousePlaceOfBirth;
}
