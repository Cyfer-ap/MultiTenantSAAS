package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.TenantCreateRequest;
import com.chacha.multitenantsaas.dto.TenantResponse;
import com.chacha.multitenantsaas.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

