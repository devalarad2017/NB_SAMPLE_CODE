package com.balic.newbusiness.domain.enums;

/**
 * List of all internal API names.
 *
 * Each name here must have a matching entry in application.properties under api.endpoints
 * Matching rows in the partner_field_mapping table if DB based mapping will be used in future.
 *
 * If you add a new API, add it here first.
 */
public enum ApiName {

    // --- Sequential journey APIs --- update actual sequence here 
    CIBIL_API,
    UCS_API,
	EDC_API,
  
    // --- PAS and reverse feed ---
    PAS_API,
    REVERSE_FEED_API
}
