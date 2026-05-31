package com.balic.newbusiness.reversefeed;

import com.balic.newbusiness.journey.JourneyContext;

/**
 * Each partner gets one implementation of this interface.
 * Spring auto-discovers all @Component implementations.
 * PartnerNotifierFactory routes to the correct one by partnerCode.
 */
public interface PartnerNotifier {

    String getSupportedPartnerCode();

    void notify(JourneyContext context, String applicationNumber);
}
