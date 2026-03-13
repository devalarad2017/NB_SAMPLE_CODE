package com.insurance.newbusiness.integration.model.premium;
import lombok.Data;
import java.math.BigDecimal;
/**
 * PremiumResponse — received from PREMIUM_CALC_API.
 * calculatedPremium and frequency feed into ProposalRequest.
 * TODO: Replace with actual PREMIUM_CALC_API response fields.
 */
@Data
public class PremiumResponse {
    private BigDecimal calculatedPremium;  // annual premium amount
    private String     frequency;          // ANNUAL / SEMI_ANNUAL / QUARTERLY / MONTHLY
    private BigDecimal modalPremium;       // premium per frequency period
    private String     premiumRefId;
}
