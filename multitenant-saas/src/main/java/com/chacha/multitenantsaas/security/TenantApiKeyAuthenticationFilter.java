package com.chacha.multitenantsaas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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

    private final TenantApiKeyAuthenticationService authenticationService;
    private final ApiKeyAuthenticationEntryPoint authenticationEntryPoint;

    public TenantApiKeyAuthenticationFilter(
            TenantApiKeyAuthenticationService authenticationService,
            ApiKeyAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationService = authenticationService;
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
            authenticationEntryPoint.commence(
                    request, response, new BadCredentialsException("Missing API key"));
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
                        principal, null, List.of(new SimpleGrantedAuthority(API_KEY_AUTHORITY)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}
