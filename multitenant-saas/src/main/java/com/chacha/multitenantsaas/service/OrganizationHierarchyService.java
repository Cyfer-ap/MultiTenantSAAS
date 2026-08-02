package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.OrganizationalUnit;
import com.chacha.multitenantsaas.entity.OrganizationalUnitClosure;
import com.chacha.multitenantsaas.entity.OrganizationalUnitStatus;
import com.chacha.multitenantsaas.entity.OrganizationalUnitType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.OrganizationalUnitClosureRepository;
import com.chacha.multitenantsaas.repository.OrganizationalUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrganizationHierarchyService {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_CODE_LENGTH = 100;

    private final TenantLookupService tenantLookupService;

    private final OrganizationalUnitRepository
            organizationalUnitRepository;

    private final OrganizationalUnitClosureRepository
            organizationalUnitClosureRepository;

    public OrganizationHierarchyService(
            TenantLookupService tenantLookupService,
            OrganizationalUnitRepository
                    organizationalUnitRepository,
            OrganizationalUnitClosureRepository
                    organizationalUnitClosureRepository
    ) {
        this.tenantLookupService = tenantLookupService;
        this.organizationalUnitRepository =
                organizationalUnitRepository;
        this.organizationalUnitClosureRepository =
                organizationalUnitClosureRepository;
    }

    @Transactional
    public OrganizationalUnit createUnit(
            UUID tenantId,
            UUID parentUnitId,
            String name,
            String code,
            OrganizationalUnitType type
    ) {
        Tenant tenant =
                tenantLookupService.getByIdOrThrow(tenantId);

        ensureTenantIsActive(tenant);

        String normalizedName = normalizeName(name);
        String normalizedCode = normalizeCode(code);

        validateType(type);
        ensureCodeIsAvailable(
                tenantId,
                normalizedCode
        );

        OrganizationalUnit parentUnit = null;

        List<OrganizationalUnitClosure> parentPaths =
                List.of();

        if (parentUnitId != null) {
            parentUnit = getUnitOrThrow(
                    tenantId,
                    parentUnitId
            );

            ensureParentIsActive(parentUnit);

            parentPaths =
                    organizationalUnitClosureRepository
                            .findAncestorPaths(
                                    tenantId,
                                    parentUnitId
                            );

            ensureParentHierarchyIsConsistent(
                    parentUnit,
                    parentPaths
            );
        }

        OrganizationalUnit organizationalUnit =
                new OrganizationalUnit(
                        tenant,
                        parentUnit,
                        normalizedName,
                        normalizedCode,
                        type
                );

        OrganizationalUnit savedUnit =
                organizationalUnitRepository
                        .saveAndFlush(organizationalUnit);

        List<OrganizationalUnitClosure> closureRows =
                buildClosureRows(
                        tenant,
                        savedUnit,
                        parentPaths
                );

        organizationalUnitClosureRepository
                .saveAllAndFlush(closureRows);

        return savedUnit;
    }

    @Transactional(readOnly = true)
    public OrganizationalUnit getUnitOrThrow(
            UUID tenantId,
            UUID organizationalUnitId
    ) {
        return organizationalUnitRepository
                .findByTenant_IdAndId(
                        tenantId,
                        organizationalUnitId
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Organizational unit not found "
                                        + "with id: "
                                        + organizationalUnitId
                        )
                );
    }

    private List<OrganizationalUnitClosure>
    buildClosureRows(
            Tenant tenant,
            OrganizationalUnit savedUnit,
            List<OrganizationalUnitClosure> parentPaths
    ) {
        List<OrganizationalUnitClosure> closureRows =
                new ArrayList<>();

        closureRows.add(
                new OrganizationalUnitClosure(
                        tenant,
                        savedUnit,
                        savedUnit,
                        0
                )
        );

        for (
                OrganizationalUnitClosure parentPath
                : parentPaths
        ) {
            closureRows.add(
                    new OrganizationalUnitClosure(
                            tenant,
                            parentPath.getAncestorUnit(),
                            savedUnit,
                            parentPath.getDepth() + 1
                    )
            );
        }

        return closureRows;
    }

    private void ensureParentHierarchyIsConsistent(
            OrganizationalUnit parentUnit,
            List<OrganizationalUnitClosure> parentPaths
    ) {
        boolean selfPathExists = parentPaths.stream()
                .anyMatch(
                        path ->
                                path.getDepth() == 0
                                        && path
                                        .getAncestorUnit()
                                        .getId()
                                        .equals(
                                                parentUnit.getId()
                                        )
                );

        if (!selfPathExists) {
            throw new IllegalStateException(
                    "Organizational hierarchy data is "
                            + "inconsistent for parent unit: "
                            + parentUnit.getId()
            );
        }
    }

    private void ensureTenantIsActive(Tenant tenant) {
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Organizational units can only be "
                            + "created for an active tenant."
            );
        }
    }

    private void ensureParentIsActive(
            OrganizationalUnit parentUnit
    ) {
        if (
                parentUnit.getStatus()
                        != OrganizationalUnitStatus.ACTIVE
        ) {
            throw new IllegalArgumentException(
                    "A child organizational unit cannot "
                            + "be created under an inactive "
                            + "parent unit."
            );
        }
    }

    private void ensureCodeIsAvailable(
            UUID tenantId,
            String normalizedCode
    ) {
        if (normalizedCode == null) {
            return;
        }

        boolean codeAlreadyExists =
                organizationalUnitRepository
                        .existsByTenant_IdAndCodeIgnoreCase(
                                tenantId,
                                normalizedCode
                        );

        if (codeAlreadyExists) {
            throw new DuplicateResourceException(
                    "An organizational unit already exists "
                            + "with code: "
                            + normalizedCode
            );
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Organizational unit name is required."
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Organizational unit name must not "
                            + "exceed "
                            + MAX_NAME_LENGTH
                            + " characters."
            );
        }

        return normalizedName;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String normalizedCode = code
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalizedCode.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "Organizational unit code must not "
                            + "exceed "
                            + MAX_CODE_LENGTH
                            + " characters."
            );
        }

        if (
                !normalizedCode.matches(
                        "[A-Z0-9][A-Z0-9_-]*"
                )
        ) {
            throw new IllegalArgumentException(
                    "Organizational unit code may contain "
                            + "only letters, numbers, "
                            + "hyphens, and underscores."
            );
        }

        return normalizedCode;
    }

    private void validateType(
            OrganizationalUnitType type
    ) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Organizational unit type is required."
            );
        }
    }
}