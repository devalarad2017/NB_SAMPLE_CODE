package com.balic.newbusiness.journey;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BiProductConfigLoader {

    private final Properties props = new Properties();

    public BiProductConfigLoader() {
        try (InputStream in = getClass().getResourceAsStream("/bi-product-codes.properties")) {
            if (in == null) throw new RuntimeException("bi-product-codes.properties not found in classpath");
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load bi-product-codes.properties", e);
        }
    }

    /**
     * Loads mappings for a given product and section (inputOptions, funds, riders).
     */
    public List<Map<String, String>> getSection(String productId, String section) {
        List<Map<String, String>> result = new ArrayList<>();
        String prefix = productId + "." + section + ".";
        // Collect matching keys and sort by index
        List<String> keys = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        keys.sort(Comparator.comparingInt(key -> {
            String idx = key.substring(prefix.length());
            try { return Integer.parseInt(idx); } catch (Exception e) { return Integer.MAX_VALUE; }
        }));

        for (String key : keys) {
            String val = props.getProperty(key, "").trim();
            if (val.isEmpty()) continue;
            String[] parts = val.split(":");
            Map<String, String> map = new HashMap<>();
            if ("inputOptions".equals(section) && parts.length == 2) {
                map.put("optionId", parts[0]);
                map.put("param", parts[1]);
                result.add(map);
            }
        }
        return result;
    }
}
