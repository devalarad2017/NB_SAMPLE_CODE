package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionnaireDetails {

    private String role;
    private DghDetails dgh;
    private LifeStyleDetails lifeStyle;
}
