package com.balic.newbusiness.exception;

import java.util.List;

public class MappingValidationException extends NewBusinessException {
    private final List<String> missingFields;
    public MappingValidationException(String targetApi, List<String> missingFields) {
        super("MAPPING_ERROR",
              "Mandatory fields missing for [" + targetApi + "]: " + missingFields);
        this.missingFields = missingFields;
    }
    public List<String> getMissingFields() { return missingFields; }
}
