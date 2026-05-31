package com.balic.newbusiness.journey;

import com.balic.newbusiness.integration.model.bi.BiRequest.Riders;
import com.balic.newbusiness.integration.model.bi.BiRequest.Funds;
import com.balic.newbusiness.integration.model.bi.BiRequest.InputOptions;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BiProductImpl {

    public String channel(@NonNull Map<String, String> params) {
        String channel = "Other";
        String productId = params.get("obj1.stringval10");
        String partnerCode = params.get("obj1.stringval1");
        String sysCode = params.get("obj1.stringval4");
        if (Arrays.asList("265", "267", "317")
                .contains(productId)) {
            channel = "Web";
        } else if ("345".equals(productId) && Arrays.asList("VIRTUAL_DG", "ROBINHOOD").contains(partnerCode)) {
            channel = "Offline";
        } else if ("319".equals(productId) && !"7000002372".equals(sysCode)) {
            channel = "WebSales";
        } else if (Arrays.asList("335", "343", "319", "383").contains(productId) && "7000002372".equals(sysCode)) {
            channel = "Other";
        } else if ("7000002372".equals(sysCode) && Arrays.asList("351", "365", "359").contains(productId)) {
            channel = "Offline";
        } else if (Arrays.asList("241", "249", "299", "287", "283", "335", "375").contains(productId)) {
            channel = "Web Sales";
        } else if (Arrays.asList("297", "339", "331", "311").contains(productId)) {
            channel = "Online Sales";
        } else if (Arrays.asList("343", "345").contains(productId) &&
                Arrays.asList("OKBIMA_DG", "WEALTHY_DG", "ALLIANCE_DG", "ASSET_PLUS_DG", "DEZTINATION_DG", "DYS_FAMILY_DG", "JIO_DG", "CHOLA_DG").contains(partnerCode)) {
            channel = "Online Sales";
        } else if (
                Arrays.asList("307", "309", "316", "329", "351", "355", "259", "367", "273", "365", "383", "377").contains(productId)
                        || "3000000007".equals(sysCode)
        ) {
            channel = "Online";
        } else if (
                Arrays.asList("321", "359").contains(productId) &&
                        Arrays.asList("JANA_BANK", "IDFC_UPSURE", "IDFC_RIGHTPRO", "DYS_FAMILY_DG", "TURTLEMINT_DG", "KARUR_BANK", "ZOPPER_DG", "CHOLA_DG").contains(partnerCode)
        ) {
            channel = "Online";
        } else {
            channel = "Other";
        }

        // Overrides
        String posChannel = null;
        if (params.get("obj3.stringval103") != null &&
                (params.get("obj3.stringval103").startsWith("1600") || params.get("obj3.stringval103").startsWith("4P"))) {
            posChannel = "POS";
        }
        if ("Offline".equalsIgnoreCase(params.get("obj6.stringval5") == null ? "" : params.get("obj6.stringval5"))
                && "POLICY_BAZAAR".equals(partnerCode)) {
            channel = "Offline";
        } else if ("POS".equals(posChannel)) {
            channel = "POS";
        }
        return channel;
    }

    public List<InputOptions> getInputOptions(Map<String, String> params) {
        String policyOption = null;
        String ropFlag = null;
        String adbcover = null;
        String ptadb = null;
        String pptadb = null;
        String saadb = null;
        String atpdbcover = null;
        String ptofatpdb = null;
        String pptofatpdb = null;
        String saatpdb = null;
        String cicover = null;
        String ptci = null;
        String saci = null;
        String wopic = null;
        String wopiic = null;
        String policyCoverOn = null;
        String vOption = null;

        List<InputOptions> inputOptions = new ArrayList<>();

        String productId = params.get("obj1.stringval10");
        List<Integer> indices1 = params.keySet().stream()
                .filter(k -> k.startsWith("list1[") && k.contains(".stringval1"))
                .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (Integer i : indices1) {
            String baseKey = "list1[" + i + "]";
            String riderCode = params.get(baseKey + ".stringval1");

            if (riderCode != null && !riderCode.trim().isEmpty()) {
                policyOption = getPolicyOption(productId, riderCode);
            }

            //v_option
            vOption = getVOption(policyOption, riderCode);

//ropFlag
            ropFlag = getRopFlag(productId, riderCode);
// For productId "321"
            if ("321".equals(productId)) {
                if ("R049A01".equals(riderCode) || "R064A01".equals(riderCode)) {
                    adbcover = "Y";
                    ptadb = params.get("obj1.stringval3");
                    pptadb = params.get("obj1.stringval4");
                    saadb = params.get("obj1.stringval2");
                }
                if ("R050A01".equals(riderCode) || "R065A01".equals(riderCode)) {
                    atpdbcover = "Y";
                    ptofatpdb = params.get("obj1.stringval3");
                    pptofatpdb = params.get("obj1.stringval4");
                    saatpdb = params.get("obj1.stringval2");
                }
                if ("R051A01".equals(riderCode) || "R066A01".equals(riderCode)) {
                    cicover = "Y";
                    ptci = params.get("obj1.stringval3");
                    saci = params.get("obj1.stringval2");
                }
                if ("R052A01".equals(riderCode) || "R067A01".equals(riderCode)) {
                    wopic = "Y";
                }
                if ("R053A01".equals(riderCode)) {
                    wopiic = "Y";
                }
            }

// For productId "316"
            if ("316".equals(productId)) {
                String stringval9 = params.get("obj1.stringval9");
                int ipAge = (params.get("obj1.stringval17") != null && !params.get("obj1.stringval17").trim().isEmpty())
                        ? Integer.parseInt(params.get("obj1.stringval17").trim()) : 0;
                if (stringval9 != null && !stringval9.trim().isEmpty()) {
                    if (("N".equals(stringval9) || "1".equals(stringval9)) && ipAge >= 18) {
                        policyCoverOn = "Wife";
                    } else if (("N".equals(stringval9) || "1".equals(stringval9)) && ipAge < 18) {
                        policyCoverOn = "Child";
                    } else {
                        policyCoverOn = "Self";
                    }
                }
            }
        }

        if ("321".equals(productId)) {
            inputOptions.add(new InputOptions("PolicyOption", ""));
            if(policyOption != null) {
                inputOptions.add(new InputOptions("PolicyOption", policyOption));
                if (!"InLC".equals(policyOption) && !"LCWJL".equals(policyOption) && !"LCWCE".equals(policyOption)) {
                    InputOptions retPBOption = new InputOptions();
                    retPBOption.setOptionId("RetPB");
                    // Use ropFlag if set, else fallback to stringval37, else "N"
                    String retPBValue = (ropFlag != null) ? ropFlag : (params.get("obj4.stringval37") != null && !(params.get("obj4.stringval37").trim().isEmpty()) ? params.get("obj4.stringval37") : "N");
                    retPBOption.setOptionValue(retPBValue);
                    inputOptions.add(retPBOption);
                }
                // JL block (if obj1.stringval11 == "JL")
                if ("JL".equals(params.get("obj1.stringval11") != null ? params.get("obj1.stringval11") : "N")) {
                    inputOptions.add(new InputOptions("JL", params.get("obj1.stringval41")));
                    inputOptions.add(new InputOptions("JLGender", params.get("obj1.stringval27")));
                    inputOptions.add(new InputOptions("WorkStatusJL", "Y"));
                    inputOptions.add(new InputOptions("spouseAge", params.get("obj1.stringval26")));
                    inputOptions.add(new InputOptions("smokeStatusJL", "S".equals(params.get("obj3.stringval1")) ? "Y" : params.get("obj3.stringval1")));
                    inputOptions.add(new InputOptions("NameSpouse", buildFullName(params.get("obj1.stringval22"), params.get("obj1.stringval23"), params.get("obj1.stringval24"))));
                }
                inputOptions.add(new InputOptions("ADBcover", adbcover));
                if("Y".equalsIgnoreCase(adbcover)) {
                    inputOptions.add(new InputOptions("PTADB", ptadb));
                    inputOptions.add(new InputOptions("PPTADB", (params.get("obj1.stringval39") != null && params.get("obj1.stringval39").trim().equals("01")) ? "1" : pptadb));
                    inputOptions.add(new InputOptions("SaADB", saadb));
                }
                inputOptions.add(new InputOptions("ATPDBCover", atpdbcover));
                if("Y".equalsIgnoreCase(atpdbcover)) {
                    inputOptions.add(new InputOptions("PTofATPDB", ptofatpdb));
                    inputOptions.add(new InputOptions("PPTofATPDB", (params.get("obj1.stringval39") != null && params.get("obj1.stringval39").trim().equals("01")) ? "1" : pptofatpdb));
                    inputOptions.add(new InputOptions("SaATPDB", saatpdb));
                }
                inputOptions.add(new InputOptions("CICover", cicover));
                if("Y".equalsIgnoreCase(cicover)) {
                    inputOptions.add(new InputOptions("PTCI", ptci));
                    inputOptions.add(new InputOptions("SaCI", saci));
                }
                inputOptions.add(new InputOptions("WOPIC", wopic));
                inputOptions.add(new InputOptions("WOPIIC", wopiic));
                inputOptions.add(new InputOptions("PerIncSA", params.get("obj4.stringval35")));
            }
            inputOptions.add(new InputOptions("plEdu", params.getOrDefault("obj1.stringval129", "")));
            inputOptions.add(new InputOptions("plOccup", params.getOrDefault("obj1.stringval131", "")));
            inputOptions.add(new InputOptions("plIncome", params.getOrDefault("obj1.stringval130", "")));
            // med: 'N' if stringval1 in ('PAYTM', 'POLICY_BAZAAR', 'BFS', 'ALLIANCEIBPL'), else 'Y'
            String partner = params.getOrDefault("obj1.stringval1", "");
            String med = ("PAYTM".equals(partner) || "POLICY_BAZAAR".equals(partner) || "BFS".equals(partner) || "ALLIANCEIBPL".equals(partner)) ? "N" : "Y";
            inputOptions.add(new InputOptions("med", med));
            inputOptions.add(new InputOptions("slEdu", params.getOrDefault("obj1.stringval141", "")));
            inputOptions.add(new InputOptions("slOcc", params.getOrDefault("obj1.stringval143", "")));
            inputOptions.add(new InputOptions("slIncome", params.getOrDefault("obj1.stringval142", "")));
        }
        else if ("297".equals(productId) && policyOption != null) {
            inputOptions.add(new InputOptions("plan", policyOption));
            inputOptions.add(new InputOptions("LBPayout", params.get("obj1.stringval32")));
            inputOptions.add(new InputOptions("ADBPayout", params.get("obj1.stringval33")));
        }
        else if("316".equals(productId)) {
            inputOptions.add(new InputOptions("policyCoverOn", policyCoverOn));
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            String spwValue = params.get("obj3.stringval5");
            inputOptions.add(new InputOptions("spwValue", (spwValue != null && !spwValue.trim().isEmpty()) ? spwValue : "0"));
            inputOptions.add(new InputOptions("spw", params.get("obj3.stringval7")));
        }
        else if("319".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("elc", params.get("obj4.stringval23")));
        }
        else if("317".equals(productId)) {
            inputOptions.add(new InputOptions("EnhancedSA", params.get("obj4.stringval23")));
        }
        else if ("249".equals(productId) || "287".equals(productId) || "299".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
        }
        else if ("311".equals(productId) || "301".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            String spwFlag = params.get("obj3.stringval7");
            if ("Y".equals(spwFlag != null ? spwFlag : "N")) {
                inputOptions.add(new InputOptions("spw_flag", spwFlag));
                inputOptions.add(new InputOptions("spw_perc", params.getOrDefault("obj3.stringval5", "")));
                inputOptions.add(new InputOptions("spw_year", params.getOrDefault("obj3.stringval8", "")));
                inputOptions.add(new InputOptions("spw_freq", params.getOrDefault("obj3.stringval6", "")));
            } else {
                inputOptions.add(new InputOptions("spw_flag", spwFlag != null ? spwFlag : "N"));
            }
        }
        else if("329".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption));
            inputOptions.add(new InputOptions("RetianGMI", params.getOrDefault("obj4.stringval40", "")));
            inputOptions.add(new InputOptions("RecGMI", params.getOrDefault("obj4.stringval41", "")));
            inputOptions.add(new InputOptions("RetainCB", params.getOrDefault("obj4.stringval50", "")));
            inputOptions.add(new InputOptions("RecCB", params.getOrDefault("obj4.stringval51", "")));
            inputOptions.add(new InputOptions("RTgmiFY", params.getOrDefault("obj4.stringval52", "")));
            inputOptions.add(new InputOptions("RTgmi2Y", params.getOrDefault("obj4.stringval53", "")));
            inputOptions.add(new InputOptions("RTcbFY", params.getOrDefault("obj4.stringval54", "")));
            inputOptions.add(new InputOptions("RTcb2Y", params.getOrDefault("obj4.stringval55", "")));
            inputOptions.add(new InputOptions("WCB", params.getOrDefault("obj4.stringval56", "N")));
            inputOptions.add(new InputOptions("WcbFY", params.getOrDefault("obj4.stringval57", "")));
            inputOptions.add(new InputOptions("Wcb2Y", params.getOrDefault("obj4.stringval58", "")));
            inputOptions.add(new InputOptions("per_WCB", params.getOrDefault("obj4.stringval59", "")));
            inputOptions.add(new InputOptions("W_GMI", params.getOrDefault("obj4.stringval60", "")));
            inputOptions.add(new InputOptions("WgmiFY", params.getOrDefault("obj4.stringval61", "")));
            inputOptions.add(new InputOptions("Wgmi2Y", params.getOrDefault("obj4.stringval62", "")));
            inputOptions.add(new InputOptions("per_WGMI", params.getOrDefault("obj4.stringval63", "")));
            inputOptions.add(new InputOptions("RTagls", params.getOrDefault("obj4.stringval64", "")));
            inputOptions.add(new InputOptions("RTaglsFY", params.getOrDefault("obj4.stringval65", "")));
            inputOptions.add(new InputOptions("RTagls2Y", params.getOrDefault("obj4.stringval66", "")));
            inputOptions.add(new InputOptions("W_agls", params.getOrDefault("obj4.stringval67", "")));
            inputOptions.add(new InputOptions("W_aglsFY", params.getOrDefault("obj4.stringval68", "")));
            inputOptions.add(new InputOptions("W_agls2Y", params.getOrDefault("obj4.stringval69", "")));
            inputOptions.add(new InputOptions("per_agls", params.getOrDefault("obj4.stringval70", "")));
            inputOptions.add(new InputOptions("setOpt", params.getOrDefault("obj4.stringval71", "")));
            inputOptions.add(new InputOptions("setPrd", params.getOrDefault("obj4.stringval72", "")));

            String jlValue = params.getOrDefault("obj1.stringval11", "N");
            if ("JL".equals(jlValue)) {
                inputOptions.add(new InputOptions("JL", "R029A01"));
                inputOptions.add(new InputOptions("nameJL", buildFullName(params.get("obj1.stringval22"), params.get("obj1.stringval23"), params.get("obj1.stringval24"))));
                inputOptions.add(new InputOptions("genderJL", params.getOrDefault("obj1.stringval27", "")));
                inputOptions.add(new InputOptions("ageJL", params.getOrDefault("obj1.stringval26", "")));
                inputOptions.add(new InputOptions("saJL", params.getOrDefault("obj1.stringval41", "")));
            } else {
                inputOptions.add(new InputOptions("JL", "1"));
            }
        }
        else if ("331".equals(productId)) {
            String jlFlag = params.getOrDefault("obj1.stringval11", "N");
            if ("JL".equals(jlFlag)) {
                // JL block: add name, gender, age for JL
                String nameJL = buildFullName(params.get("obj1.stringval22"), params.get("obj1.stringval23"), params.get("obj1.stringval24"));
                inputOptions.add(new InputOptions("nameJL", nameJL));
                inputOptions.add(new InputOptions("genderJL", params.getOrDefault("obj1.stringval27", "")));
                inputOptions.add(new InputOptions("ageJL", params.getOrDefault("obj1.stringval26", "")));
            } else {
                // Standard block: variant, RecLB, and spw fields
                inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
                inputOptions.add(new InputOptions("RecLB", params.getOrDefault("obj4.stringval76", "")));
                String spwFlag = params.getOrDefault("obj3.stringval7", "N");
                if ("Y".equals(spwFlag)) {
                    inputOptions.add(new InputOptions("spw_flag", spwFlag));
                    inputOptions.add(new InputOptions("spw_perc", params.getOrDefault("obj3.stringval5", "")));
                    inputOptions.add(new InputOptions("spw_year", params.getOrDefault("obj3.stringval8", "")));
                    inputOptions.add(new InputOptions("spw_freq", params.getOrDefault("obj3.stringval6", "")));
                } else {
                    inputOptions.add(new InputOptions("spw_flag", spwFlag));
                }
            }
        }
        else if ("343".equals(productId)) {
            inputOptions.add(new InputOptions("variant", params.getOrDefault("policy_option", "")));
            String dpValue = params.getOrDefault("obj4.stringval83", "");
            if (null != dpValue && !dpValue.isEmpty()) {
                inputOptions.add(new InputOptions("DP", dpValue));
            }
            inputOptions.add(new InputOptions("IP", params.getOrDefault("obj4.stringval109", "")));
            inputOptions.add(new InputOptions("PT", params.getOrDefault("obj1.stringval35", "")));
            String freq = params.getOrDefault("obj4.stringval110", "");
            inputOptions.add(new InputOptions("frequency", "01".equals(freq) ? "1" : freq));
            inputOptions.add(new InputOptions("ROP", ropFlag));
            inputOptions.add(new InputOptions("autopay", params.getOrDefault("obj4.stringval119", "N")));
            inputOptions.add(new InputOptions("choosedate", params.getOrDefault("obj4.stringval120", "N")));

            String selectDate;
            if ("Y".equals(params.getOrDefault("obj4.stringval120", "N"))) {
                // If you have a date formatter, use it here. Otherwise, just pass the value as is.
                selectDate = params.getOrDefault("obj4.stringval111", "");
            } else {
                selectDate = params.getOrDefault("obj4.stringval111", "");
            }
            inputOptions.add(new InputOptions("selectdate", selectDate));

            String sName = buildFullName(params.get("obj1.stringval22"), params.get("obj1.stringval23"), params.get("obj1.stringval24"));
            inputOptions.add(new InputOptions("SNAME", sName));
            inputOptions.add(new InputOptions("SAGE", params.getOrDefault("obj1.stringval26", "")));
            inputOptions.add(new InputOptions("SGENDER", params.getOrDefault("obj1.stringval27", "")));
        }
        else if ("335".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("POLICY_OPTION", params.getOrDefault("obj4.stringval80", "")));
            inputOptions.add(new InputOptions("BDPP", params.getOrDefault("obj4.stringval81", "")));
            inputOptions.add(new InputOptions("NOB", params.getOrDefault("obj4.stringval82", "")));

            String dpValue = params.getOrDefault("obj4.stringval83", "");
            if (dpValue != null && !dpValue.isEmpty()) {
                inputOptions.add(new InputOptions("DP", dpValue));
            }

            inputOptions.add(new InputOptions("AF", params.getOrDefault("obj4.stringval84", "")));

            String sName = buildFullName(params.get("obj1.stringval22"), params.get("obj1.stringval23"), params.get("obj1.stringval24"));
            inputOptions.add(new InputOptions("SNAME", sName));
            inputOptions.add(new InputOptions("SAGE", params.getOrDefault("obj1.stringval26", "")));
            inputOptions.add(new InputOptions("SGENDER", params.getOrDefault("obj1.stringval27", "")));
            inputOptions.add(new InputOptions("ROP_P", params.getOrDefault("obj6.stringval32", "")));
        }
        else if ("365".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("syIncome", params.getOrDefault("obj6.stringval41", "")));
            inputOptions.add(new InputOptions("IP", params.getOrDefault("obj4.stringval109", "")));
            inputOptions.add(new InputOptions("frequency", params.getOrDefault("obj4.stringval110", "")));
            inputOptions.add(new InputOptions("EGP", params.getOrDefault("obj6.stringval39", "")));
            inputOptions.add(new InputOptions("DP", params.getOrDefault("obj4.stringval83", "")));
            inputOptions.add(new InputOptions("options", params.getOrDefault("obj6.stringval97", "")));
            inputOptions.add(new InputOptions("insta_perc", params.getOrDefault("obj3.stringval54", "")));
        }
        else if ("315".equals(productId)) {
            inputOptions.add(new InputOptions("Option", policyOption != null ? policyOption : ""));
            // JLife: "2" if obj1.stringval11 == "JL", else "1"
            String jLifeValue = "JL".equals(params.getOrDefault("obj1.stringval11", "N")) ? "2" : "1";
            inputOptions.add(new InputOptions("JLife", jLifeValue));

            if ("JL".equals(params.getOrDefault("obj1.stringval11", "N"))) {
                inputOptions.add(new InputOptions("JL", "R029A01"));
                String nameJL = buildFullName(params.get("obj1.stringval22"), params.get("obj1.stringval23"), params.get("obj1.stringval24"));
                inputOptions.add(new InputOptions("nameJL", nameJL));
                inputOptions.add(new InputOptions("genderJL", params.getOrDefault("obj1.stringval27", "")));
                inputOptions.add(new InputOptions("ageJL", params.getOrDefault("obj1.stringval26", "")));
                inputOptions.add(new InputOptions("saJL", params.getOrDefault("obj1.stringval41", "")));
            } else {
                // The PL/SQL has an extra check for NOT IN ('315'), but since we're in the "315" block, this is always false.
                inputOptions.add(new InputOptions("JL", "1"));
            }
        }
        else if ("345".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("LBPayout", params.getOrDefault("obj1.stringval32", "")));
            inputOptions.add(new InputOptions("MBPayout", params.getOrDefault("obj4.stringval141", "")));

            // med: "Y" if obj4.stringval89 is "M", else "N"
            String medValue = "M".equals(params.getOrDefault("obj4.stringval89", "M")) ? "Y" : "N";
            inputOptions.add(new InputOptions("med", medValue));

            inputOptions.add(new InputOptions("adb", params.getOrDefault("obj4.stringval142", "")));
            inputOptions.add(new InputOptions("LBperc", params.getOrDefault("obj4.stringval143", "")));
            inputOptions.add(new InputOptions("smoker", params.getOrDefault("obj1.stringval8", "")));
            inputOptions.add(new InputOptions("Edu", params.getOrDefault("obj1.stringval129", "")));
            inputOptions.add(new InputOptions("Occup", params.getOrDefault("obj1.stringval131", "")));
            inputOptions.add(new InputOptions("Income", params.getOrDefault("obj1.stringval130", "")));
            inputOptions.add(new InputOptions("Installment", params.getOrDefault("obj6.stringval96", "")));
        }
        else if ("355".equals(productId)) {
            inputOptions.add(new InputOptions("smoker", params.getOrDefault("obj1.stringval8", "")));
            inputOptions.add(new InputOptions("Edu", params.getOrDefault("obj1.stringval129", "")));
            inputOptions.add(new InputOptions("Occup", params.getOrDefault("obj1.stringval131", "")));
            inputOptions.add(new InputOptions("Income", params.getOrDefault("obj1.stringval130", "")));
            // NVL(p_in_obj_4.stringval134, 'N')
            String salaried = params.getOrDefault("obj4.stringval134", "");
            inputOptions.add(new InputOptions("salaried", salaried.isEmpty() ? "N" : salaried));
        }
        else if ("359".equals(productId)) {
            inputOptions.add(new InputOptions("Dp", params.getOrDefault("obj4.stringval83", "")));
            inputOptions.add(new InputOptions("GPB", params.getOrDefault("obj5.stringval138", "")));
            inputOptions.add(new InputOptions("frequency", params.getOrDefault("obj4.stringval110", "")));
        }
        else if ("367".equals(productId)) {
            // NVL(p_in_obj_4.stringval110, p_in_obj_1.stringval37)
            String frequency = params.getOrDefault("obj4.stringval110", "");
            if (frequency.isEmpty()) {
                frequency = params.getOrDefault("obj1.stringval37", "");
            }
            inputOptions.add(new InputOptions("frequency", frequency));
        }
        else if ("375".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("DP", params.getOrDefault("obj4.stringval83", "")));
            inputOptions.add(new InputOptions("IP", params.getOrDefault("obj4.stringval109", "")));
            inputOptions.add(new InputOptions("frequency", params.getOrDefault("obj4.stringval110", "")));
            inputOptions.add(new InputOptions("options", vOption != null ? vOption : ""));
            // ROP logic
            String ropValue;
            if (("AI".equals(policyOption) || "SI".equals(policyOption)) && "POLICY_BAZAAR".equals(params.getOrDefault("obj1.stringval1", ""))) {
                ropValue = "Y";
            } else {
                String ropFlagValue = ropFlag != null ? ropFlag : params.getOrDefault("obj4.stringval37", "N");
                ropValue = ropFlagValue != null ? ropFlagValue : "N";
            }
            inputOptions.add(new InputOptions("ROP", ropValue));
            // ROP_P logic
            String ropPValue;
            if ("POLICY_BAZAAR".equals(params.getOrDefault("obj1.stringval1", "")) && ("AI".equals(policyOption) || "SI".equals(policyOption))) {
                ropPValue = "100";
            } else {
                ropPValue = params.getOrDefault("obj6.stringval32", "");
            }
            inputOptions.add(new InputOptions("ROP_P", ropPValue));
            // PT logic
            String ptValue = "WC".equals(policyOption) ? params.getOrDefault("obj1.stringval35", "") : null;
            inputOptions.add(new InputOptions("PT", ptValue != null ? ptValue : ""));
        }
        else if ("377".equals(productId)) {
            inputOptions.add(new InputOptions("options", policyOption != null ? policyOption : ""));
            String spwFlag = params.getOrDefault("obj3.stringval7", "");
            if ("Y".equals(spwFlag)) {
                inputOptions.add(new InputOptions("spw_flag", spwFlag));
                inputOptions.add(new InputOptions("spw_perc", params.getOrDefault("obj3.stringval5", "")));
                inputOptions.add(new InputOptions("spw_year", params.getOrDefault("obj3.stringval8", "")));
                inputOptions.add(new InputOptions("spw_freq", params.getOrDefault("obj3.stringval6", "")));
            } else {
                // NVL(p_in_obj_3.stringval7, 'N')
                inputOptions.add(new InputOptions("spw_flag", spwFlag.isEmpty() ? "N" : spwFlag));
            }
        }
        else if ("379".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("GPB", params.getOrDefault("obj5.stringval138", "")));
            inputOptions.add(new InputOptions("ICB", params.getOrDefault("obj3.stringval54", "")));
            inputOptions.add(new InputOptions("IP", params.getOrDefault("obj4.stringval109", "")));
            inputOptions.add(new InputOptions("syIncome", params.getOrDefault("obj6.stringval41", "")));
            inputOptions.add(new InputOptions("ISB_TYPE", params.getOrDefault("obj3.stringval55", "")));
            inputOptions.add(new InputOptions("frequency", params.getOrDefault("obj4.stringval110", "")));
            // NVL (v_rop_flag, NVL (p_in_obj_4.stringval37, 'N'))
            String rop = ropFlag != null ? ropFlag : params.getOrDefault("obj4.stringval37", "N");
            inputOptions.add(new InputOptions("ROP", rop));
            inputOptions.add(new InputOptions("ROP_P", params.getOrDefault("obj6.stringval32", "")));
        }
        else if ("259".equals(productId)) {
            inputOptions.add(new InputOptions("plOccup", params.getOrDefault("obj1.stringval131", "")));
            inputOptions.add(new InputOptions("plIncome", params.getOrDefault("obj1.stringval130", "")));
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("LBPayout", params.getOrDefault("obj1.stringval32", "")));
            inputOptions.add(new InputOptions("MBPayout", params.getOrDefault("obj4.stringval141", "")));
            inputOptions.add(new InputOptions("LBperc", params.getOrDefault("obj4.stringval143", "")));
            inputOptions.add(new InputOptions("SourceAccountAggregator", "")); // always empty string
            inputOptions.add(new InputOptions("Installment", params.getOrDefault("obj6.stringval96", "")));
        }
        else if ("351".equals(productId)) {
            inputOptions.add(new InputOptions("Edu", params.getOrDefault("obj1.stringval129", "")));
            inputOptions.add(new InputOptions("Occup", params.getOrDefault("obj1.stringval131", "")));
            inputOptions.add(new InputOptions("Income", params.getOrDefault("obj1.stringval130", "")));
        }
        else if ("353".equals(productId)) {
            inputOptions.add(new InputOptions("existing_customer", params.getOrDefault("obj4.stringval112", "")));
            inputOptions.add(new InputOptions("autopay", params.getOrDefault("obj4.stringval119", "N")));
            String spwFlag = params.getOrDefault("obj3.stringval7", "N");
            if ("Y".equals(spwFlag)) {
                inputOptions.add(new InputOptions("spw_flag", spwFlag));
                inputOptions.add(new InputOptions("spw_perc", params.getOrDefault("obj3.stringval5", "")));
                inputOptions.add(new InputOptions("spw_year", params.getOrDefault("obj3.stringval8", "")));
                inputOptions.add(new InputOptions("spw_freq", params.getOrDefault("obj3.stringval6", "")));
            } else {
                inputOptions.add(new InputOptions("spw_flag", spwFlag));
            }
        }
        else if ("307".equals(productId)) {
            String spwFlag = params.getOrDefault("obj3.stringval7", "N");
            if ("Y".equals(spwFlag)) {
                inputOptions.add(new InputOptions("spw_flag", spwFlag));
                inputOptions.add(new InputOptions("spw_perc", params.getOrDefault("obj3.stringval5", "")));
                inputOptions.add(new InputOptions("spw_year", params.getOrDefault("obj3.stringval8", "")));
                inputOptions.add(new InputOptions("spw_freq", params.getOrDefault("obj3.stringval6", "")));
            } else {
                inputOptions.add(new InputOptions("spw_flag", spwFlag));
            }
        } else if ("309".equals(productId)) {
            inputOptions.add(new InputOptions("MBPayout", params.getOrDefault("obj4.stringval141", "")));
            inputOptions.add(new InputOptions("IP", params.getOrDefault("obj4.stringval109", "")));
            inputOptions.add(new InputOptions("frequency", params.getOrDefault("obj4.stringval110", "")));
        } else if ("273".equals(productId)) {
            inputOptions.add(new InputOptions("variant", policyOption != null ? policyOption : ""));
            inputOptions.add(new InputOptions("plOccup", params.getOrDefault("obj1.stringval131", "")));
        }


        return inputOptions;
    }

    public static String getPolicyOption(String productId, String riderCode) {
        if (productId == null || riderCode == null) return null;

        // 1. Product 375: explicit mapping for each code
        if (productId.equals("375")) {
            // All relevant rider codes for 375
            if (riderCode.equals("RP11A01") || riderCode.equals("RP12A01") || riderCode.equals("RP12A02") ||
                    riderCode.equals("RP13A01") || riderCode.equals("RP13A02") || riderCode.equals("RP13A03") ||
                    riderCode.equals("RP13B01") || riderCode.equals("RP13B02") || riderCode.equals("RP13B03")) {
                if (riderCode.startsWith("RP13")) return "SI";
                if (riderCode.startsWith("RP11")) return "WC";
                if (riderCode.startsWith("RP12")) return "AI";
            }
        }
        // 2. RP14/RP15 codes (applies to all products)
        if (riderCode.equals("RP14A01") || riderCode.equals("RP14A02") || riderCode.equals("RP15A01")) {
            if (riderCode.contains("RP14")) return "EI";
            else if (riderCode.contains("RP15")) return "II";
        }
        // 3. Product 321
        if (productId.equals("321")) {
            if (riderCode.endsWith("A")) return "LC";
            if (riderCode.endsWith("B")) return "LCWCE";
            if (riderCode.endsWith("C")) return "LCWJL";
            if (riderCode.endsWith("D")) return "InLC";
        }
        // 4. Product 297
        else if (productId.equals("297")) {
            if (riderCode.endsWith("A")) return "shield";
            if (riderCode.endsWith("B")) return "plus";
            if (riderCode.endsWith("C")) return "super";
            if (riderCode.endsWith("D")) return "superme";
        }
        // 5. Products 316, 287, 299
        else if (productId.equals("316") || productId.equals("287") || productId.equals("299")) {
            if (riderCode.endsWith("A")) return "N";
            if (riderCode.endsWith("B")) return "Y";
        }
        // 6. Products 319, 249, 331
        else if (productId.equals("319") || productId.equals("249") || productId.equals("331")) {
            if (riderCode.endsWith("A")) return "Y";
            if (riderCode.endsWith("B")) return "N";
        }
        // 7. Product 329
        else if (productId.equals("329")) {
            if (riderCode.contains("81")) return "N";
            if (riderCode.contains("82")) return "Y";
        }
        // 8. Product 335
        else if (productId.equals("335")) {
            if (riderCode.endsWith("A")) return "LA";
            if (riderCode.endsWith("B")) return "LWROPDA";
            if (riderCode.contains("C01")) return "ACFIVE";
            if (riderCode.contains("C02")) return "ACTEN";
            if (riderCode.contains("C03")) return "ACFIFTEEN";
            if (riderCode.contains("C04")) return "ACTWENTY";
            if (riderCode.endsWith("D")) return "JLLSWFIFTY";
            if (riderCode.endsWith("E")) return "JLLSWHUNDRED";
            if (riderCode.endsWith("F")) return "JLLSWROP";
            if (riderCode.endsWith("G")) return "LAWROPDS";
            if (riderCode.endsWith("H")) return "LAWROPDIAS";
            if (riderCode.endsWith("I")) return "FP";
        }
        // 9. Product 315
        else if (productId.equals("315")) {
            if (riderCode.endsWith("A")) return "1";
            if (riderCode.endsWith("B")) return "2";
            if (riderCode.endsWith("C")) return "3";
            if (riderCode.endsWith("D")) return "4";
        }
        // 10. Product 343
        else if (productId.equals("343")) {
            if (riderCode.endsWith("A")) return "LI";
            if (riderCode.endsWith("B")) return "SI";
            if (riderCode.endsWith("C")) return "SUI";
            if (riderCode.endsWith("D")) return "EI";
            if (riderCode.endsWith("E")) return "WC";
            if (riderCode.endsWith("F")) return "AI";
        }
        // 11. Product 301
        else if (productId.equals("301")) {
            if (riderCode.endsWith("A")) return "WP";
            if (riderCode.endsWith("B")) return "WPC";
        }
        // 12. Product 345
        else if (productId.equals("345")) {
            if (riderCode.endsWith("A")) return "shield";
            if (riderCode.endsWith("B")) return "plus";
            if (riderCode.endsWith("C")) return "rop";
        }
        // 13. Product 259
        else if (productId.equals("259")) {
            if (riderCode.contains("A01")) return "life";
            if (riderCode.contains("A02")) return "lifeRop";
            if (riderCode.endsWith("C")) return "easy";
        }
        // 14. Product 311
        else if (productId.equals("311")) {
            if (riderCode.endsWith("A")) return "LI";
            if (riderCode.endsWith("B")) return "SI";
        }
        // 15. Product 365
        else if (productId.equals("365")) {
            if (riderCode.endsWith("A")) return "SI";
            if (riderCode.endsWith("B")) return "RI";
        }
        // 16. Product 273
        else if (productId.equals("273")) {
            if (riderCode.endsWith("A")) return "classic";
            if (riderCode.endsWith("B")) return "classic";
            if (riderCode.endsWith("C")) return "Assure";
            if (riderCode.endsWith("D")) return "Assure";
        }
        // 17. Product 377
        else if (productId.equals("377")) {
            if (riderCode.contains("01")) return "1";
            if (riderCode.contains("02")) return "2";
        }
        // 18. Product 379
        else if (productId.equals("379")) {
            if (riderCode.contains("14")) return "EI";
            if (riderCode.contains("15")) return "II";
        }
        // 19. Product 383
        else if (productId.equals("383")) {
            if (riderCode.endsWith("A")) return "A";
            if (riderCode.endsWith("B")) return "B";
            if (riderCode.endsWith("C")) return "C";
            if (riderCode.endsWith("D")) return "D";
            if (riderCode.endsWith("E")) return "E";
            if (riderCode.endsWith("F")) return "F";
            if (riderCode.endsWith("G")) return "G";
            if (riderCode.endsWith("H")) return "H";
            if (riderCode.endsWith("I")) return "I";
        }

        // Default: not found
        return null;
    }

    /**
     * Determines the value of v_rop_flag based on productId and riderCode.
     * Returns "Y" if the conditions are met, otherwise null.
     */
    @NonNull
    public static String getRopFlag(String productId, String riderCode) {
        if ("321".equals(productId) && Arrays.asList(
                "L177A03", "L177A06", "R064A01", "R065A01", "R066A01", "R067A01"
        ).contains(riderCode)) {
            return "Y";
        }
        if ("343".equals(productId) && "L190B02".equals(riderCode)) {
            return "Y";
        }
        if ("375".equals(productId) && Arrays.asList(
                "RP13B01", "RP13B02", "RP13B03"
        ).contains(riderCode)) {
            return "Y";
        }
        if ("379".equals(productId) && "RP14A01".equals(riderCode)) {
            return "Y";
        }
        return null;
    }

    public static String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder nameBuilder = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty() && !"null".equalsIgnoreCase(firstName)) {
            nameBuilder.append(firstName.trim());
        }
        if (middleName != null && !middleName.trim().isEmpty() && !"null".equalsIgnoreCase(middleName)) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty() && !"null".equalsIgnoreCase(lastName)) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(lastName.trim());
        }
        return nameBuilder.toString();
    }

    public static String getVOption(String policyOption, String riderCode) {
        String vOption = null;
        if ("SI".equals(policyOption)) {
            if ("RP13A01".equals(riderCode)) {
                vOption = "1";
            } else if ("RP13A02".equals(riderCode)) {
                vOption = "2";
            } else if ("RP13A03".equals(riderCode)) {
                vOption = "3";
            } else if ("RP13B01".equals(riderCode)) {
                vOption = "1";
            } else if ("RP13B02".equals(riderCode)) {
                vOption = "2";
            } else if ("RP13B03".equals(riderCode)) {
                vOption = "3";
            }
        } else if ("WC".equals(policyOption) && "RP11A01".equals(riderCode)) {
            vOption = "1";
        } else if ("AI".equals(policyOption)) {
            if ("RP12A01".equals(riderCode)) {
                vOption = "1";
            } else if ("RP12A02".equals(riderCode)) {
                vOption = "2";
            }
        }
        return vOption;
    }

    public List<Funds> getFunds(Map<String, String> params) {
        List<Funds> funds = new ArrayList<>();

        List<Integer> indices3 = params.keySet().stream()
                .filter(k -> k.startsWith("list3[") && k.contains(".stringval1"))
                .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        for (Integer i : indices3) {
            String baseKey = "list3[" + i + "]";
            Funds fund = new Funds();
            fund.setFundId(params.get(baseKey + ".stringval1"));
            fund.setPercent(params.get(baseKey + ".stringval2"));
            funds.add(fund);
        }
        return funds;
    }

    public List<Riders> getRiders(Map<String, String> params) {
        List<Riders> riders = new ArrayList<>();
        Set<String> opdRiderCodes = new HashSet<>(Arrays.asList(
                "R097A01", "R097B01", "R097C01", "R097D01", "R097E01",
                "R100A01", "R107A01", "R107B01", "R107C01", "R107D01", "R107E01"
        ));
        Set<String> skipRiderCodes = new HashSet<>(Arrays.asList(
                "R099A01", "R108A01", "R083A01", "R084A01", "R085A01", "R086A01", "R087A01", "R088A01", "R089A01"
        ));

        List<Integer> riderIndices1 = params.keySet().stream()
                .filter(k -> k.startsWith("list1[") && k.contains(".stringval1"))
                .map(k -> Integer.parseInt(k.substring(6, k.indexOf(']'))))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        String gwgFlag = "N";
        String fgiFlag = "N";
        String productId = params.getOrDefault("obj1.stringval10", "");


        for (Integer i : riderIndices1) {
            String baseKey = "list1[" + i + "]";
            String riderId = params.get(baseKey + ".stringval1");

            // v_gwg_flag logic
            if ("375".equals(productId) && Arrays.asList(
                    "RP11A01", "RP12A01", "RP12A02", "RP13A01", "RP13A02", "RP13A03",
                    "RP13B01", "RP13B02", "RP13B03"
            ).contains(riderId)) {
                gwgFlag = "Y";
            }
            // v_fgi_flag logic
            if (Arrays.asList("RP14A01", "RP14A02", "RP15A01").contains(riderId)) {
                fgiFlag = "Y";
            }
            if (riderId == null || !riderId.startsWith("R")) {
                continue;
            }

            // Product 316: skip all riders
            if ("316".equals(productId)) continue;
            // Product 331 + R048A01: skip
            if ("331".equals(productId) && "R048A01".equals(riderId)) continue;
            // Product 345 + R068A01: skip
            if ("345".equals(productId) && "R068A01".equals(riderId)) continue;
            // Skip specific rider codes
            if (skipRiderCodes.contains(riderId)) continue;
            // Skip if v_gwg_flag or v_fgi_flag is Y
            if ("Y".equalsIgnoreCase(gwgFlag) || "Y".equalsIgnoreCase(fgiFlag)) continue;
            // Product 351: only R080A01 and R081A01
            if ("351".equals(productId)) {
                if (!"R080A01".equals(riderId) && !"R081A01".equals(riderId)) continue;
                Riders rider = new Riders();
                rider.setRiderId(riderId);
                rider.setRiderSA(params.get(baseKey + ".stringval2"));
                riders.add(rider);
                continue;
            }
            Riders rider = new Riders();
            rider.setRiderId(riderId);
            if (opdRiderCodes.contains(riderId)) {
                rider.setRiderPPT(params.get(baseKey + ".stringval4"));
                rider.setRiderPT(params.get(baseKey + ".stringval3"));
            } else {
                rider.setRiderSA(params.get(baseKey + ".stringval2"));
            }
            riders.add(rider);
        }
        return riders;
    }
}