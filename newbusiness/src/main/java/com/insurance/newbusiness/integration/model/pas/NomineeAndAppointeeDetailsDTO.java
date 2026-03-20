package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NomineeAndAppointeeDetailsDTO {

    private AppointeeDetails appointeeDetails;
    private NomineeDetails nomineeDetails;
}
