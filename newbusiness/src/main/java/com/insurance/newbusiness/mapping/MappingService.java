package com.insurance.newbusiness.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.newbusiness.domain.entity.PartnerFieldMapping;
import com.insurance.newbusiness.exception.MappingValidationException;
import com.insurance.newbusiness.repository.PartnerFieldMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// =============================================================================
// MappingCache
// =============================================================================
//
// Loads ALL active rows from partner_field_mapping table at application startup.
// Stored in-memory as a HashMap — zero DB calls per request.
//
// CACHE KEY: "partnerCode::targetApi"
// CACHE VALUE: List of all mapping rows for that combination
//
// Example:
//   "PARTNER_A::MEDICAL_API" → [
//       {stringval1 → firstName, STRING, mandatory, TRIM},
//       {stringval3 → dateOfBirth, DATE, mandatory, dd/MM/yyyy},
//       {stringval45 → panNumber, STRING, mandatory, UPPERCASE}
//   ]
//
// WHY IN-MEMORY: 600 params × 10 APIs × N partners = potentially thousands of rows.
// Loading once and caching avoids N DB queries per incoming request.
//
// REFRESH: Restart the application after adding new mapping rows to DB.
// A manual refresh endpoint can be added later if needed.
// =============================================================================
@Component
class MappingCache {

    private static final Logger log = LoggerFactory.getLogger(MappingCache.class);

    @Autowired
    private PartnerFieldMappingRepository repository;

    // key = "partnerCode::targetApi", value = list of field mappings
    private final Map<String, List<PartnerFieldMapping>> cache = new HashMap<>();

    @PostConstruct
    public void load() {
        List<PartnerFieldMapping> all = repository.findByActiveTrue();
        cache.clear();
        for (PartnerFieldMapping m : all) {
            String key = buildKey(m.getPartnerCode(), m.getTargetApi());
            cache.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }
        log.info("MappingCache loaded — {} partner+api combinations | {} total rows",
                cache.size(), all.size());
    }

    public List<PartnerFieldMapping> getMappings(String partnerCode, String targetApi) {
        List<PartnerFieldMapping> mappings = cache.get(buildKey(partnerCode, targetApi));
        return mappings != null ? mappings : Collections.emptyList();
    }

    private String buildKey(String partnerCode, String targetApi) {
        return partnerCode + "::" + targetApi;
    }
}

// =============================================================================
// ParameterMappingService
// =============================================================================
//
// The BRIDGE between partner's 600 generic stringvalN params and the typed
// POJO expected by each downstream API.
//
// ── HYBRID ARCHITECTURE EXPLAINED ───────────────────────────────────────────
//
// Problem:  Partner sends stringval1=John, stringval3=12/03/1985, stringval45=abc123
// Solution: DB table says stringval1 → firstName, stringval3 → dateOfBirth, etc.
//           MappingService resolves these into a Map {"firstName":"John", ...}
//           ObjectMapper.convertValue() turns the Map into the typed POJO.
//
// CRITICAL CONSTRAINT:
//   target_field in partner_field_mapping DB table MUST exactly match the
//   Java field name in the POJO class. Both must be camelCase.
//
//   DB:   target_field = "dateOfBirth"
//   POJO: private LocalDate dateOfBirth;   ← same name, Jackson maps automatically
//
//   If names differ → field will be null in the POJO silently.
//   Always verify when adding new API mappings.
//
// ── RESOLUTION PRIORITY (for each field, highest to lowest) ─────────────────
//   1. Raw partner value     — actual stringvalN value from inbound request
//   2. default_value         — fallback from DB when partner sends blank/null
//   3. Mandatory check       — if still null, collect ALL missing and throw once
//   4. Not mandatory + blank — field omitted from Map (POJO field stays null)
//
// NOTE: enrichedParams (outputs from prior API steps) are now handled directly
// in JourneyOrchestrator — the orchestrator sets fields like medicalScore on the
// next API's request POJO after resolveAs() returns. This is more explicit and
// readable than the previous PostProcessor/enrichedParams pattern.
// =============================================================================
@Service
public class ParameterMappingService {

    private static final Logger log = LoggerFactory.getLogger(ParameterMappingService.class);

