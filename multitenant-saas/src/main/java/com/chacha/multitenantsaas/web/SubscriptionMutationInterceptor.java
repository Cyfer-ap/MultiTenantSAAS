package com.chacha.multitenantsaas.web;

import com.chacha.multitenantsaas.security.AuthorizationSecurityService;
import com.chacha.multitenantsaas.security.SystemSecurityService;
import com.chacha.multitenantsaas.service.SubscriptionLifecycleGuardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SubscriptionMutationInterceptor
        implements HandlerInterceptor {

    private static final Set<String> READ_METHODS =
            Set.of(
                    "GET",
                    "HEAD",
                    "OPTIONS"
            );

    private final SubscriptionLifecycleGuardService
            subscriptionLifecycleGuardService;
    private final AuthorizationSecurityService
            authorizationSecurityService;
    private final SystemSecurityService
            systemSecurityService;

    public SubscriptionMutationInterceptor(
            SubscriptionLifecycleGuardService
                    subscriptionLifecycleGuardService,
            AuthorizationSecurityService
                    authorizationSecurityService,
            SystemSecurityService systemSecurityService
    ) {
        this.subscriptionLifecycleGuardService =
                subscriptionLifecycleGuardService;
        this.authorizationSecurityService =
                authorizationSecurityService;
        this.systemSecurityService =
                systemSecurityService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (READ_METHODS.contains(request.getMethod())) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        UUID tenantId = resolveTenantId(request);

        if (tenantId == null) {
            return true;
        }

        if (systemSecurityService.isSystemAdmin()) {
            return true;
        }

        /*
         * Do not disclose subscription state before tenant
         * authorization has established that the caller belongs
         * to the route tenant. Method security remains responsible
         * for the eventual 401/403 response.
         */
        if (!authorizationSecurityService
                .isCurrentTenant(tenantId)) {
            return true;
        }

        if (isReadOnlyAllowed(handlerMethod)) {
            return true;
        }

        subscriptionLifecycleGuardService
                .requireBusinessMutationAllowed(tenantId);

        return true;
    }

    private boolean isReadOnlyAllowed(
            HandlerMethod handlerMethod
    ) {
        return handlerMethod.getMethodAnnotation(
                SubscriptionReadOnlyAllowed.class
        ) != null
                || handlerMethod.getBeanType()
                .isAnnotationPresent(
                        SubscriptionReadOnlyAllowed.class
                );
    }

    private UUID resolveTenantId(
            HttpServletRequest request
    ) {
        Object attribute = request.getAttribute(
                HandlerMapping
                        .URI_TEMPLATE_VARIABLES_ATTRIBUTE
        );

        if (!(attribute instanceof Map<?, ?> variables)) {
            return null;
        }

        Object value = variables.get("tenantId");

        if (value == null) {
            return null;
        }

        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
