package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.SubscriptionPlanCreateRequest;
import com.chacha.multitenantsaas.dto.SubscriptionPlanResponse;
import com.chacha.multitenantsaas.dto.SubscriptionPlanUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionLifecycleUpdateRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionPlanChangeRequest;
import com.chacha.multitenantsaas.dto.TenantSubscriptionResponse;
import com.chacha.multitenantsaas.dto.TenantSubscriptionStartRequest;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.PlatformAuditAction;
import com.chacha.multitenantsaas.entity.SubscriptionPlanStatus;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionAdministrationService {

    private final SubscriptionPlanService subscriptionPlanService;

    private final TenantSubscriptionService
            tenantSubscriptionService;

    private final CurrentSystemAdminService
            currentSystemAdminService;

    private final TenantLookupService tenantLookupService;

    private final PlatformAuditLogService
            platformAuditLogService;

    private final AuditLogService auditLogService;

    public SubscriptionAdministrationService(
            SubscriptionPlanService subscriptionPlanService,
            TenantSubscriptionService tenantSubscriptionService,
            CurrentSystemAdminService currentSystemAdminService,
            TenantLookupService tenantLookupService,
            PlatformAuditLogService platformAuditLogService,
            AuditLogService auditLogService
    ) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.tenantSubscriptionService =
                tenantSubscriptionService;
        this.currentSystemAdminService =
                currentSystemAdminService;
        this.tenantLookupService = tenantLookupService;
        this.platformAuditLogService =
                platformAuditLogService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPlans(
            boolean activeOnly
    ) {
        return subscriptionPlanService.getPlans(activeOnly);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlan(UUID planId) {
        return subscriptionPlanService.getPlan(planId);
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(
            SubscriptionPlanCreateRequest request,
            Jwt jwt
    ) {
        SystemAdmin actor =
                currentSystemAdminService
                        .getRequiredActiveSystemAdmin(jwt);

        SubscriptionPlanResponse created =
                subscriptionPlanService.createPlan(request);

        platformAuditLogService.recordSuccess(
                actor,
                null,
                PlatformAuditAction.SUBSCRIPTION_PLAN_CREATED,
                "Subscription plan created: "
                        + created.code()
        );

        return created;
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(
            UUID planId,
            SubscriptionPlanUpdateRequest request,
            Jwt jwt
    ) {
        SystemAdmin actor =
                currentSystemAdminService
                        .getRequiredActiveSystemAdmin(jwt);

        SubscriptionPlanResponse updated =
                subscriptionPlanService.updatePlan(
                        planId,
                        request
                );

        platformAuditLogService.recordSuccess(
                actor,
                null,
                PlatformAuditAction.SUBSCRIPTION_PLAN_UPDATED,
                "Subscription plan updated: "
                        + updated.code()
        );

        return updated;
    }

    @Transactional
    public SubscriptionPlanResponse changePlanStatus(
            UUID planId,
            SubscriptionPlanStatus status,
            Jwt jwt
    ) {
        SystemAdmin actor =
                currentSystemAdminService
                        .getRequiredActiveSystemAdmin(jwt);

        SubscriptionPlanResponse updated =
                subscriptionPlanService.changeStatus(
                        planId,
                        status
                );

        platformAuditLogService.recordSuccess(
                actor,
                null,
                PlatformAuditAction
                        .SUBSCRIPTION_PLAN_STATUS_UPDATED,
                "Subscription plan status updated: "
                        + updated.code()
                        + " -> "
                        + updated.status()
        );

        return updated;
    }

    @Transactional(readOnly = true)
    public TenantSubscriptionResponse getTenantSubscription(
            UUID tenantId
    ) {
        return tenantSubscriptionService
                .getSubscription(tenantId);
    }

    @Transactional
    public TenantSubscriptionResponse startTenantSubscription(
            UUID tenantId,
            TenantSubscriptionStartRequest request,
            Jwt jwt
    ) {
        SystemAdmin actor =
                currentSystemAdminService
                        .getRequiredActiveSystemAdmin(jwt);

        Tenant tenant =
                tenantLookupService.getActiveByIdOrThrow(
                        tenantId
                );

        TenantSubscriptionResponse created =
                tenantSubscriptionService
                        .startSubscription(
                                tenantId,
                                request
                        );

        platformAuditLogService.recordSuccess(
                actor,
                null,
                PlatformAuditAction
                        .TENANT_SUBSCRIPTION_STARTED,
                "Tenant subscription started for "
                        + tenant.getSlug()
                        + " on plan "
                        + created.plan().code()
        );

        auditLogService.recordSystemAdminSuccess(
                tenant,
                actor,
                null,
                AuditAction.TENANT_SUBSCRIPTION_STARTED,
                "Subscription started on plan "
                        + created.plan().code()
        );

        return created;
    }

    @Transactional
    public TenantSubscriptionResponse changeTenantPlan(
            UUID tenantId,
            TenantSubscriptionPlanChangeRequest request,
            Jwt jwt
    ) {
        SystemAdmin actor =
                currentSystemAdminService
                        .getRequiredActiveSystemAdmin(jwt);

        Tenant tenant =
                tenantLookupService.getByIdOrThrow(
                        tenantId
                );

        TenantSubscriptionResponse before =
                tenantSubscriptionService
                        .getSubscription(tenantId);

        TenantSubscriptionResponse updated =
                tenantSubscriptionService.changePlan(
                        tenantId,
                        request
                );

        String message =
                "Tenant subscription plan changed from "
                        + before.plan().code()
                        + " to "
                        + updated.plan().code();

        platformAuditLogService.recordSuccess(
                actor,
                null,
                PlatformAuditAction
                        .TENANT_SUBSCRIPTION_PLAN_CHANGED,
                tenant.getSlug() + ": " + message
        );

        auditLogService.recordSystemAdminSuccess(
                tenant,
                actor,
                null,
                AuditAction
                        .TENANT_SUBSCRIPTION_PLAN_CHANGED,
                message
        );

        return updated;
    }

    @Transactional
    public TenantSubscriptionResponse
    updateTenantSubscriptionLifecycle(
            UUID tenantId,
            TenantSubscriptionLifecycleUpdateRequest request,
            Jwt jwt
    ) {
        SystemAdmin actor =
                currentSystemAdminService
                        .getRequiredActiveSystemAdmin(jwt);

        Tenant tenant =
                tenantLookupService.getByIdOrThrow(
                        tenantId
                );

        TenantSubscriptionResponse before =
                tenantSubscriptionService
                        .getSubscription(tenantId);

        TenantSubscriptionResponse updated =
                tenantSubscriptionService
                        .updateLifecycle(
                                tenantId,
                                request
                        );

        String message =
                "Tenant subscription lifecycle updated from "
                        + before.status()
                        + " to "
                        + updated.status()
                        + "; cancelAtPeriodEnd="
                        + updated.cancelAtPeriodEnd();

        platformAuditLogService.recordSuccess(
                actor,
                null,
                PlatformAuditAction
                        .TENANT_SUBSCRIPTION_LIFECYCLE_UPDATED,
                tenant.getSlug() + ": " + message
        );

        auditLogService.recordSystemAdminSuccess(
                tenant,
                actor,
                null,
                AuditAction
                        .TENANT_SUBSCRIPTION_LIFECYCLE_UPDATED,
                message
        );

        return updated;
    }
}
