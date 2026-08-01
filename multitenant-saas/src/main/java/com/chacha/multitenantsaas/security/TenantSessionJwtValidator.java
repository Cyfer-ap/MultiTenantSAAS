package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TenantSessionJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN = "invalid_token";

    private final AppUserRepository appUserRepository;

    public TenantSessionJwtValidator(
            AppUserRepository appUserRepository
    ) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if ("SYSTEM_ADMIN".equals(
                jwt.getClaimAsString("accountType")
        )) {
            return OAuth2TokenValidatorResult.success();
        }

        try {
            UUID tenantId = UUID.fromString(
                    jwt.getClaimAsString("tenantId")
            );
            UUID userId = UUID.fromString(jwt.getSubject());

            Object sessionVersionClaim =
                    jwt.getClaims().get("sessionVersion");

            long tokenSessionVersion;

            if (sessionVersionClaim == null) {
                // Access tokens issued before V8 are version zero.
                tokenSessionVersion = 0L;
            } else if (sessionVersionClaim instanceof Number number) {
                tokenSessionVersion = number.longValue();
            } else {
                return failure(
                        "The tenant access token has an invalid session version."
                );
            }

            Optional<AppUser> user =
                    appUserRepository.findByTenantIdAndId(
                            tenantId,
                            userId
                    );

            if (user.isEmpty()) {
                return failure(
                        "The tenant access-token account no longer exists."
                );
            }

            if (user.get().getSessionVersion()
                    != tokenSessionVersion) {
                return failure(
                        "The tenant access-token session was revoked."
                );
            }

            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException exception) {
            return failure(
                    "The tenant access token contains invalid session data."
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
