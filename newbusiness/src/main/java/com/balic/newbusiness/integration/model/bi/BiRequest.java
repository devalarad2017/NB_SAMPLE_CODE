package com.balic.newbusiness.integration.model.bi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiRequest {
    private String quoteId;
    private String applnNo;
    private String userId;
    private String ipAddress;
    private String module;
    private BasicInfo basicInfo;
    private List<Funds> funds;
    private List<Riders> riders;
    private List<InputOptions> inputOptions;

    @Data
    public static class BasicInfo {
        private String apiKey;

        @JsonProperty("li_name")
        private String liName;

        @JsonProperty("li_entry_age")
        private String liEntryAge;

        @JsonProperty("li_dob")
        private String liDob;

        @JsonProperty("li_gender")
        private String liGender;

        @JsonProperty("li_state")
        private String liState;

        @JsonProperty("mat_ben")
        private String matBen;

        @JsonProperty("partner_disc")
        private String partnerDisc;

        @JsonProperty("proposer_name")
        private String proposerName;

        @JsonProperty("proposer_age")
        private String proposerAge;

        @JsonProperty("proposer_dob")
        private String proposerDob;

        @JsonProperty("proposer_gender")
        private String proposerGender;

        private String kfd;
        private String pincode;
        private String sameproposer;

        @JsonProperty("company_state")
        private String companyState;

        private String gstin;

        @JsonProperty("gstin_number")
        private String gstinNumber;

        @JsonProperty("input_mode")
        private String inputMode;

        @JsonProperty("pr_id")
        private String prId;

        @JsonProperty("pr_pt")
        private String prPt;

        @JsonProperty("pr_ppt")
        private String prPpt;

        @JsonProperty("agent_id")
        private String agentId;

        @JsonProperty("li_mobileno")
        private String liMobileNo;

        @JsonProperty("li_emailid")
        private String liEmailId;

        @JsonProperty("pr_annprem")
        private String prAnnPrem;

        @JsonProperty("pr_mi")
        private String prMi;

        @JsonProperty("pr_sa")
        private String prSa;

        @JsonProperty("pr_samf")
        private String prSamf;

        @JsonProperty("pr_modalprem")
        private String prModalPrem;

        @JsonProperty("li_smoke")
        private String liSmoke;

        @JsonProperty("existing_customer")
        private String existingCustomer;

        @JsonProperty("pr_channel")
        private String prChannel;

    }

    @Data
    public static class InputOptions {
        private String optionId;
        private String optionValue;


        public InputOptions(String optionId, String optionValue) {
            this.optionId = optionId;
            this.optionValue = optionValue;
        }

        public InputOptions() {
        }
    }

    @Data
    public static class Funds {
        private String fundId;
        private String percent;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Riders {
        private String riderId;
        private String riderSA;
        private String riderPPT;
        private String riderPT;
    }
}
