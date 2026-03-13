package com.insurance.newbusiness.integration.model.scoring;

import lombok.Data;
import java.math.BigDecimal;

/**
 * EdcResponse — received from EDC_API (credit bureau check).
 *
 * Used in JourneyOrchestrator after parallel scoring completes:
 *   context.setEdcResult(edcFuture.get());
 *
 * Fields from this response that feed UNDERWRITING_API are set explicitly
 * in JourneyOrchestrator when building the UnderwritingRequest:
 *   uwReq.setCreditScore(context.getEdcResult().getCreditScore());
 *
 * TODO: Replace field names with actual EDC_API response contract fields.
 */
@Data
public class EdcResponse {
    private String     status;          // PASS / FAIL / REFER
    private Integer    creditScore;     // bureau score (e.g. 750)
    private String     creditBureau;    // CIBIL / EXPERIAN / EQUIFAX
    private BigDecimal outstandingDebt; // total existing debt from bureau
    private String     defaultHistory;  // NONE / MINOR / MAJOR
    private String     referenceId;     // EDC bureau query reference number
}
