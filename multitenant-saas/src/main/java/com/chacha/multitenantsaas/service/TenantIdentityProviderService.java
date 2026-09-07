package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.TenantIdentityProviderCreateRequest;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderResponse;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderSecretRotatedResponse;
import com.chacha.multitenantsaas.dto.TenantIdentityProviderUpdateRequest;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.IdentityProviderProtocol;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantIdentityProvider;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TenantIdentityProviderRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantIdentityProviderService {

    private static final int SECRET_HINT_LENGTH = 6;
    private static final int MAX_SCOPES = 20;
    private static final int MAX_SCOPE_LENGTH = 100;
    private static final Pattern SCOPE_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");
    private static final Set<String> DEFAULT_SCOPES = Set.of("openid", "profile", "email");

    private final TenantIdentityProviderRepository identityProviderRepository;
    private final TenantRepository tenantRepository;
    private final IdentityProviderIssuerValidator issuerValidator;
    private final IdentityProviderSecretCipher secretCipher;
    private final AuditLogService auditLogService;

    public TenantIdentityProviderService(
            TenantIdentityProviderRepository identityProviderRepository,
            TenantRepository tenantRepository,
            IdentityProviderIssuerValidator issuerValidator,
            IdentityProviderSecretCipher secretCipher,
            AuditLogService auditLogService) {
        this.identityProviderRepository = identityProviderRepository;
        this.tenantRepository = tenantRepository;
        this.issuerValidator = issuerValidator;
        this.secretCipher = secretCipher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TenantIdentityProviderResponse create(
            UUID tenantId, AppUser actor, TenantIdentityProviderCreateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        if (identityProviderRepository.existsByTenant_Id(tenantId)) {
            throw new DuplicateResourceException(
                    "Identity-provider configuration already exists for this tenant");
        }
        if (request.protocol() != IdentityProviderProtocol.OIDC) {
            throw new IllegalArgumentException(
                    "Only OIDC identity providers are supported currently");
        }

        String rawSecret = normalizeClientSecret(request.clientSecret());
        Instant now = Instant.now();
        TenantIdentityProvider identityProvider =
                new TenantIdentityProvider(
                        tenant,
                        request.protocol(),
                        normalizeDisplayName(request.displayName()),
                        issuerValidator.validateAndNormalize(request.issuerUri()),
                        normalizeClientId(request.clientId()),
                        secretCipher.encrypt(rawSecret),
                        secretHint(rawSecret),
                        normalizeScopes(request.scopes()),
                        actor,
                        now);

        TenantIdentityProvider saved = identityProviderRepository.saveAndFlush(identityProvider);
        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.IDENTITY_PROVIDER_CONFIGURED,
                "Configured tenant identity provider " + saved.getId());
        return mapResponse(saved);
    }

    @Transactional(readOnly = true)
    public TenantIdentityProviderResponse get(UUID tenantId) {
        requireTenant(tenantId);
        return mapResponse(requireConfiguration(tenantId));
    }

    @Transactional
    public TenantIdentityProviderResponse update(
            UUID tenantId, AppUser actor, TenantIdentityProviderUpdateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        TenantIdentityProvider identityProvider = requireConfiguration(tenantId);

        identityProvider.update(
                normalizeDisplayName(request.displayName()),
                issuerValidator.validateAndNormalize(request.issuerUri()),
                normalizeClientId(request.clientId()),
                normalizeScopes(request.scopes()),
                actor,
                Instant.now());
        identityProviderRepository.save(identityProvider);
        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.IDENTITY_PROVIDER_UPDATED,
                "Updated tenant identity provider " + identityProvider.getId());
        return mapResponse(identityProvider);
    }

    @Transactional
    public TenantIdentityProviderSecretRotatedResponse rotateClientSecret(
            UUID tenantId, AppUser actor, String clientSecret) {
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        TenantIdentityProvider identityProvider = requireConfiguration(tenantId);
        String rawSecret = normalizeClientSecret(clientSecret);
        Instant now = Instant.now();

        identityProvider.rotateClientSecret(
                secretCipher.encrypt(rawSecret), secretHint(rawSecret), actor, now);
        identityProviderRepository.save(identityProvider);
        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.IDENTITY_PROVIDER_CLIENT_SECRET_ROTATED,
                "Rotated client secret for tenant identity provider " + identityProvider.getId());
        return new TenantIdentityProviderSecretRotatedResponse(
                identityProvider.getId(),
                identityProvider.getClientSecretHint(),
                identityProvider.getSecretVersion(),
                identityProvider.getStatus(),
                identityProvider.getSecretRotatedAt());
    }

    @Transactional
    public TenantIdentityProviderResponse disable(UUID tenantId, AppUser actor) {
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        TenantIdentityProvider identityProvider = requireConfiguration(tenantId);

        if (identityProvider.getStatus()
                != com.chacha.multitenantsaas.entity.TenantIdentityProviderStatus.DISABLED) {
            identityProvider.disable(actor, Instant.now());
            identityProviderRepository.save(identityProvider);
            auditLogService.recordSelfSuccess(
                    tenant,
                    actor,
                    AuditAction.IDENTITY_PROVIDER_DISABLED,
                    "Disabled tenant identity provider " + identityProvider.getId());
        }
        return mapResponse(identityProvider);
    }

    private TenantIdentityProvider requireConfiguration(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return identityProviderRepository
                .findByTenant_Id(tenantId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Tenant identity-provider configuration not found"));
    }

    private Tenant getActiveTenant(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Identity federation can only be configured for active tenants");
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

    private String normalizeDisplayName(String value) {
        return normalizeRequired(value, 120, "Identity-provider display name");
    }

    private String normalizeClientId(String value) {
        return normalizeRequired(value, 512, "Identity-provider client ID");
    }

    private String normalizeClientSecret(String value) {
        String normalized = normalizeRequired(value, 2048, "Identity-provider client secret");
        if (normalized.length() < 8) {
            throw new IllegalArgumentException(
                    "Identity-provider client secret must contain at least 8 characters");
        }
        return normalized;
    }

    private String normalizeRequired(String value, int maxLength, String label) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private Set<String> normalizeScopes(Set<String> requestedScopes) {
        Set<String> source =
                requestedScopes == null || requestedScopes.isEmpty()
                        ? DEFAULT_SCOPES
                        : requestedScopes;
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : source) {
            if (value == null || value.trim().isBlank()) {
                throw new IllegalArgumentException("OIDC scopes must not contain blank values");
            }
            String scope = value.trim();
            if (scope.length() > MAX_SCOPE_LENGTH || !SCOPE_PATTERN.matcher(scope).matches()) {
                throw new IllegalArgumentException("OIDC scope contains unsupported characters");
            }
            normalized.add(scope);
        }
        if (normalized.size() > MAX_SCOPES) {
            throw new IllegalArgumentException("No more than 20 OIDC scopes may be configured");
        }
        if (!normalized.contains("openid")) {
            throw new IllegalArgumentException("OIDC scopes must include openid");
        }
        return normalized;
    }

    private String secretHint(String rawSecret) {
        int start = Math.max(0, rawSecret.length() - SECRET_HINT_LENGTH);
        return "****" + rawSecret.substring(start);
    }

    private TenantIdentityProviderResponse mapResponse(TenantIdentityProvider identityProvider) {
        return new TenantIdentityProviderResponse(
                identityProvider.getId(),
                identityProvider.getTenant().getId(),
                identityProvider.getProtocol(),
                identityProvider.getDisplayName(),
                identityProvider.getIssuerUri(),
                identityProvider.getClientId(),
                identityProvider.getScopes(),
                identityProvider.getStatus(),
                identityProvider.getClientSecretHint(),
                identityProvider.getSecretVersion(),
                identityProvider.getVerifiedAt(),
                identityProvider.getDisabledAt(),
                identityProvider.getSecretRotatedAt(),
                identityProvider.getCreatedAt(),
                identityProvider.getUpdatedAt());
    }
}
