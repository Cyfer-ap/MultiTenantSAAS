package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointCreateRequest;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointCreatedResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointResponse;
import com.chacha.multitenantsaas.dto.OutboundWebhookEndpointUpdateRequest;
import com.chacha.multitenantsaas.dto.OutboundWebhookSecretRotatedResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.AuditAction;
import com.chacha.multitenantsaas.entity.OutboundWebhookEndpoint;
import com.chacha.multitenantsaas.entity.OutboundWebhookEventType;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.entity.TenantStatus;
import com.chacha.multitenantsaas.exception.AuthenticationFailedException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.OutboundWebhookEndpointRepository;
import com.chacha.multitenantsaas.repository.TenantRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundWebhookEndpointService {

    private static final String SECRET_PREFIX = "mwh_";
    private static final int SECRET_HINT_LENGTH = 8;

    private final OutboundWebhookEndpointRepository endpointRepository;
    private final TenantRepository tenantRepository;
    private final OutboundWebhookUrlValidator urlValidator;
    private final OutboundWebhookSecretCipher secretCipher;
    private final SecureTokenService secureTokenService;
    private final AuditLogService auditLogService;

    public OutboundWebhookEndpointService(
            OutboundWebhookEndpointRepository endpointRepository,
            TenantRepository tenantRepository,
            OutboundWebhookUrlValidator urlValidator,
            OutboundWebhookSecretCipher secretCipher,
            SecureTokenService secureTokenService,
            AuditLogService auditLogService) {
        this.endpointRepository = endpointRepository;
        this.tenantRepository = tenantRepository;
        this.urlValidator = urlValidator;
        this.secretCipher = secretCipher;
        this.secureTokenService = secureTokenService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OutboundWebhookEndpointCreatedResponse create(
            UUID tenantId, AppUser actor, OutboundWebhookEndpointCreateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);

        String rawSecret = generateSecret();
        Instant now = Instant.now();
        OutboundWebhookEndpoint endpoint =
                new OutboundWebhookEndpoint(
                        tenant,
                        normalizeName(request.name()),
                        urlValidator.validateAndNormalize(request.url()),
                        request.enabled() == null || request.enabled(),
                        normalizeEvents(request.events()),
                        secretCipher.encrypt(rawSecret),
                        secretHint(rawSecret),
                        actor,
                        now);

        OutboundWebhookEndpoint saved = endpointRepository.saveAndFlush(endpoint);
        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.OUTBOUND_WEBHOOK_CREATED,
                "Created outbound webhook endpoint " + saved.getId());

        return new OutboundWebhookEndpointCreatedResponse(mapResponse(saved), rawSecret);
    }

    @Transactional(readOnly = true)
    public List<OutboundWebhookEndpointResponse> list(UUID tenantId) {
        requireTenant(tenantId);
        return endpointRepository
                .findAllByTenant_IdAndArchivedAtIsNullOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::mapResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OutboundWebhookEndpointResponse get(UUID tenantId, UUID endpointId) {
        requireTenant(tenantId);
        return mapResponse(requireActiveEndpoint(tenantId, endpointId));
    }

    @Transactional
    public OutboundWebhookEndpointResponse update(
            UUID tenantId,
            UUID endpointId,
            AppUser actor,
            OutboundWebhookEndpointUpdateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        OutboundWebhookEndpoint endpoint = requireActiveEndpoint(tenantId, endpointId);
        Instant now = Instant.now();

        endpoint.update(
                normalizeName(request.name()),
                urlValidator.validateAndNormalize(request.url()),
                request.enabled(),
                normalizeEvents(request.events()),
                actor,
                now);
        endpointRepository.save(endpoint);
        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.OUTBOUND_WEBHOOK_UPDATED,
                "Updated outbound webhook endpoint " + endpoint.getId());
        return mapResponse(endpoint);
    }

    @Transactional
    public OutboundWebhookSecretRotatedResponse rotateSecret(
            UUID tenantId, UUID endpointId, AppUser actor) {
        Tenant tenant = getActiveTenant(tenantId);
        requireTenantActor(tenantId, actor);
        OutboundWebhookEndpoint endpoint = requireActiveEndpoint(tenantId, endpointId);
        String rawSecret = generateSecret();
        Instant now = Instant.now();

        endpoint.rotateSecret(secretCipher.encrypt(rawSecret), secretHint(rawSecret), actor, now);
        endpointRepository.save(endpoint);
        auditLogService.recordSelfSuccess(
                tenant,
                actor,
                AuditAction.OUTBOUND_WEBHOOK_SECRET_ROTATED,
                "Rotated signing secret for outbound webhook endpoint " + endpoint.getId());

        return new OutboundWebhookSecretRotatedResponse(
                endpoint.getId(),
                endpoint.getSecretHint(),
                endpoint.getSecretVersion(),
                rawSecret,
                endpoint.getSecretRotatedAt());
    }

    @Transactional
    public OutboundWebhookEndpointResponse archive(UUID tenantId, UUID endpointId, AppUser actor) {
        Tenant tenant = requireTenant(tenantId);
        requireTenantActor(tenantId, actor);
        OutboundWebhookEndpoint endpoint =
                endpointRepository
                        .findByIdAndTenant_Id(endpointId, tenantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Outbound webhook endpoint not found"));

        if (!endpoint.isArchived()) {
            endpoint.archive(actor, Instant.now());
            endpointRepository.save(endpoint);
            auditLogService.recordSelfSuccess(
                    tenant,
                    actor,
                    AuditAction.OUTBOUND_WEBHOOK_ARCHIVED,
                    "Archived outbound webhook endpoint " + endpoint.getId());
        }
        return mapResponse(endpoint);
    }

    public List<String> eventCatalog() {
        return java.util.Arrays.stream(OutboundWebhookEventType.values())
                .map(OutboundWebhookEventType::wireName)
                .toList();
    }

    private OutboundWebhookEndpoint requireActiveEndpoint(UUID tenantId, UUID endpointId) {
        Objects.requireNonNull(endpointId, "endpointId must not be null");
        return endpointRepository
                .findByIdAndTenant_IdAndArchivedAtIsNull(endpointId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Outbound webhook endpoint not found"));
    }

    private Tenant getActiveTenant(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Outbound webhooks can only be configured for active tenants");
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

    private String normalizeName(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Webhook endpoint name must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException(
                    "Webhook endpoint name must not exceed 120 characters");
        }
        return normalized;
    }

    private Set<OutboundWebhookEventType> normalizeEvents(Set<OutboundWebhookEventType> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one outbound webhook event must be selected");
        }
        if (events.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Webhook event selection must not contain null");
        }
        return EnumSet.copyOf(events);
    }

    private String generateSecret() {
        return SECRET_PREFIX + secureTokenService.generateToken();
    }

    private String secretHint(String rawSecret) {
        int start = Math.max(0, rawSecret.length() - SECRET_HINT_LENGTH);
        return "****" + rawSecret.substring(start);
    }

    private OutboundWebhookEndpointResponse mapResponse(OutboundWebhookEndpoint endpoint) {
        return new OutboundWebhookEndpointResponse(
                endpoint.getId(),
                endpoint.getTenant().getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                endpoint.isEnabled(),
                endpoint.getEventTypes(),
                endpoint.getSecretHint(),
                endpoint.getSecretVersion(),
                endpoint.getCreatedByUser().getId(),
                endpoint.getUpdatedByUser().getId(),
                endpoint.getSecretRotatedAt(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt());
    }
}
