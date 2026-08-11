package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SystemAdminSessionJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN = "invalid_token";

    private final SystemAdminRepository systemAdminRepository;

    public SystemAdminSessionJwtValidator(
            SystemAdminRepository systemAdminRepository
    ) {
        this.systemAdminRepository = systemAdminRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!"SYSTEM_ADMIN".equals(
                jwt.getClaimAsString("accountType")
        )) {
            return OAuth2TokenValidatorResult.success();
        }

        try {
            UUID systemAdminId = UUID.fromString(
                    jwt.getSubject()
            );

            Optional<SystemAdmin> systemAdmin =
                    systemAdminRepository.findById(systemAdminId);

            if (systemAdmin.isEmpty()) {
                return failure(
                        "The system-admin access-token account no longer exists."
                );
            }

            if (systemAdmin.get().getStatus()
                    != UserStatus.ACTIVE) {
                return failure(
                        "The system-admin access-token account is not active."
                );
            }

            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException exception) {
            return failure(
                    "The system-admin access token contains invalid session data."
            );
        }
    }

    private OAuth2TokenValidatorResult failure(
            String description
    ) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(
                        INVALID_TOKEN,
                        description,
                        null
                )
        );
    }
}
