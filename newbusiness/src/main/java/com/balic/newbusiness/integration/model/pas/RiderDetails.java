package com.balic.newbusiness.integration.model.pas;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiderDetails {
	
	private String rider;
	private String sumAssured;
	private String benefitTerm;
	private String premiumTerm;

}