    @Autowired
    private MappingCache mappingCache;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // resolveAs() — the main method used by JourneyOrchestrator
    //
    // Resolves stringvalN → typed POJO in one call.
    //
    // Usage in JourneyOrchestrator:
    //   MedicalRequest req = mappingService.resolveAs(
    //       context.getPartnerCode(), "MEDICAL_API",
    //       context.getRawParams(), MedicalRequest.class);
    //
    // Then optionally set fields from prior API responses:
    //   req.setSomePriorField(context.getEligibilityResult().getSomeField());
    //
    // @param partnerCode  e.g. "PARTNER_A"
    // @param targetApi    must match the target_api values in partner_field_mapping table
    // @param rawParams    the 600 stringval1..N from the inbound request
    // @param targetClass  the POJO class to populate — field names must match target_field in DB
    // ─────────────────────────────────────────────────────────────────────────
    public <T> T resolveAs(String partnerCode,
                            String targetApi,
                            Map<String, String> rawParams,
                            Class<T> targetClass) {

        Map<String, Object> resolvedMap = resolve(partnerCode, targetApi, rawParams);

        // objectMapper.convertValue: fills POJO fields by matching Map key to field name.
        // No reflection code needed. Jackson handles type coercion automatically.
        // Prerequisite: target_field in DB == Java field name in POJO (camelCase).
        return objectMapper.convertValue(resolvedMap, targetClass);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resolve() — returns raw Map if you need it before building the POJO.
    // Normally you use resolveAs() directly.
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> resolve(String partnerCode,
                                       String targetApi,
                                       Map<String, String> rawParams) {

        List<PartnerFieldMapping> mappings = mappingCache.getMappings(partnerCode, targetApi);

        if (mappings.isEmpty()) {
            log.warn("No mapping rows found — partnerCode={} targetApi={}. " +
                     "Check partner_field_mapping table.", partnerCode, targetApi);
        }

        Map<String, Object> resolved     = new LinkedHashMap<>();
        List<String>        missingFields = new ArrayList<>();

        for (PartnerFieldMapping mapping : mappings) {

            // Priority 1: raw value from partner's inbound request
            String rawValue = rawParams.get(mapping.getSourceParam());
            boolean isBlank = (rawValue == null || rawValue.trim().isEmpty());

            // Priority 2: default_value from DB when partner sends blank
            // The default is type-converted the same way as a real value.
            if (isBlank && isNotBlank(mapping.getDefaultValue())) {
                rawValue = mapping.getDefaultValue();
                isBlank  = false;
                log.debug("Default value applied — field={} api={} default={}",
                        mapping.getTargetField(), targetApi, rawValue);
            }

            // Priority 3: mandatory check — collect ALL missing before throwing
            if (isBlank) {
                if (mapping.isMandatory()) {
                    missingFields.add(
                        mapping.getSourceParam() + " → " + mapping.getTargetField());
                }
                // Not mandatory and no default → skip field, POJO field stays null
                continue;
            }

            // Apply transformation (TRIM / UPPERCASE / LOWERCASE) then type conversion
            String transformed = applyTransformation(rawValue, mapping.getTransformation());
            Object typed = convertToType(transformed, mapping.getDataType(), mapping.getDateFormat());
            resolved.put(mapping.getTargetField(), typed);
        }

        // Throw ONCE with ALL missing fields listed — not one at a time.
        // Much faster to debug during partner integration testing.
        if (!missingFields.isEmpty()) {
            throw new MappingValidationException(targetApi, missingFields);
        }

        return resolved;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String applyTransformation(String value, String transformation) {
        if (transformation == null || transformation.trim().isEmpty()) return value;
        switch (transformation.toUpperCase()) {
            case "TRIM":      return value.trim();
            case "UPPERCASE": return value.trim().toUpperCase();
            case "LOWERCASE": return value.trim().toLowerCase();
            default:          return value;
        }
    }

    private Object convertToType(String value, String dataType, String dateFormat) {
        if (value == null) return null;
        switch (dataType.toUpperCase()) {
            case "STRING":  return value;
            case "INTEGER": return Integer.parseInt(value.trim());
            case "DECIMAL": return new BigDecimal(value.trim());
            case "BOOLEAN": return Boolean.parseBoolean(value.trim());
            case "DATE":
                // dateFormat must be set in DB row when data_type = DATE
                // e.g. dd/MM/yyyy or yyyy-MM-dd
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat);
                return LocalDate.parse(value.trim(), fmt);
            default:
                log.warn("Unknown dataType '{}' — treating as STRING", dataType);
                return value;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
