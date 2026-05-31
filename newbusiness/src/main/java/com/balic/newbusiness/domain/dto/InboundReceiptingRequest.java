package com.balic.newbusiness.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundReceiptingRequest {

    private Map<String, String> pnlObj1;

    private Map<String, String> pnlObj2;

    private Map<String, String> pnlObj3;

    private Map<String, String> pnlObj4;

    private Map<String, String> pnlObj5;

    private Map<String, String> pnlObj6;

    private Map<String, String> pnlObj7;

    private Map<String, String> pnlObj8;

    private Map<String, String> pnlObj9;

    private Map<String, String> pnlObj10;

    public String getPartnerCode() {
        return pnlObj1 != null ? pnlObj1.get("stringval1") : null;
    }

    public Map<String, String> getParams() {

        Map<String, String> merged = new LinkedHashMap<>();
        addPrefixed(merged, "pnlObj1", pnlObj1);
        addPrefixed(merged, "pnlObj2", pnlObj2);
        addPrefixed(merged, "pnlObj3", pnlObj3);
        addPrefixed(merged, "pnlObj4", pnlObj4);
        addPrefixed(merged, "pnlObj5", pnlObj5);
        addPrefixed(merged, "pnlObj6", pnlObj6);
        addPrefixed(merged, "pnlObj7", pnlObj7);
        addPrefixed(merged, "pnlObj8", pnlObj8);
        addPrefixed(merged, "pnlObj9", pnlObj9);
        addPrefixed(merged, "pnlObj10", pnlObj10);

        return merged;
    }

    private void addPrefixed(Map<String, String> target, String prefix, Map<String, String> source) {
        if (source == null) return;

        for (Map.Entry<String, String> entry : source.entrySet()) {
            target.put(prefix + "." + entry.getKey(), entry.getValue());
        }
    }
}
