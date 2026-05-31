package com.balic.newbusiness.reversefeed;

import org.springframework.stereotype.Service;

import com.balic.newbusiness.exception.PartnerConfigException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes to the correct PartnerNotifier based on partnerCode.
 *
 * Spring auto-collects all PartnerNotifier beans into the constructor List.
 * Adding a new partner = create a new @Component implementing PartnerNotifier.
 * Nothing else changes here.
 */
@Service
public class PartnerNotifierFactory {

    private final Map<String, PartnerNotifier> notifiers = new HashMap<>();

    public PartnerNotifierFactory(List<PartnerNotifier> notifierList) {
        for (PartnerNotifier notifier : notifierList) {
            notifiers.put(notifier.getSupportedPartnerCode(), notifier);
        }
    }

    public PartnerNotifier getNotifier(String partnerCode) {
        PartnerNotifier notifier = notifiers.get(partnerCode);
        if (notifier == null) {
            throw new PartnerConfigException(
                    "No PartnerNotifier for partnerCode: " + partnerCode +
                    ". Create a @Component implementing PartnerNotifier.");
        }
        return notifier;
    }
}
