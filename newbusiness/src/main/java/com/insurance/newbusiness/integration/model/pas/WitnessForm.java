package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WitnessForm {

    private String address;
    private String name;
    private String vernacularDeclaration;
    private String date;
}
