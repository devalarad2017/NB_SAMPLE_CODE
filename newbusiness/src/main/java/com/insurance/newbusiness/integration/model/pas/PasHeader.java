package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasHeader {

    private String containerId;
    private String correlationId;
    private String processTaskId;
    private ProcessVars processVars;
    private Object result;
    private String telemetryId;
}
