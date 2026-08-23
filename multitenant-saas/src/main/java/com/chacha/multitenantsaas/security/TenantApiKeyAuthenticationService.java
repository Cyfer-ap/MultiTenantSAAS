package com.chacha.multitenantsaas.security;

import com.chacha.multitenantsaas.entity.TenantApiKey;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.repository.TenantApiKeyRepository;
import com.chacha.multitenantsaas.service.SecureTokenService;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantApiKeyAuthenticationService {

    static final String KEY_PREFIX = "mts_";
    static final int LOOKUP_PREFIX_LENGTH = 16;

    private final TenantApiKeyRepository apiKeyRepository;
    private final SecureTokenService secureTokenService;

    public TenantApiKeyAuthenticationService(
            TenantApiKeyRepository apiKeyRepository, SecureTokenService secureTokenService) {
        this.apiKeyRepository = apiKeyRepository;
        this.secureTokenService = secureTokenService;
    }

    @Transactional
    public Optional<TenantApiKeyPrincipal> authenticate(String rawApiKey) {
        if (rawApiKey == null
                || !rawApiKey.startsWith(KEY_PREFIX)
                || rawApiKey.length() < LOOKUP_PREFIX_LENGTH) {
            return Optional.empty();
        }

        String lookupPrefix = rawApiKey.substring(0, LOOKUP_PREFIX_LENGTH);
        TenantApiKey apiKey = apiKeyRepository.findByKeyPrefix(lookupPrefix).orElse(null);
        Instant authenticatedAt = Instant.now();

        if (apiKey == null
                || apiKey.getTenant().getStatus() != TenantStatus.ACTIVE
                || !apiKey.isActive()
                || !secureTokenService.matchesToken(rawApiKey, apiKey.getKeyHash())) {
            return Optional.empty();
        }

        apiKey.markUsed(authenticatedAt);
        apiKeyRepository.save(apiKey);

        return Optional.of(
                new TenantApiKeyPrincipal(
                        apiKey.getTenant().getId(),
                        apiKey.getId(),
                        apiKey.getName(),
                        apiKey.getKeyPrefix()));
    }
}
