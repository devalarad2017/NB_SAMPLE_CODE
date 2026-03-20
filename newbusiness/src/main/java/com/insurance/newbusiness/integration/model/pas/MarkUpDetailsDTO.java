package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarkUpDetailsDTO {

    private String highPremiumMarkUp;
    private String webMarkUp;
    private String autoPayMarkUp;
    private String familyBenefitMarkUp;
    private String partnerAndWorksiteMarketingMarkUp;
    private String staffMarkUp;
    private String loyaltyMarkUp;
    private String lowCoverBoosterMarkUp;
    private String onlineMarkUp;
    private String femaleLifeMarkUp;
    private String multipleProductOptionMarkup;
    private String customerSalesMarkup;
    private String agencyMarkUp;
    private String caMarkUp;
    private String posMarkUp;
    private String brokerMarkUp;
    private String directMarkUp;
    private String onlineSalesMarkUp;
    private String npsMarkUp;
    private String webAggegatorMarkUp;
    private String isFromExistingPensionPolicy;
    private String isISNPMarkUp;
}
