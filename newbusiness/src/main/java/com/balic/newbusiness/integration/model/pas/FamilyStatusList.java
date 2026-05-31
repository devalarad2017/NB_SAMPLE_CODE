package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FamilyStatusList {
	
	private String relationship;
	private String age;
	private String healthStatus;
	private String causeOfDeath;

}
