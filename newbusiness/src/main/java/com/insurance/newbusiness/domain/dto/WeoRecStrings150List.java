package com.insurance.newbusiness.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class WeoRecStrings150List {

    @JsonProperty("WeoRecStrings150User")
    private List<WeoRecStrings150> weoRecStrings150User;
}
