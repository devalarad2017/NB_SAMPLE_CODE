package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessVars {

    @JsonProperty("MDRT")
    private Boolean mdrt;

    @JsonProperty("PrefferedIC")
    private Boolean prefferedIC;

    @JsonProperty("PremReciptDate")
    private String premReciptDate;
}
