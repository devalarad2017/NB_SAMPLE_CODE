package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactDetails {

    private PhoneNumber alternateMobileNumber;

    @JsonProperty("emailId")
    private EmailAddress emailId;

    @JsonProperty("faceBook")
    private SocialMediaId faceBook;

    @JsonProperty("linkedInId")
    private SocialMediaId linkedInId;

    private PhoneNumber mobileNumber;

    private SocialMediaId skype;

    @JsonProperty("telNo")
    private PhoneNumber telNo;

    @JsonProperty("twitterId")
    private SocialMediaId twitterId;

    @JsonProperty("whatsApp")
    private PhoneNumber whatsApp;
}
