package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.RefreshToken;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.RefreshTokenRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceSecurityTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AppUser user;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(refreshTokenRepository, appUserRepository, 12L, 30L);
    }

    @Test
    void browserRefreshCredentialStoresOnlyHashesAndPersistenceMode() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued =
                service.createIssuedRefreshToken(user, true);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(refreshTokenRepository).save(captor.capture());

        RefreshToken stored = captor.getValue();

        assertThat(issued.refreshToken()).isNotBlank();
        assertThat(issued.csrfToken()).isNotBlank();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(issued.refreshToken());
        assertThat(stored.getCsrfTokenHash()).hasSize(64).isNotEqualTo(issued.csrfToken());
        assertThat(stored.isPersistentSession()).isTrue();
    }

    @Test
    void cookieBackedRotationRejectsWrongCsrfProof() {
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued =
                service.createIssuedRefreshToken(user, true);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(refreshTokenRepository).save(captor.capture());

        RefreshToken stored = captor.getValue();

        when(refreshTokenRepository.findUserIdByTokenHash(anyString()))
                .thenReturn(Optional.of(userId));
        when(appUserRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(stored));

        assertThrows(
                AuthenticationFailedException.class,
                () -> service.rotateRefreshToken(issued.refreshToken(), "wrong-csrf-proof"));
    }
}
