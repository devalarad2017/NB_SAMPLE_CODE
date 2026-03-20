package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EiaDTO {

    private String applicationNumber;
    private String eiaAccountNumber;
    private Boolean eiaFlag;
    private String irAccountType;
    private String irNameType;
}
