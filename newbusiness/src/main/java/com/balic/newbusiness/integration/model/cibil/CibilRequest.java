package com.balic.newbusiness.integration.model.cibil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * CibilRequest — sent to CIBIL API for credit bureau check.
 *
 * Field source reference (from cibil-internal-api-mapping.txt):
 *   appName               → obj1.stringval1  (partner code / app name)
 *   applicationNumber     → obj1.stringval6
 *   isCibilBypass         → hardcoded "False" or from config
 *   basicDetailsDto       → nested object with applicant details
 *   ipPhSame              → obj1.stringval9
 *
 * Fields marked [MANUAL] are set directly in JourneyOrchestrator
 * because they need logic or hardcoded values not in partner params.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CibilRequest {

    private Integer nbId;                          // [MANUAL] — no mapping, set from DB sequence or context
    private String appName;                     // obj1.stringval1
    private String applicationNumber;           // obj1.stringval6
    private String isCibilBypass;               // [MANUAL] — hardcoded "False" or from config
    private List<BasicDetailsDto> basicDetailsDto;  // nested — built manually in orchestrator
    private String ipPhSame;                    // obj1.stringval9
    private Boolean isMinorLife;                // [MANUAL] — no mapping
    private Boolean isWop;                      // [MANUAL] — no mapping

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BasicDetailsDto {

        private String applicantFirstName;      // obj1.stringval13
        private String applicantLastName;       // obj1.stringval15
        private String applicantMiddleName;     // obj1.stringval14
        private String gender;                  // obj1.stringval18
        private String dateOfBirth;             // obj1.stringval16
        private String idNumber;                // obj1.stringval122 (PAN)
        private String idType;                  // [MANUAL] — hardcoded "01" for PAN
        private String mobileNo;                // obj1.stringval19
        private String ipPhType;                // [MANUAL] — hardcoded "PH"
        private String emailAddress;            // obj1.stringval20
        private String residenceType;           // [MANUAL] — hardcoded "01"
        private String addressType;             // [MANUAL] — hardcoded "02"
        private String addressLine1;            // obj1.stringval56
        private String addressLine2;            // obj1.stringval57
        private String addressLine3;            // obj1.stringval58
        private String city;                    // obj1.stringval61
        private String pinCode;                 // obj1.stringval55
        private Long amount;                    // obj1.stringval40 (sum assured)
        private String stateCode;               // obj1.stringval62
        private String gstStateCode;            // obj1.stringval62
    }
}
