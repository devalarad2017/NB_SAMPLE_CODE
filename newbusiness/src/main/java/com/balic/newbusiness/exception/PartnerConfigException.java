package com.balic.newbusiness.exception;

public class PartnerConfigException extends NewBusinessException {
    public PartnerConfigException(String message) {
        super("PARTNER_CONFIG_ERROR", message);
    }
}
