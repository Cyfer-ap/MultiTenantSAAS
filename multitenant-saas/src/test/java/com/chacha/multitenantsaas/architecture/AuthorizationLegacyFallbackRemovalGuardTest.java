package com.chacha.multitenantsaas.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthorizationLegacyFallbackRemovalGuardTest {

    private static final Path
            AUTHORIZATION_SECURITY_SERVICE =
            Path.of(
                    "src",
                    "main",
                    "java",
                    "com",
                    "chacha",
                    "multitenantsaas",
                    "security",
                    "AuthorizationSecurityService.java"
            );

    private static final List<String>
            FORBIDDEN_FALLBACK_REFERENCES =
            List.of(
                    "TenantSecurityService",
                    "isLegacyFallbackAllowed",
                    "OrLegacy",
                    ".canReadTasks(",
                    ".canManageTasks(",
                    ".canUpdateTaskStatus("
            );

    @Test
    void authorizationSecurityContainsNoLegacyFallback()
            throws IOException {

        String source =
                Files.readString(
                        AUTHORIZATION_SECURITY_SERVICE
                );

        for (
                String forbiddenReference
                : FORBIDDEN_FALLBACK_REFERENCES
        ) {
            assertFalse(
                    source.contains(
                            forbiddenReference
                    ),
                    () ->
                            "AuthorizationSecurityService "
                                    + "contains forbidden "
                                    + "legacy fallback: "
                                    + forbiddenReference
            );
        }
    }
}