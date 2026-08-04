package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitCreateRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitMoveRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitPathResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.OrganizationalUnitTreeResponse;
import com.chacha.multitenantsaas.dto.OrganizationalUnitUpdateRequest;
import com.chacha.multitenantsaas.service.OrganizationHierarchyCommandService;
import com.chacha.multitenantsaas.service.OrganizationHierarchyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/tenants/{tenantId}/organization/units"
)
public class OrganizationHierarchyController {

    private final OrganizationHierarchyService
            organizationHierarchyService;

    private final OrganizationHierarchyCommandService
            organizationHierarchyCommandService;

    public OrganizationHierarchyController(
            OrganizationHierarchyService
                    organizationHierarchyService,
            OrganizationHierarchyCommandService
                    organizationHierarchyCommandService
    ) {
        this.organizationHierarchyService =
                organizationHierarchyService;

        this.organizationHierarchyCommandService =
                organizationHierarchyCommandService;
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasCreateOrganizationalUnitPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#request.parentUnitId(),"
                    + "'organization.unit.manage'"
                    + ")"
    )
    @PostMapping
    public ResponseEntity<
            ApiResponse<OrganizationalUnitResponse>
            >
    createUnit(
            @PathVariable
            UUID tenantId,

            @Valid
            @RequestBody
            OrganizationalUnitCreateRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        OrganizationalUnitResponse response =
                organizationHierarchyCommandService
                        .createUnit(
                                tenantId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit created "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/roots")
    public ResponseEntity<
            ApiResponse<
                    List<OrganizationalUnitResponse>
                    >
            >
    getRootUnits(
            @PathVariable
            UUID tenantId
    ) {
        List<OrganizationalUnitResponse> response =
                organizationHierarchyService
                        .getRootUnits(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Root organizational units "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasTenantPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/tree")
    public ResponseEntity<
            ApiResponse<
                    List<OrganizationalUnitTreeResponse>
                    >
            >
    getTree(
            @PathVariable
            UUID tenantId
    ) {
        List<OrganizationalUnitTreeResponse> response =
                organizationHierarchyService
                        .getTree(tenantId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational hierarchy "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalUnitPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/{unitId}")
    public ResponseEntity<
            ApiResponse<OrganizationalUnitResponse>
            >
    getUnit(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId
    ) {
        OrganizationalUnitResponse response =
                organizationHierarchyService
                        .getUnit(
                                tenantId,
                                unitId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalSubtreePermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/{unitId}/children")
    public ResponseEntity<
            ApiResponse<
                    List<OrganizationalUnitResponse>
                    >
            >
    getDirectChildren(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId
    ) {
        List<OrganizationalUnitResponse> response =
                organizationHierarchyService
                        .getDirectChildren(
                                tenantId,
                                unitId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Direct child units fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalSubtreePermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/{unitId}/subtree")
    public ResponseEntity<
            ApiResponse<OrganizationalUnitTreeResponse>
            >
    getSubtree(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId
    ) {
        OrganizationalUnitTreeResponse response =
                organizationHierarchyService
                        .getSubtree(
                                tenantId,
                                unitId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational subtree fetched "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalUnitPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/{unitId}/ancestors")
    public ResponseEntity<
            ApiResponse<
                    List<OrganizationalUnitPathResponse>
                    >
            >
    getAncestors(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId
    ) {
        List<OrganizationalUnitPathResponse> response =
                organizationHierarchyService
                        .getAncestors(
                                tenantId,
                                unitId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit ancestors "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalSubtreePermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.read'"
                    + ")"
    )
    @GetMapping("/{unitId}/descendants")
    public ResponseEntity<
            ApiResponse<
                    List<OrganizationalUnitPathResponse>
                    >
            >
    getDescendants(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId
    ) {
        List<OrganizationalUnitPathResponse> response =
                organizationHierarchyService
                        .getDescendants(
                                tenantId,
                                unitId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit descendants "
                                + "fetched successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalUnitPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.manage'"
                    + ")"
    )
    @PutMapping("/{unitId}")
    public ResponseEntity<
            ApiResponse<OrganizationalUnitResponse>
            >
    updateUnit(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId,

            @Valid
            @RequestBody
            OrganizationalUnitUpdateRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        OrganizationalUnitResponse response =
                organizationHierarchyCommandService
                        .updateUnit(
                                tenantId,
                                unitId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit updated "
                                + "successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasOrganizationalUnitPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "'organization.unit.manage'"
                    + ")"
    )
    @PatchMapping("/{unitId}/status")
    public ResponseEntity<
            ApiResponse<OrganizationalUnitResponse>
            >
    updateUnitStatus(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId,

            @Valid
            @RequestBody
            OrganizationalUnitStatusUpdateRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        OrganizationalUnitResponse response =
                organizationHierarchyCommandService
                        .updateUnitStatus(
                                tenantId,
                                unitId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit status "
                                + "updated successfully",
                        response
                )
        );
    }

    @PreAuthorize(
            "@authorizationSecurity"
                    + ".hasMoveOrganizationalUnitPermissionOrLegacyAdmin("
                    + "#tenantId,"
                    + "#unitId,"
                    + "#request.parentUnitId(),"
                    + "'organization.unit.manage'"
                    + ")"
    )
    @PatchMapping("/{unitId}/move")
    public ResponseEntity<
            ApiResponse<OrganizationalUnitResponse>
            >
    moveUnit(
            @PathVariable
            UUID tenantId,

            @PathVariable
            UUID unitId,

            @RequestBody
            OrganizationalUnitMoveRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        OrganizationalUnitResponse response =
                organizationHierarchyCommandService
                        .moveUnit(
                                tenantId,
                                unitId,
                                request,
                                jwt
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Organizational unit moved "
                                + "successfully",
                        response
                )
        );
    }
}