package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasResponse {

    private Object header;
    private PasResponseBody response;
    private List<Object> faults;
}
