package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WitnessDetailsDTO {

    private ApplicationForm applicationForm;
    private VernacularDeclaration verniacDeclaration;
    private String ipPreferredLanguage;
    private String phPreferredLanguage;
}
