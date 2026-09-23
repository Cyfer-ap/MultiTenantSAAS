package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.service.SecureTokenService;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalGuestSessionService {

    private static final String INVALID_INVITATION = "Invalid or unavailable guest invitation";
    private static final String INVALID_SESSION = "Invalid or expired guest session";

    private final ExternalAccessGrantRepository grantRepository;
    private final ExternalAccessGrantCapabilityRepository capabilityRepository;
    private final ExternalGuestSessionRepository sessionRepository;
    private final SecureTokenService secureTokenService;

    public ExternalGuestSessionService(
            ExternalAccessGrantRepository grantRepository,
            ExternalAccessGrantCapabilityRepository capabilityRepository,
            ExternalGuestSessionRepository sessionRepository,
            SecureTokenService secureTokenService) {
        this.grantRepository = grantRepository;
        this.capabilityRepository = capabilityRepository;
        this.sessionRepository = sessionRepository;
        this.secureTokenService = secureTokenService;
    }

    @Transactional
    public ExternalAccessDtos.ExchangeResponse exchange(String invitationToken) {
        String tokenHash = secureTokenService.hashToken(invitationToken.trim());
        ExternalAccessGrant grant =
                grantRepository
                        .findByInvitationTokenHashForUpdate(tokenHash)
                        .orElseThrow(() -> new AuthenticationFailedException(INVALID_INVITATION));
        Instant now = Instant.now();
        if (!grant.isAvailable(now) || grant.getAcceptedAt() != null) {
            throw new AuthenticationFailedException(INVALID_INVITATION);
        }

        grant.accept(now);
        grantRepository.saveAndFlush(grant);

        String rawSessionToken = secureTokenService.generateToken();
        Instant sessionExpiresAt = grant.getExpiresAt();
        sessionRepository.saveAndFlush(
                new ExternalGuestSession(
                        grant.getTenantId(),
                        grant.getProjectId(),
                        grant.getId(),
                        secureTokenService.hashToken(rawSessionToken),
                        sessionExpiresAt));

        return new ExternalAccessDtos.ExchangeResponse(
                grant.getId(),
                grant.getProjectId(),
                grant.getGuestName(),
                rawSessionToken,
                sessionExpiresAt);
    }

    @Transactional(readOnly = true)
    ExternalGuestSessionContext requireSession(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new AuthenticationFailedException(INVALID_SESSION);
        }

        String tokenHash = secureTokenService.hashToken(rawSessionToken.trim());
        ExternalGuestSession session =
                sessionRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() -> new AuthenticationFailedException(INVALID_SESSION));
        ExternalAccessGrant grant =
                grantRepository
                        .findByTenantIdAndProjectIdAndId(
                                session.getTenantId(), session.getProjectId(), session.getGrantId())
                        .orElseThrow(() -> new AuthenticationFailedException(INVALID_SESSION));

        Instant now = Instant.now();
        if (!session.isAvailable(now) || !grant.isAvailable(now) || grant.getAcceptedAt() == null) {
            throw new AuthenticationFailedException(INVALID_SESSION);
        }

        return new ExternalGuestSessionContext(
                grant.getId(),
                grant.getTenantId(),
                grant.getProjectId(),
                grant.getGuestName(),
                grant.getGuestEmail(),
                capabilities(grant),
                grant.getExpiresAt(),
                session.getExpiresAt());
    }

    private Set<ExternalAccessCapability> capabilities(ExternalAccessGrant grant) {
        return capabilityRepository
                .findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
                        grant.getTenantId(), grant.getProjectId(), grant.getId())
                .stream()
                .map(ExternalAccessGrantCapability::getCapability)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
