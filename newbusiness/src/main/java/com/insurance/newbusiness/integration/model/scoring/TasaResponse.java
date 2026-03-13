package com.insurance.newbusiness.integration.model.scoring;

import lombok.Data;

/**
 * TasaResponse — received from TASA_API (risk assessment scoring).
 *
 * TODO: Replace field names with actual TASA_API response contract fields.
 */
@Data
public class TasaResponse {
    private String  status;          // LOW_RISK / MEDIUM_RISK / HIGH_RISK
    private Integer tasaScore;       // risk score
    private String  riskCategory;    // LOW / MEDIUM / HIGH
    private String  remarks;
    private String  referenceId;
}
