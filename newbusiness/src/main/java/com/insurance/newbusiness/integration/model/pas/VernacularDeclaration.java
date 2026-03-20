package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VernacularDeclaration {

    private String address;
    private String customerPreferredLanguage;
    private String name;
    private String date;
}
