package com.balic.newbusiness.mapping;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// =============================================================================
// PROPERTY-FILE BASED MAPPING 
// =============================================================================
//
// How it works:
//   - Each partner + api combination has a .properties file
//   - File name: {partnerCode}-{targetApi}.properties
//   - Fallback:  default-{targetApi}.properties
//   - Each line: stringvalN=targetFieldName
//     e.g. stringval1=firstName
//
// resolveAs() reads the properties, renames keys in the raw param map, then uses
// ObjectMapper.convertValue() to build the typed POJO .
//
// Adding a new partner: create mapping/NEW_PARTNER-UCS_API.properties
// Adding a new API:     create mapping/default-NEW_API.properties
// No code changes needed for either.
//
// Unmapped/blank fields are skipped with a WARN log. Handle null blank if required.
// =============================================================================
@Service
public class MappingService {

    private static final Logger log = LoggerFactory.getLogger(MappingService.class);
    private static final String MAPPING_DIR = "mapping/";

    @Autowired
    private ObjectMapper objectMapper;

    // Cache: "PARTNER_PB::UCS_API" → {stringval1=firstName, stringval2=lastName}
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        log.info("MappingService initialized — property-file based mapping active");
    }

    // =========================================================================
    // resolveAs() — maps partner's raw params to a typed POJO.
    //
    
    // Usage:
    //   UCSRequest req = mappingService.resolveAs(
    //       "PARTNER_PB", "UCS_API", rawParams, UCSRequest.class);
    //
    // After this, you can manually set any extra fields not in the properties:
    //   req.setUCSId(context.getUCSResult().getUCSId());
    // =========================================================================
    public <T> T resolveAs(String partnerCode,
                            String targetApi,
                            Map<String, String> rawParams,
                            Class<T> targetClass) {

        Map<String, Object> resolvedMap = resolve(partnerCode, targetApi, rawParams);
        return objectMapper.convertValue(resolvedMap, targetClass);
    }

    // =========================================================================
    // resolve() — returns mapped field names with values as a flat Map.
    // Use this when you want to inspect or manually tweak before POJO conversion.
    // =========================================================================
    public Map<String, Object> resolve(String partnerCode,
                                       String targetApi,
                                       Map<String, String> rawParams) {

        Map<String, String> fieldMappings = getFieldMappings(partnerCode, targetApi);

        if (fieldMappings.isEmpty()) {
            log.warn("No mappings found for partner={} api={}. " +
                     "Check mapping/*.properties files.", partnerCode, targetApi);
            return Collections.emptyMap();
        }

        Map<String, Object> resolved = new LinkedHashMap<>();

        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String sourceParam = mapping.getKey();    // e.g. stringval1
            String targetField = mapping.getValue();  // e.g. firstName

            String rawValue = rawParams.get(sourceParam);

            if (rawValue == null || rawValue.trim().isEmpty()) {
                log.debug("Blank/missing source={} for target={} — skipped", sourceParam, targetField);
                continue;
            }

            resolved.put(targetField, rawValue.trim());
        }

        log.info("Mapped {}/{} fields | partner={} api={}",
                resolved.size(), rawParams.size(), partnerCode, targetApi);

        return resolved;
    }

    // =========================================================================
    // getFieldMappings() — loads and caches properties for a partner+api pair.
    //
    // Lookup order:
    //   1. mapping/{partnerCode}-{targetApi}.properties  (partner-specific)
    //   2. mapping/default-{targetApi}.properties         (fallback for all)
    // =========================================================================
    private Map<String, String> getFieldMappings(String partnerCode, String targetApi) {
        String cacheKey = partnerCode + "::" + targetApi;

        return cache.computeIfAbsent(cacheKey, key -> {

            // Try partner-specific file first
            String partnerFile = partnerCode + "-" + targetApi + ".properties";
            Map<String, String> mappings = loadPropertiesFile(partnerFile);

            if (!mappings.isEmpty()) {
                log.info("Loaded {} mappings from {}", mappings.size(), partnerFile);
                return mappings;
            }

            // Fallback to default file
            String defaultFile = "default-" + targetApi + ".properties";
            mappings = loadPropertiesFile(defaultFile);

            if (!mappings.isEmpty()) {
                log.info("Using {} default mappings from {} for partner={}",
                        mappings.size(), defaultFile, partnerCode);
            } else {
                log.warn("No mapping file found for partner={} api={}", partnerCode, targetApi);
            }

            return mappings;
        });
    }

    private Map<String, String> loadPropertiesFile(String fileName) {
        String path = MAPPING_DIR + fileName;
        ClassPathResource resource = new ClassPathResource(path);

        if (!resource.exists()) {
            return Collections.emptyMap();
        }

        Properties props = new Properties();
        try (InputStream is = resource.getInputStream()) {
            props.load(is);
        } catch (IOException e) {
            log.error("Failed to load mapping file: {}", path, e);
            return Collections.emptyMap();
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (String propKey : props.stringPropertyNames()) {
            map.put(propKey, props.getProperty(propKey));
        }
        return map;
    }

    // =========================================================================
    // Cache management — call from admin endpoint to reload without restart.
    // =========================================================================
    public void reloadAll() {
        cache.clear();
        log.info("MappingService cache cleared — properties will reload on next request");
    }

    public void reloadPartner(String partnerCode) {
        cache.entrySet().removeIf(e -> e.getKey().startsWith(partnerCode + "::"));
        log.info("Cleared cached mappings for partner={}", partnerCode);
    }
}