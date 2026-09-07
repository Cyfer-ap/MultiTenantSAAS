package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.config.OidcLoginProperties;
import com.chacha.multitenantsaas.dto.LoginResponse;
import com.chacha.multitenantsaas.entity.OidcSessionHandoff;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.repository.OidcSessionHandoffRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OidcSessionHandoffService {

    private final OidcSessionHandoffRepository handoffRepository;
    private final SecureTokenService secureTokenService;
    private final OidcLoginProperties properties;
    private final OidcSessionService sessionService;

    public OidcSessionHandoffService(
            OidcSessionHandoffRepository handoffRepository,
            SecureTokenService secureTokenService,
            OidcLoginProperties properties,
            OidcSessionService sessionService) {
        this.handoffRepository = handoffRepository;
        this.secureTokenService = secureTokenService;
        this.properties = properties;
        this.sessionService = sessionService;
    }

    @Transactional
    public IssuedHandoff issue(UUID tenantId, UUID userId, boolean persistentSession) {
        Instant now = Instant.now();
        String rawCode = secureTokenService.generateToken();
        String codeHash = secureTokenService.hashToken(rawCode);
        Instant expiresAt = now.plus(properties.sessionHandoffTtl());

        handoffRepository.save(
                new OidcSessionHandoff(
                        codeHash, tenantId, userId, persistentSession, now, expiresAt));

        return new IssuedHandoff(rawCode, expiresAt);
    }

    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResponse exchange(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw failed();
        }

        String codeHash = secureTokenService.hashToken(rawCode.trim());
        OidcSessionHandoff handoff =
                handoffRepository.findByCodeHashForUpdate(codeHash).orElseThrow(this::failed);

        try {
            handoff.consume(Instant.now());
        } catch (IllegalStateException exception) {
            throw failed();
        }

        handoffRepository.save(handoff);
        return sessionService.issue(
                handoff.getTenantId(), handoff.getUserId(), handoff.isPersistentSession());
    }

    public record IssuedHandoff(String code, Instant expiresAt) {}

    private AuthenticationFailedException failed() {
        return new AuthenticationFailedException("OIDC session handoff is invalid or expired");
    }
}
