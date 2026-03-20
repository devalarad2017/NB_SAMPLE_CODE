package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PremiumBreakup {

    private String premiumAmount;
    private String taxAmount;
    private List<Object> rebateDetails;
}
