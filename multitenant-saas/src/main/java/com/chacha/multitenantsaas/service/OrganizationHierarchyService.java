package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OrganizationalUnitPathResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitTreeResponse;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationHierarchyService {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_CODE_LENGTH = 100;

    private static final Comparator<OrganizationalUnit>
            UNIT_ORDER =
            Comparator.comparing(
                            OrganizationalUnit::getName,
                            String.CASE_INSENSITIVE_ORDER
                    )
                    .thenComparing(
                            OrganizationalUnit::getId
                    );

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
        this.tenantLookupService =
                tenantLookupService;

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
                tenantLookupService.getByIdOrThrow(
                        tenantId
                );

        ensureTenantIsActive(tenant);

        String normalizedName =
                normalizeName(name);

        String normalizedCode =
                normalizeCode(code);

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

            validateClosurePaths(
                    tenantId,
                    parentPaths
            );

            ensurePathContainsSelf(
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
                        .saveAndFlush(
                                organizationalUnit
                        );

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
    public OrganizationalUnitResponse getUnit(
            UUID tenantId,
            UUID organizationalUnitId
    ) {
        tenantLookupService.ensureExists(tenantId);

        return mapToResponse(
                getUnitOrThrow(
                        tenantId,
                        organizationalUnitId
                )
        );
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnitResponse>
    getRootUnits(UUID tenantId) {
        tenantLookupService.ensureExists(tenantId);

        return organizationalUnitRepository
                .findAllByTenant_IdAndParentUnitIsNullOrderByNameAsc(
                        tenantId
                )
                .stream()
                .sorted(UNIT_ORDER)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnitResponse>
    getDirectChildren(
            UUID tenantId,
            UUID parentUnitId
    ) {
        tenantLookupService.ensureExists(tenantId);

        getUnitOrThrow(
                tenantId,
                parentUnitId
        );

        return organizationalUnitRepository
                .findAllByTenant_IdAndParentUnit_IdOrderByNameAsc(
                        tenantId,
                        parentUnitId
                )
                .stream()
                .sorted(UNIT_ORDER)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnitTreeResponse>
    getTree(UUID tenantId) {
        tenantLookupService.ensureExists(tenantId);

        List<OrganizationalUnit> units =
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(
                                tenantId
                        );

        if (units.isEmpty()) {
            return List.of();
        }

        validateTenantHierarchy(
                tenantId,
                units
        );

        Map<UUID, List<OrganizationalUnit>>
                childrenByParentId =
                buildChildrenMap(units);

        List<OrganizationalUnit> roots =
                units.stream()
                        .filter(
                                unit ->
                                        unit.getParentUnit()
                                                == null
                        )
                        .sorted(UNIT_ORDER)
                        .toList();

        if (roots.isEmpty()) {
            throw new IllegalStateException(
                    "Organizational hierarchy contains "
                            + "units but has no root unit."
            );
        }

        return roots.stream()
                .map(
                        root ->
                                buildTreeNode(
                                        root,
                                        childrenByParentId,
                                        new HashSet<>()
                                )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationalUnitTreeResponse getSubtree(
            UUID tenantId,
            UUID organizationalUnitId
    ) {
        tenantLookupService.ensureExists(tenantId);

        OrganizationalUnit rootUnit =
                getUnitOrThrow(
                        tenantId,
                        organizationalUnitId
                );

        List<OrganizationalUnit> units =
                organizationalUnitRepository
                        .findAllByTenant_IdOrderByNameAsc(
                                tenantId
                        );

        validateTenantHierarchy(
                tenantId,
                units
        );

        Map<UUID, List<OrganizationalUnit>>
                childrenByParentId =
                buildChildrenMap(units);

        return buildTreeNode(
                rootUnit,
                childrenByParentId,
                new HashSet<>()
        );
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnitPathResponse>
    getAncestors(
            UUID tenantId,
            UUID organizationalUnitId
    ) {
        tenantLookupService.ensureExists(tenantId);

        OrganizationalUnit organizationalUnit =
                getUnitOrThrow(
                        tenantId,
                        organizationalUnitId
                );

        List<OrganizationalUnitClosure> paths =
                organizationalUnitClosureRepository
                        .findAncestorPaths(
                                tenantId,
                                organizationalUnitId
                        );

        validateClosurePaths(
                tenantId,
                paths
        );

        ensurePathContainsSelf(
                organizationalUnit,
                paths
        );

        return paths.stream()
                .map(
                        path ->
                                mapToPathResponse(
                                        path.getAncestorUnit(),
                                        path.getDepth()
                                )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationalUnitPathResponse>
    getDescendants(
            UUID tenantId,
            UUID organizationalUnitId
    ) {
        tenantLookupService.ensureExists(tenantId);

        OrganizationalUnit organizationalUnit =
                getUnitOrThrow(
                        tenantId,
                        organizationalUnitId
                );

        List<OrganizationalUnitClosure> paths =
                organizationalUnitClosureRepository
                        .findDescendantPaths(
                                tenantId,
                                organizationalUnitId
                        );

        validateClosurePaths(
                tenantId,
                paths
        );

        ensurePathContainsSelf(
                organizationalUnit,
                paths
        );

        return paths.stream()
                .map(
                        path ->
                                mapToPathResponse(
                                        path.getDescendantUnit(),
                                        path.getDepth()
                                )
                )
                .toList();
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
                        () ->
                                new ResourceNotFoundException(
                                        "Organizational unit "
                                                + "not found with id: "
                                                + organizationalUnitId
                                )
                );
    }

    private Map<UUID, List<OrganizationalUnit>>
    buildChildrenMap(
            List<OrganizationalUnit> units
    ) {
        Map<UUID, List<OrganizationalUnit>>
                childrenByParentId =
                new HashMap<>();

        units.stream()
                .filter(
                        unit ->
                                unit.getParentUnit()
                                        != null
                )
                .sorted(UNIT_ORDER)
                .forEach(
                        unit ->
                                childrenByParentId
                                        .computeIfAbsent(
                                                unit.getParentUnit()
                                                        .getId(),
                                                ignored ->
                                                        new ArrayList<>()
                                        )
                                        .add(unit)
                );

        return childrenByParentId;
    }

    private OrganizationalUnitTreeResponse
    buildTreeNode(
            OrganizationalUnit unit,
            Map<UUID, List<OrganizationalUnit>>
                    childrenByParentId,
            Set<UUID> currentPath
    ) {
        if (!currentPath.add(unit.getId())) {
            throw new IllegalStateException(
                    "Cycle detected in organizational "
                            + "hierarchy at unit: "
                            + unit.getId()
            );
        }

        try {
            List<OrganizationalUnitTreeResponse> children =
                    childrenByParentId
                            .getOrDefault(
                                    unit.getId(),
                                    List.of()
                            )
                            .stream()
                            .sorted(UNIT_ORDER)
                            .map(
                                    child ->
                                            buildTreeNode(
                                                    child,
                                                    childrenByParentId,
                                                    currentPath
                                            )
                            )
                            .toList();

            return new OrganizationalUnitTreeResponse(
                    unit.getId(),
                    unit.getTenant().getId(),
                    getParentUnitId(unit),
                    unit.getName(),
                    unit.getCode(),
                    unit.getType(),
                    unit.getStatus(),
                    children
            );
        } finally {
            currentPath.remove(unit.getId());
        }
    }

    private void validateTenantHierarchy(
            UUID tenantId,
            List<OrganizationalUnit> units
    ) {
        Set<UUID> tenantUnitIds =
                units.stream()
                        .map(
                                OrganizationalUnit::getId
                        )
                        .collect(
                                java.util.stream.Collectors
                                        .toSet()
                        );

        for (OrganizationalUnit unit : units) {
            if (
                    !tenantId.equals(
                            unit.getTenant().getId()
                    )
            ) {
                throw new IllegalStateException(
                        "Organizational unit belongs to "
                                + "an unexpected tenant: "
                                + unit.getId()
                );
            }

            OrganizationalUnit parent =
                    unit.getParentUnit();

            if (parent == null) {
                continue;
            }

            if (
                    !tenantId.equals(
                            parent.getTenant().getId()
                    )
            ) {
                throw new IllegalStateException(
                        "Organizational unit has a "
                                + "cross-tenant parent: "
                                + unit.getId()
                );
            }

            if (
                    !tenantUnitIds.contains(
                            parent.getId()
                    )
            ) {
                throw new IllegalStateException(
                        "Organizational unit references "
                                + "a parent outside the "
                                + "tenant hierarchy: "
                                + unit.getId()
                );
            }

            if (
                    unit.getId().equals(
                            parent.getId()
                    )
            ) {
                throw new IllegalStateException(
                        "Organizational unit cannot be "
                                + "its own parent: "
                                + unit.getId()
                );
            }
        }
    }

    private void validateClosurePaths(
            UUID tenantId,
            List<OrganizationalUnitClosure> paths
    ) {
        for (
                OrganizationalUnitClosure path
                : paths
        ) {
            boolean validTenant =
                    tenantId.equals(
                            path.getTenant().getId()
                    )
                            && tenantId.equals(
                            path.getAncestorUnit()
                                    .getTenant()
                                    .getId()
                    )
                            && tenantId.equals(
                            path.getDescendantUnit()
                                    .getTenant()
                                    .getId()
                    );

            if (!validTenant) {
                throw new IllegalStateException(
                        "Organizational closure data "
                                + "contains a cross-tenant "
                                + "relationship."
                );
            }

            boolean selfPath =
                    path.getAncestorUnit()
                            .getId()
                            .equals(
                                    path.getDescendantUnit()
                                            .getId()
                            );

            if (
                    selfPath
                            && path.getDepth() != 0
            ) {
                throw new IllegalStateException(
                        "Organizational closure self-path "
                                + "must have depth zero."
                );
            }

            if (
                    !selfPath
                            && path.getDepth() <= 0
            ) {
                throw new IllegalStateException(
                        "Organizational closure descendant "
                                + "path must have positive depth."
                );
            }
        }
    }

    private void ensurePathContainsSelf(
            OrganizationalUnit unit,
            List<OrganizationalUnitClosure> paths
    ) {
        boolean selfPathExists =
                paths.stream()
                        .anyMatch(
                                path ->
                                        path.getDepth() == 0
                                                && path
                                                .getAncestorUnit()
                                                .getId()
                                                .equals(
                                                        unit.getId()
                                                )
                                                && path
                                                .getDescendantUnit()
                                                .getId()
                                                .equals(
                                                        unit.getId()
                                                )
                        );

        if (!selfPathExists) {
            throw new IllegalStateException(
                    "Organizational hierarchy data is "
                            + "inconsistent for unit: "
                            + unit.getId()
            );
        }
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

    private OrganizationalUnitResponse mapToResponse(
            OrganizationalUnit unit
    ) {
        return new OrganizationalUnitResponse(
                unit.getId(),
                unit.getTenant().getId(),
                getParentUnitId(unit),
                unit.getName(),
                unit.getCode(),
                unit.getType(),
                unit.getStatus(),
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }

    private OrganizationalUnitPathResponse
    mapToPathResponse(
            OrganizationalUnit unit,
            int depth
    ) {
        return new OrganizationalUnitPathResponse(
                unit.getId(),
                unit.getTenant().getId(),
                getParentUnitId(unit),
                unit.getName(),
                unit.getCode(),
                unit.getType(),
                unit.getStatus(),
                depth
        );
    }

    private UUID getParentUnitId(
            OrganizationalUnit unit
    ) {
        return unit.getParentUnit() == null
                ? null
                : unit.getParentUnit().getId();
    }

    private void ensureTenantIsActive(
            Tenant tenant
    ) {
        if (
                tenant.getStatus()
                        != TenantStatus.ACTIVE
        ) {
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
                    "An organizational unit already "
                            + "exists with code: "
                            + normalizedCode
            );
        }
    }

    private String normalizeName(String name) {
        if (
                name == null
                        || name.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Organizational unit name is required."
            );
        }

        String normalizedName = name.trim();

        if (
                normalizedName.length()
                        > MAX_NAME_LENGTH
        ) {
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
        if (
                code == null
                        || code.isBlank()
        ) {
            return null;
        }

        String normalizedCode =
                code.trim()
                        .toUpperCase(Locale.ROOT);

        if (
                normalizedCode.length()
                        > MAX_CODE_LENGTH
        ) {
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