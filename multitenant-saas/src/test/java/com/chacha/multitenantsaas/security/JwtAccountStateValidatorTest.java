package com.chacha.multitenantsaas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.SystemAdmin;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.entity.UserStatus;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.SystemAdminRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class JwtAccountStateValidatorTest {

    @Mock private AppUserRepository appUserRepository;

    @Mock private SystemAdminRepository systemAdminRepository;

    @Test
    void activeTenantUserWithCurrentSessionIsAccepted() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AppUser user = activeUser(TenantStatus.ACTIVE, 3L);

        when(appUserRepository.findSessionUserByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                new TenantSessionJwtValidator(appUserRepository)
                        .validate(tenantJwt(tenantId, userId, 3L));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void inactiveTenantInvalidatesExistingUserAccessToken() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AppUser user = activeUser(TenantStatus.INACTIVE, 0L);

        when(appUserRepository.findSessionUserByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                new TenantSessionJwtValidator(appUserRepository)
                        .validate(tenantJwt(tenantId, userId, 0L));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void inactiveTenantUserInvalidatesExistingAccessToken() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setStatus(TenantStatus.ACTIVE);

        AppUser user = new AppUser();
        user.setTenant(tenant);
        user.setStatus(UserStatus.INACTIVE);

        when(appUserRepository.findSessionUserByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                new TenantSessionJwtValidator(appUserRepository)
                        .validate(tenantJwt(tenantId, userId, 0L));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void sessionVersionMismatchRemainsRejected() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AppUser user = activeUser(TenantStatus.ACTIVE, 4L);

        when(appUserRepository.findSessionUserByTenantIdAndId(tenantId, userId))
                .thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result =
                new TenantSessionJwtValidator(appUserRepository)
                        .validate(tenantJwt(tenantId, userId, 3L));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void activeSystemAdminIsAccepted() {
        UUID systemAdminId = UUID.randomUUID();
        SystemAdmin systemAdmin = mock(SystemAdmin.class);

        when(systemAdmin.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(systemAdminRepository.findById(systemAdminId)).thenReturn(Optional.of(systemAdmin));

        OAuth2TokenValidatorResult result =
                new SystemAdminSessionJwtValidator(systemAdminRepository)
                        .validate(systemAdminJwt(systemAdminId));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void inactiveSystemAdminInvalidatesExistingAccessToken() {
        UUID systemAdminId = UUID.randomUUID();
        SystemAdmin systemAdmin = mock(SystemAdmin.class);

        when(systemAdmin.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(systemAdminRepository.findById(systemAdminId)).thenReturn(Optional.of(systemAdmin));

        OAuth2TokenValidatorResult result =
                new SystemAdminSessionJwtValidator(systemAdminRepository)
                        .validate(systemAdminJwt(systemAdminId));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void systemAdminValidatorIgnoresTenantTokens() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OAuth2TokenValidatorResult result =
                new SystemAdminSessionJwtValidator(systemAdminRepository)
                        .validate(tenantJwt(tenantId, userId, 0L));

        assertThat(result.hasErrors()).isFalse();
        verifyNoInteractions(systemAdminRepository);
    }

    private AppUser activeUser(TenantStatus tenantStatus, long sessionVersion) {
        Tenant tenant = new Tenant();
        tenant.setStatus(tenantStatus);

        AppUser user = new AppUser();
        user.setTenant(tenant);
        user.setStatus(UserStatus.ACTIVE);

        for (long version = 0; version < sessionVersion; version++) {
            user.incrementSessionVersion();
        }

        return user;
    }

    private Jwt tenantJwt(UUID tenantId, UUID userId, long sessionVersion) {
        Instant now = Instant.now();

        return Jwt.withTokenValue("tenant-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("tenantId", tenantId.toString())
                .claim("sessionVersion", sessionVersion)
                .build();
    }

    private Jwt systemAdminJwt(UUID systemAdminId) {
        Instant now = Instant.now();

        return Jwt.withTokenValue("system-admin-token")
                .header("alg", "HS256")
                .subject(systemAdminId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("accountType", "SYSTEM_ADMIN")
                .claim("role", "SYSTEM_ADMIN")
                .build();
    }
}
