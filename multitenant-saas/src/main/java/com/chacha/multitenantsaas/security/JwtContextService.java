package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.UserRole;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JwtContextService {

    public AuthenticatedUserContext getCurrentUser(Jwt jwt) {
        if (jwt == null) {
            throw new AuthenticationFailedException("Missing authentication token");
        }

        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenantId"));

            String email = jwt.getClaimAsString("email");
            String fullName = jwt.getClaimAsString("fullName");
            String roleValue = jwt.getClaimAsString("role");

            UserRole role = UserRole.valueOf(roleValue);

            return new AuthenticatedUserContext(
                    tenantId,
                    userId,
                    email,
                    fullName,
                    role
            );

        } catch (Exception exception) {
            throw new AuthenticationFailedException("Invalid token data");
        }
    }
}