package com.balic.newbusiness.integration.model.pas;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FundOptedFor {
	
	private String fundCode;
	private String fundName;
	private String percentageInvested;

}
