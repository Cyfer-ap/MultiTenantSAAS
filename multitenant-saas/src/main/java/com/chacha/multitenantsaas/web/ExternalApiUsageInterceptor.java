package com.chacha.multitenantsaas.web;

import com.chacha.multitenantsaas.billing.service.ApiRequestQuotaService;
import com.chacha.multitenantsaas.security.TenantApiKeyPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ExternalApiUsageInterceptor implements HandlerInterceptor {

    private final ApiRequestQuotaService apiRequestQuotaService;

    public ExternalApiUsageInterceptor(ApiRequestQuotaService apiRequestQuotaService) {
        this.apiRequestQuotaService = apiRequestQuotaService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof TenantApiKeyPrincipal principal)) {
            return true;
        }

        apiRequestQuotaService.consume(principal.tenantId(), principal.apiKeyId());
        return true;
    }
}
