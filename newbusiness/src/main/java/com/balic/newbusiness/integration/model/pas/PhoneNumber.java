package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhoneNumber {

    private String countryCode;
    private String number;
    private String stdCode;
    private String type;
}
