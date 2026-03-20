package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LifeStyleDetails {

    private Object ls01;
    private Object ls02;
    private String ls03;
    private Boolean ls04;
    private Boolean ls05;
    private Boolean ls06;
    private Boolean ls07;
    private Boolean ls08;
}
