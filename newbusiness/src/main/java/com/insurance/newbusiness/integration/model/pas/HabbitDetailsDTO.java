package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HabbitDetailsDTO {

    private Double bmi;
    private Double height;
    private Double weight;
    private Boolean isAlcohol;
    private Boolean isChangeInWeight;
    private Boolean isDGH;
    private Boolean isPEP;
    private Boolean isSmoker;
    private Boolean isTobacco;
}
