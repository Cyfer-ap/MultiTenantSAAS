package com.chacha.multitenantsaas.config;

import com.chacha.multitenantsaas.common.RequestCorrelationFilter;
import java.util.List;
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

        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        RequestCorrelationFilter.REQUEST_ID_HEADER));

        configuration.setExposedHeaders(
                List.of("Location", RequestCorrelationFilter.REQUEST_ID_HEADER));

        /*
         * JWTs are currently sent through the Authorization header,
         * not through browser cookies.
         */
        configuration.setAllowCredentials(false);

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
