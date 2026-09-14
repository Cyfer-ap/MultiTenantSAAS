package com.chacha.multitenantsaas.projects.savedviews;

import com.chacha.multitenantsaas.savedviews.model.SavedViewTarget;
import com.chacha.multitenantsaas.savedviews.spi.SavedViewContextValidator;
import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.PlatformPermissionCodes;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProjectTasksSavedViewContextValidator implements SavedViewContextValidator {

    private final AuthorizationSecurityService authorizationSecurity;

    public ProjectTasksSavedViewContextValidator(AuthorizationSecurityService authorizationSecurity) {
        this.authorizationSecurity = authorizationSecurity;
    }

    @Override
    public SavedViewTarget target() {
        return SavedViewTarget.PROJECT_TASKS;
    }

    @Override
    public boolean canUse(UUID tenantId, UUID userId, UUID contextId) {
        return contextId != null
                && authorizationSecurity.canReadProjectTasks(
                        tenantId, contextId, PlatformPermissionCodes.PROJECT_TASK_READ);
    }
}
