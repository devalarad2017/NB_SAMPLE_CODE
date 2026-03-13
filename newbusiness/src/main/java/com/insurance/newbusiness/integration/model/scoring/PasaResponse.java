package com.insurance.newbusiness.integration.model.scoring;

import lombok.Data;

/**
 * PasaResponse — received from PASA_API (financial scoring).
 *
 * TODO: Replace field names with actual PASA_API response contract fields.
 */
@Data
public class PasaResponse {
    private String  status;       // APPROVED / DECLINED / REFER
    private Integer pasaScore;    // proprietary financial score
    private String  scoreGrade;   // A / B / C / D
    private String  remarks;
    private String  referenceId;
}
