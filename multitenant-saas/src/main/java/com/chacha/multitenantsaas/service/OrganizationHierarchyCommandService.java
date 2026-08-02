package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OrganizationalUnitCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitMoveRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizationHierarchyCommandService {

    private final OrganizationHierarchyService
            organizationHierarchyService;

    private final CurrentActorService
            currentActorService;

    private final AuditLogService
            auditLogService;

    public OrganizationHierarchyCommandService(
            OrganizationHierarchyService
                    organizationHierarchyService,
            CurrentActorService currentActorService,
            AuditLogService auditLogService
    ) {
        this.organizationHierarchyService =
                organizationHierarchyService;

        this.currentActorService =
                currentActorService;

        this.auditLogService =
                auditLogService;
    }

    @Transactional
    public OrganizationalUnitResponse createUnit(
            UUID tenantId,
            OrganizationalUnitCreateRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                currentActorService
                        .getRequiredActiveActor(
                                tenantId,
                                jwt
                        );

        OrganizationalUnitResponse createdUnit =
                organizationHierarchyService
                        .createUnit(
                                tenantId,
                                request
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.ORG_UNIT_CREATED,
                "Organizational unit created: "
                        + createdUnit.id()
                        + " - "
                        + createdUnit.name()
                        + "; code="
                        + formatNullableValue(
                        createdUnit.code()
                )
                        + "; type="
                        + createdUnit.type()
                        + "; parentUnitId="
                        + formatNullableUuid(
                        createdUnit.parentUnitId()
                )
        );

        return createdUnit;
    }

    @Transactional
    public OrganizationalUnitResponse updateUnit(
            UUID tenantId,
            UUID organizationalUnitId,
            OrganizationalUnitUpdateRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                currentActorService
                        .getRequiredActiveActor(
                                tenantId,
                                jwt
                        );

        OrganizationalUnitResponse previousUnit =
                organizationHierarchyService
                        .getUnit(
                                tenantId,
                                organizationalUnitId
                        );

        OrganizationalUnitResponse updatedUnit =
                organizationHierarchyService
                        .updateUnit(
                                tenantId,
                                organizationalUnitId,
                                request
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.ORG_UNIT_UPDATED,
                "Organizational unit updated: "
                        + updatedUnit.id()
                        + "; name="
                        + previousUnit.name()
                        + " -> "
                        + updatedUnit.name()
                        + "; code="
                        + formatNullableValue(
                        previousUnit.code()
                )
                        + " -> "
                        + formatNullableValue(
                        updatedUnit.code()
                )
                        + "; type="
                        + previousUnit.type()
                        + " -> "
                        + updatedUnit.type()
        );

        return updatedUnit;
    }

    @Transactional
    public OrganizationalUnitResponse updateUnitStatus(
            UUID tenantId,
            UUID organizationalUnitId,
            OrganizationalUnitStatusUpdateRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                currentActorService
                        .getRequiredActiveActor(
                                tenantId,
                                jwt
                        );

        OrganizationalUnitResponse previousUnit =
                organizationHierarchyService
                        .getUnit(
                                tenantId,
                                organizationalUnitId
                        );

        OrganizationalUnitResponse updatedUnit =
                organizationHierarchyService
                        .updateUnitStatus(
                                tenantId,
                                organizationalUnitId,
                                request
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.ORG_UNIT_STATUS_UPDATED,
                "Organizational unit status updated: "
                        + updatedUnit.id()
                        + " - "
                        + updatedUnit.name()
                        + "; status="
                        + previousUnit.status()
                        + " -> "
                        + updatedUnit.status()
        );

        return updatedUnit;
    }

    @Transactional
    public OrganizationalUnitResponse moveUnit(
            UUID tenantId,
            UUID organizationalUnitId,
            OrganizationalUnitMoveRequest request,
            Jwt jwt
    ) {
        AppUser actor =
                currentActorService
                        .getRequiredActiveActor(
                                tenantId,
                                jwt
                        );

        OrganizationalUnitResponse previousUnit =
                organizationHierarchyService
                        .getUnit(
                                tenantId,
                                organizationalUnitId
                        );

        OrganizationalUnitResponse movedUnit =
                organizationHierarchyService
                        .moveUnit(
                                tenantId,
                                organizationalUnitId,
                                request
                        );

        auditLogService.recordSuccess(
                actor.getTenant(),
                actor,
                actor,
                AuditAction.ORG_UNIT_MOVED,
                "Organizational unit moved: "
                        + movedUnit.id()
                        + " - "
                        + movedUnit.name()
                        + "; parentUnitId="
                        + formatNullableUuid(
                        previousUnit.parentUnitId()
                )
                        + " -> "
                        + formatNullableUuid(
                        movedUnit.parentUnitId()
                )
        );

        return movedUnit;
    }

    private String formatNullableUuid(UUID value) {
        return value == null
                ? "ROOT"
                : value.toString();
    }

    private String formatNullableValue(String value) {
        return value == null
                ? "NONE"
                : value;
    }
}