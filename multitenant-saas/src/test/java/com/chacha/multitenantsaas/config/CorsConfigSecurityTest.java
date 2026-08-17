package com.chacha.multitenantsaas.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsConfigSecurityTest {

    @Test
    void credentialedCorsRejectsWildcardOrigin() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("*"));

        CorsConfig config = new CorsConfig(properties);

        assertThrows(IllegalStateException.class, config::corsConfigurationSource);
    }
}
