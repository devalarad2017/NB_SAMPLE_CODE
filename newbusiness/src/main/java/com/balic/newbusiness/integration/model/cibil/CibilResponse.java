package com.balic.newbusiness.integration.model.cibil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * CibilResponse — received from CIBIL API after credit bureau check.
 * Fields are populated by RestTemplate deserialization.
 * Used by later stages (scoring, underwriting) for credit data.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CibilResponse {

    private Integer cibilId;
    private Integer nbId;
    private String applnNumber;
    private String score;
    private Integer trlScore;
    private String scoreName;
    private String cibilStatus;
    private String role;
    private String module;
}
