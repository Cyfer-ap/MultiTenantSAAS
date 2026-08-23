package com.chacha.multitenantsaas.config;

import com.chacha.multitenantsaas.security.ApiKeyAuthenticationEntryPoint;
import com.chacha.multitenantsaas.security.JwtAccessDeniedHandler;
import com.chacha.multitenantsaas.security.JwtAuthenticationEntryPoint;
import com.chacha.multitenantsaas.security.TenantApiKeyAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final ApiKeyAuthenticationEntryPoint apiKeyAuthenticationEntryPoint;
    private final TenantApiKeyAuthenticationFilter tenantApiKeyAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler,
            ApiKeyAuthenticationEntryPoint apiKeyAuthenticationEntryPoint,
            TenantApiKeyAuthenticationFilter tenantApiKeyAuthenticationFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
        this.apiKeyAuthenticationEntryPoint = apiKeyAuthenticationEntryPoint;
        this.tenantApiKeyAuthenticationFilter = tenantApiKeyAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .headers(
                        headers ->
                                headers.frameOptions(frameOptions -> frameOptions.deny())
                                        .addHeaderWriter(
                                                new StaticHeadersWriter(
                                                        "Referrer-Policy", "no-referrer"))
                                        .addHeaderWriter(
                                                new StaticHeadersWriter(
                                                        "Permissions-Policy",
                                                        "camera=(), microphone=(), geolocation=()")))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                        .defaultAuthenticationEntryPointFor(
                                                apiKeyAuthenticationEntryPoint,
                                                new AntPathRequestMatcher("/api/external/**"))
                                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/health")
                                        .permitAll()
                                        .requestMatchers("/actuator/health/**", "/livez", "/readyz")
                                        .permitAll()
                                        .requestMatchers("/actuator/**")
                                        .hasAuthority("SYSTEM_ADMIN")
                                        .requestMatchers("/h2-console/**")
                                        .denyAll()
                                        .requestMatchers(
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**")
                                        .hasAuthority("SYSTEM_ADMIN")
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/tenants/{tenantId}/auth/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/auth/logout")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/workspaces/start",
                                                "/api/auth/workspaces/verify")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/auth/password-reset/request",
                                                "/api/auth/password-reset/complete")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/onboarding/tenants")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/system/auth/login")
                                        .permitAll()
                                        .requestMatchers("/api/dashboard/**")
                                        .hasAuthority("SYSTEM_ADMIN")
                                        .requestMatchers("/api/external/**")
                                        .hasAuthority(
                                                TenantApiKeyAuthenticationFilter.API_KEY_AUTHORITY)
                                        .requestMatchers(
                                                HttpMethod.POST, "/api/user-invitations/accept")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/billing/webhooks/stripe",
                                                "/api/billing/webhooks/razorpay")
                                        .permitAll()
                                        .requestMatchers("/api/**")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                        .accessDeniedHandler(jwtAccessDeniedHandler)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())))
                .addFilterBefore(
                        tenantApiKeyAuthenticationFilter,
                        BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString("role");

            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
