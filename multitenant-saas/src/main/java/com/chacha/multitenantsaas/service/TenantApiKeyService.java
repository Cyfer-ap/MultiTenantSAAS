package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantApiKeyCreateRequest;
import com.chacha.multitenantsaas.dto.TenantApiKeyCreatedResponse;
import com.chacha.multitenantsaas.dto.TenantApiKeyResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantApiKey;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantApiKeyRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantApiKeyService {

    private static final String KEY_PREFIX = "mts_";
    private static final int DISPLAY_PREFIX_LENGTH = 16;

    private final TenantApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final SecureTokenService secureTokenService;
    private final AuditLogService auditLogService;

    public TenantApiKeyService(
            TenantApiKeyRepository apiKeyRepository,
            TenantRepository tenantRepository,
            SecureTokenService secureTokenService,
            AuditLogService auditLogService) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.secureTokenService = secureTokenService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TenantApiKeyCreatedResponse create(
            UUID tenantId, AppUser actor, TenantApiKeyCreateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        String name = normalizeName(request.name());
        Instant expiresAt = request.expiresAt();
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("API key expiry must be in the future");
        }

        String rawApiKey = KEY_PREFIX + secureTokenService.generateToken();
        String keyPrefix = rawApiKey.substring(0, DISPLAY_PREFIX_LENGTH);
        TenantApiKey apiKey =
                new TenantApiKey(
                        tenant,
                        name,
                        keyPrefix,
                        secureTokenService.hashToken(rawApiKey),
                        actor,
                        expiresAt);
        TenantApiKey saved = apiKeyRepository.saveAndFlush(apiKey);

        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.API_KEY_CREATED,
                "Created tenant API key " + saved.getId());

        return new TenantApiKeyCreatedResponse(
                saved.getId(),
                tenantId,
                saved.getName(),
                saved.getKeyPrefix(),
                rawApiKey,
                actor.getId(),
                saved.getExpiresAt(),
                saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<TenantApiKeyResponse> list(UUID tenantId) {
        requireTenant(tenantId);
        return apiKeyRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::mapResponse)
                .toList();
    }

    @Transactional
    public TenantApiKeyResponse revoke(UUID tenantId, UUID apiKeyId, AppUser actor) {
        Tenant tenant = requireTenant(tenantId);
        requireTenantActor(tenantId, actor);
        TenantApiKey apiKey =
                apiKeyRepository
                        .findByTenant_IdAndId(tenantId, apiKeyId)
                        .orElseThrow(() -> new ResourceNotFoundException("API key not found"));

        if (apiKey.getRevokedAt() == null) {
            apiKey.revoke(Instant.now());
            apiKeyRepository.save(apiKey);
            auditLogService.recordSelfSuccess(
                    tenant,
                    actor,
                    AuditAction.API_KEY_REVOKED,
                    "Revoked tenant API key " + apiKey.getId());
        }

        return mapResponse(apiKey);
    }

    private Tenant getActiveTenant(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException("API keys can only be created for active tenants");
        }
        return tenant;
    }

    private Tenant requireTenant(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return tenantRepository
                .findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }

    private void requireTenantActor(UUID tenantId, AppUser actor) {
        if (actor == null
                || actor.getTenant() == null
                || !tenantId.equals(actor.getTenant().getId())) {
            throw new AuthenticationFailedException(
                    "Authenticated user does not belong to this tenant");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("API key name must not be blank");
        }
        String normalized = name.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("API key name must not exceed 100 characters");
        }
        return normalized;
    }

    private TenantApiKeyResponse mapResponse(TenantApiKey apiKey) {
        return new TenantApiKeyResponse(
                apiKey.getId(),
                apiKey.getTenant().getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.isActive(),
                apiKey.getCreatedByUser().getId(),
                apiKey.getExpiresAt(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt());
    }
}
