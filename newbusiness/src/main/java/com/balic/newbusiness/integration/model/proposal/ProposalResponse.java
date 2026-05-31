package com.balic.newbusiness.integration.model.proposal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProposalResponse {

    private String base64File;
    private String status;
    private String extension;
    private String message;
    private String fileSize;
}
