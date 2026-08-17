package com.chacha.multitenantsaas.config;

import com.chacha.multitenantsaas.common.RequestCorrelationFilter;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins =
                corsProperties.getAllowedOrigins().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .distinct()
                        .toList();

        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "Credentialed CORS must use explicit allowed origins; wildcard '*' is forbidden");
        }

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-CSRF-Token",
                        RequestCorrelationFilter.REQUEST_ID_HEADER));

        configuration.setExposedHeaders(
                List.of("Location", RequestCorrelationFilter.REQUEST_ID_HEADER));

        /*
         * Refresh and trusted-browser credentials are HttpOnly cookies.
         * allowedOrigins must remain an explicit finite allow-list.
         */
        configuration.setAllowCredentials(true);

        /*
         * Browser may cache a successful preflight response for one hour.
         */
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        source.registerCorsConfiguration("/actuator/**", configuration);

        return source;
    }
}
