package com.balic.newbusiness.domain.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * InboundRequest — maps the actual JSON structure received from partners.
 *
 * Partner sends:
 *   pInObj1_inout, pInObj2_inout, pInObj3_inout : maps with stringval1..150
 *   pInList1_inout through pInList6_inout       : lists of dynamic key-value records
 *
 * IMPORTANT: All three pInObj maps have the SAME keys (stringval1..stringval150).
 * getParams() prefixes them as obj1.stringval1, obj2.stringval1, obj3.stringval1
 * so they do NOT overwrite each other when merged into one Map.
 *
 * Mapping properties files use these prefixed keys:
 *   obj1.stringval13=applicantFirstName
 *   obj2.stringval46=height
 */
@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "NB application request from external partner")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundRequest {

    @JsonProperty("pInObj1_inout")
    private Map<String, String> pInObj1_inout;

    @JsonProperty("pInObj2_inout")
    private Map<String, String> pInObj2_inout;

    @JsonProperty("pInObj3_inout")
    private Map<String, String> pInObj3_inout;
    
    @JsonProperty("pInObj4_inout")
    private Map<String, String> pInObj4_inout;
    
    @JsonProperty("pInObj5_inout")
    private Map<String, String> pInObj5_inout;
    
    @JsonProperty("pInObj6_inout")
    private Map<String, String> pInObj6_inout;
    
    @JsonProperty("pInObj7_inout")
    private Map<String, String> pInObj7_inout;
    
    @JsonProperty("pInObj8_inout")
    private Map<String, String> pInObj8_inout;
    
    @JsonProperty("pInObj9_inout")
    private Map<String, String> pInObj9_inout;
    
    @JsonProperty("pInObj10_inout")
    private Map<String, String> pInObj10_inout;

    @JsonProperty("pInList1_inout")
    private PInList pInList1_inout;
    
    @JsonProperty("pInList2_inout")
    private PInList pInList2_inout;
    
    @JsonProperty("pInList3_inout")
    private PInList pInList3_inout;

    @JsonProperty("pInList4_inout")
    private PInList pInList4_inout;

    @JsonProperty("pInList5_inout")
    private PInList pInList5_inout;

    @JsonProperty("pInList6_inout")
    private PInList pInList6_inout;
    
    @JsonProperty("pInList7_inout")
    private PInList pInList7_inout;
    
    @JsonProperty("pInList8_inout")
    private PInList pInList8_inout;
    
    @JsonProperty("pInList9_inout")
    private PInList pInList9_inout;
    
    @JsonProperty("pInList10_inout")
    private PInList pInList10_inout;


    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PInList {

        @JsonProperty("WeoRecStrings150User")
        private List<WeoRecStrings150User> weoRecStrings150User;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeoRecStrings150User {
    	
        private Map<String, String> values = new HashMap<>();

        @JsonAnyGetter
        public Map<String, String> getValues() { 
        	return values; 
        }

        @JsonAnySetter
        public void setValue(String key, String value) { 
        	this.values.put(key, value); 
        }
    }


    /**
     * This is temporary set partner code value. USe actual param later.
     * Partner code from pInObj1_inout.stringval1
     * Sample: "TURTLEMINT"
     */
    public String getPartnerCode() {
        return pInObj1_inout != null ? pInObj1_inout.get("stringval1") : null;
    }

    /**
     * Merges all pInObj maps into one Map with PREFIXED keys to easy use.
     * pInObj1.stringval13 = "TEST"   → key = "obj1.stringval13"
     * Empty values are included — MappingService skips them during resolve.
     * This is the Map that JourneyContext stores and MappingService reads from.
     */
    public Map<String, String> getParams() {
    	
        Map<String, String> merged = new LinkedHashMap<>();
        addPrefixed(merged, "obj1", pInObj1_inout);
        addPrefixed(merged, "obj2", pInObj2_inout);
        addPrefixed(merged, "obj3", pInObj3_inout);
        addPrefixed(merged, "obj4", pInObj4_inout);
        addPrefixed(merged, "obj5", pInObj5_inout);
        addPrefixed(merged, "obj6", pInObj6_inout);
        addPrefixed(merged, "obj7", pInObj7_inout);
        addPrefixed(merged, "obj8", pInObj8_inout);
        addPrefixed(merged, "obj9", pInObj9_inout);

        addPrefixedList(merged, "list1", pInList1_inout);
        addPrefixedList(merged, "list2", pInList2_inout);
        addPrefixedList(merged, "list3", pInList3_inout);
        addPrefixedList(merged, "list4", pInList4_inout);
        addPrefixedList(merged, "list5", pInList5_inout);
        addPrefixedList(merged, "list6", pInList6_inout);
        addPrefixedList(merged, "list7", pInList7_inout);
        addPrefixedList(merged, "list8", pInList8_inout);
        addPrefixedList(merged, "list9", pInList9_inout);
        addPrefixedList(merged, "list10", pInList10_inout);

        
        return merged;
    }

    private void addPrefixed(Map<String, String> target, String prefix, Map<String, String> source) {
        if (source == null) return;
        
        for (Map.Entry<String, String> entry : source.entrySet()) {
            target.put(prefix + "." + entry.getKey(), entry.getValue());
        }
    }

    private void addPrefixedList(Map<String, String> target, String prefix, PInList listObj) {
        if (listObj == null) return;
        List<WeoRecStrings150User> records = listObj.getWeoRecStrings150User();
        if (records == null) return;

        for (int i = 0; i < records.size(); i++) {
            Map<String, String> recordValues = records.get(i).getValues();
            if (recordValues == null) continue;

            String recordPrefix = prefix + "[" + i + "]";
            for (Map.Entry<String, String> entry : recordValues.entrySet()) {
                target.put(recordPrefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * Direct access to a specific pInObj map — for cases where you need
     * to read a value manually without going through mapping properties.
     */
    public Map<String, String> getObj1() {
    	return pInObj1_inout; 
    }
    public Map<String, String> getObj2() {
    	return pInObj2_inout; 
    }
    public Map<String, String> getObj3() {
    	return pInObj3_inout; 
    }
    
    public PInList getList1() {
    	return pInList1_inout; 
    }
    
    public PInList getList4() {
    	return pInList4_inout; 
    }
    public PInList getList5() {
    	return pInList5_inout; 
    }
    
    public PInList getList6() {
    	return pInList6_inout; 
    }

	public Map<String, String> getpInObj4_inout() {
		return pInObj4_inout;
	}

	public void setpInObj4_inout(Map<String, String> pInObj4_inout) {
		this.pInObj4_inout = pInObj4_inout;
	}

	public Map<String, String> getpInObj5_inout() {
		return pInObj5_inout;
	}

	public void setpInObj5_inout(Map<String, String> pInObj5_inout) {
		this.pInObj5_inout = pInObj5_inout;
	}

	public Map<String, String> getpInObj6_inout() {
		return pInObj6_inout;
	}

	public void setpInObj6_inout(Map<String, String> pInObj6_inout) {
		this.pInObj6_inout = pInObj6_inout;
	}

	public Map<String, String> getpInObj7_inout() {
		return pInObj7_inout;
	}

	public void setpInObj7_inout(Map<String, String> pInObj7_inout) {
		this.pInObj7_inout = pInObj7_inout;
	}

	public Map<String, String> getpInObj8_inout() {
		return pInObj8_inout;
	}

	public void setpInObj8_inout(Map<String, String> pInObj8_inout) {
		this.pInObj8_inout = pInObj8_inout;
	}

	public Map<String, String> getpInObj9_inout() {
		return pInObj9_inout;
	}

	public void setpInObj9_inout(Map<String, String> pInObj9_inout) {
		this.pInObj9_inout = pInObj9_inout;
	}

	public Map<String, String> getpInObj10_inout() {
		return pInObj10_inout;
	}

	public void setpInObj10_inout(Map<String, String> pInObj10_inout) {
		this.pInObj10_inout = pInObj10_inout;
	}

	public PInList getpInList7_inout() {
		return pInList7_inout;
	}

	public void setpInList7_inout(PInList pInList7_inout) {
		this.pInList7_inout = pInList7_inout;
	}

	public PInList getpInList8_inout() {
		return pInList8_inout;
	}

	public void setpInList8_inout(PInList pInList8_inout) {
		this.pInList8_inout = pInList8_inout;
	}

	public PInList getpInList9_inout() {
		return pInList9_inout;
	}

	public void setpInList9_inout(PInList pInList9_inout) {
		this.pInList9_inout = pInList9_inout;
	}

	public PInList getpInList10_inout() {
		return pInList10_inout;
	}

	public void setpInList10_inout(PInList pInList10_inout) {
		this.pInList10_inout = pInList10_inout;
	}

	public PInList getpInList2_inout() {
		return pInList2_inout;
	}

	public void setpInList2_inout(PInList pInList2_inout) {
		this.pInList2_inout = pInList2_inout;
	}

	public PInList getpInList3_inout() {
		return pInList3_inout;
	}

	public void setpInList3_inout(PInList pInList3_inout) {
		this.pInList3_inout = pInList3_inout;
	}
}
