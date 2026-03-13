package com.insurance.newbusiness.integration.model.underwriting;
import lombok.Data;
import java.math.BigDecimal;
/** UnderwritingResponse — received from UNDERWRITING_API. TODO: Replace with actual fields. */
@Data
public class UnderwritingResponse {
    private String     decision;        // APPROVED / DECLINED / REFERRED
    private String     decisionCode;
    private BigDecimal premiumLoading;  // additional loading %
    private String     conditions;      // any special conditions / exclusions
    private String     uwRefId;
}
