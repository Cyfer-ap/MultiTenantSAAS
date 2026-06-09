package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantCreateRequest;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chacha.multitenantsaas.dto.TenantUpdateRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody TenantCreateRequest request
    ) {
        TenantResponse tenant = tenantService.createTenant(request);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant created successfully", tenant)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants() {
        List<TenantResponse> tenants = tenantService.getAllTenants();

        return ResponseEntity.ok(
                ApiResponse.success("Tenants fetched successfully", tenants)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantById(
            @PathVariable UUID id
    ) {
        TenantResponse tenant = tenantService.getTenantById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant fetched successfully", tenant)
        );
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantBySlug(
            @PathVariable String slug
    ) {
        TenantResponse tenant = tenantService.getTenantBySlug(slug);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant fetched successfully", tenant)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody TenantUpdateRequest request
    ) {
        TenantResponse tenant = tenantService.updateTenant(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Tenant updated successfully", tenant)
        );
    }
}