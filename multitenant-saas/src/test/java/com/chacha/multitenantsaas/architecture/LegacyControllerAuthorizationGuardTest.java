package com.chacha.multitenantsaas.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LegacyControllerAuthorizationGuardTest {

    private static final Path CONTROLLER_DIRECTORY =
            Path.of("src", "main", "java", "com", "chacha", "multitenantsaas", "controller");

    private static final List<String> FORBIDDEN_SECURITY_REFERENCES =
            List.of("@tenantSecurity", "@projectSecurity", "OrLegacy");

    @Test
    void controllersDoNotUseLegacySecurityBeans() throws IOException {

        List<String> violations = new ArrayList<>();

        try (Stream<Path> sourceFiles = Files.walk(CONTROLLER_DIRECTORY)) {
            sourceFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspectFile(path, violations));
        }

        assertTrue(
                violations.isEmpty(),
                """
                Controllers must use authorizationSecurity,
                systemSecurity, authenticated(), permitAll(),
                or denyAll() instead of direct legacy security
                beans.

                Violations:
                %s
                """
                        .formatted(String.join(System.lineSeparator(), violations)));
    }

    private void inspectFile(Path sourceFile, List<String> violations) {
        try {
            String content = Files.readString(sourceFile);

            for (String forbiddenReference : FORBIDDEN_SECURITY_REFERENCES) {
                if (content.contains(forbiddenReference)) {
                    violations.add(sourceFile + " contains " + forbiddenReference);
                }
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to inspect controller: " + sourceFile, exception);
        }
    }
}
