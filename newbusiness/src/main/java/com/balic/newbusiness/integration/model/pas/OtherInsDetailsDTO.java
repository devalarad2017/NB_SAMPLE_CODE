package com.balic.newbusiness.integration.model.pas;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtherInsDetailsDTO {
	
	private String annualPremiumPaid;
	private String anyPrevPolicyPOOrDC;
	private String count;
	private Boolean existing;
	private String sumAssured;
	private String role;


}
