package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.billing.dto.BillingUsageRecordRequest;
import com.chacha.multitenantsaas.billing.service.BillingUsageMeteringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String API_KEY_AUTHORITY = "TENANT_API";
    public static final String API_REQUESTS_METRIC = "API_REQUESTS";

    private static final Logger log =
            LoggerFactory.getLogger(TenantApiKeyAuthenticationFilter.class);

    private final TenantApiKeyAuthenticationService authenticationService;
    private final BillingUsageMeteringService usageMeteringService;
    private final ApiKeyAuthenticationEntryPoint authenticationEntryPoint;

    public TenantApiKeyAuthenticationFilter(
            TenantApiKeyAuthenticationService authenticationService,
            BillingUsageMeteringService usageMeteringService,
            ApiKeyAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationService = authenticationService;
        this.usageMeteringService = usageMeteringService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/external/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawApiKey = request.getHeader(API_KEY_HEADER);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        TenantApiKeyPrincipal principal =
                authenticationService.authenticate(rawApiKey).orElse(null);
        if (principal == null) {
            authenticationEntryPoint.commence(
                    request, response, new BadCredentialsException("Invalid API key"));
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(API_KEY_AUTHORITY)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        recordUsage(principal);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void recordUsage(TenantApiKeyPrincipal principal) {
        Instant occurredAt = Instant.now();
        String idempotencyKey =
                "api:" + principal.apiKeyId() + ":" + UUID.randomUUID();

        try {
            usageMeteringService.recordUsage(
                    new BillingUsageRecordRequest(
                            principal.tenantId(),
                            API_REQUESTS_METRIC,
                            1L,
                            idempotencyKey,
                            occurredAt));
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to record API request usage for tenant {} and API key {}",
                    principal.tenantId(),
                    principal.apiKeyId(),
                    exception);
        }
    }
}
