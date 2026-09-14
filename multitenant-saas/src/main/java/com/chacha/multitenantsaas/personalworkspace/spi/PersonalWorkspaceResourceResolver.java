package com.chacha.multitenantsaas.personalworkspace.spi;

import com.chacha.multitenantsaas.personalworkspace.model.PersonalResourceType;
import com.chacha.multitenantsaas.personalworkspace.model.PersonalWorkspaceResource;
import java.util.Optional;
import java.util.UUID;

public interface PersonalWorkspaceResourceResolver {

    PersonalResourceType resourceType();

    Optional<PersonalWorkspaceResource> resolve(UUID tenantId, UUID userId, UUID resourceId);
}
