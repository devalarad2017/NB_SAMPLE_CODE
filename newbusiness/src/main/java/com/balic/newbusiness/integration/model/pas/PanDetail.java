package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PanDetail {
	
	private String pan;
    private String panStatus;
    private String nameStatus;
    private String fatherNameStatus;
    private String dobStatus;
    private String seedingStatus;

}
