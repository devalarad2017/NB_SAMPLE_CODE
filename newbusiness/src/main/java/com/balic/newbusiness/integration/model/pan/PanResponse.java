package com.balic.newbusiness.integration.model.pan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PanResponse {

	private String status;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("pancard_list")
    private List<PanCardDto> pancardList;

	@Data
	public static class PanCardDto {
	    private String stringval1; 			//cas_pan_number
	    private String stringval2;			//cas_nsdl_pan_validation_status
	    private String stringval3;			//cas_name_matching_flag
	    private String stringval4;
	    private String stringval5;			//cas_dob_matching_flag
	    private String stringval6;			//cas_aadhar_seeding_flag
	    private String stringval7;
	    private String stringval8;
	    private String stringval9;
	    private String stringval10;
	    private String stringval11;
	    private String stringval12;
	    private String stringval13;
	    private String stringval14;
	    private String stringval15;
	    private String stringval16;
	    private String stringval17;
	    private String stringval18;
	    private String stringval19;
	    private String stringval20;
	    private String stringval21;
	    private String stringval22;
	    private String stringval23;
	    private String stringval24;
	    private String stringval25;
	    private String stringval26;
	    private String stringval27;
	    private String stringval28;
	    private String stringval29;
	    private String stringval30;
	    private String stringval31;
	    private String stringval32;
	    private String stringval33;
	    private String stringval34;
	    private String stringval35;
	    private String stringval36;
	    private String stringval37;
	    private String stringval38;
	    private String stringval39;
	    private String stringval40;
	}
}
