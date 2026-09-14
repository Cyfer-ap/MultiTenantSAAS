package com.chacha.multitenantsaas.savedviews.spi;

import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import java.util.UUID;

public interface SavedViewContextValidator {

    SavedViewTarget target();

    boolean canUse(UUID tenantId, UUID userId, UUID contextId);
}
