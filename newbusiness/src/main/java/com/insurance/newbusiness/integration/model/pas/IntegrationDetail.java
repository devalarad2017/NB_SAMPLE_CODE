package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationDetail {

    private String integrationName;
    private Map<String, Object> integrationResults;
    private Boolean integrationStatus;
}
