package com.chacha.multitenantsaas.controller;

import com.chacha.multitenantsaas.common.ApiResponse;
import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.SubscriptionPlanStatusUpdateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanUpdateRequest;
import com.chacha.multitenantsaas.service.SubscriptionAdministrationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/subscription-plans")
public class SystemSubscriptionPlanController {

    private final SubscriptionAdministrationService
            subscriptionAdministrationService;

    public SystemSubscriptionPlanController(
            SubscriptionAdministrationService
                    subscriptionAdministrationService
    ) {
        this.subscriptionAdministrationService =
                subscriptionAdministrationService;
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping
    public ResponseEntity<
            ApiResponse<List<SubscriptionPlanResponse>>
            >
    getPlans(
            @RequestParam(defaultValue = "false")
            boolean activeOnly
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plans fetched successfully",
                        subscriptionAdministrationService
                                .getPlans(activeOnly)
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    getPlan(
            @PathVariable UUID planId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan fetched successfully",
                        subscriptionAdministrationService
                                .getPlan(planId)
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    createPlan(
            @Valid @RequestBody
            SubscriptionPlanCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan created successfully",
                        subscriptionAdministrationService
                                .createPlan(request, jwt)
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody
            SubscriptionPlanUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan updated successfully",
                        subscriptionAdministrationService
                                .updatePlan(
                                        planId,
                                        request,
                                        jwt
                                )
                )
        );
    }

    @PreAuthorize("@systemSecurity.isSystemAdmin()")
    @PatchMapping("/{planId}/status")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>>
    changePlanStatus(
            @PathVariable UUID planId,
            @Valid @RequestBody
            SubscriptionPlanStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription plan status updated successfully",
                        subscriptionAdministrationService
                                .changePlanStatus(
                                        planId,
                                        request.status(),
                                        jwt
                                )
                )
        );
    }
}
