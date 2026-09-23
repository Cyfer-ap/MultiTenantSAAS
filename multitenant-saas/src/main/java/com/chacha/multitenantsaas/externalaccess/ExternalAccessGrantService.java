package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.projects.access.ProjectAccessPort;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.service.SecureTokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalAccessGrantService {

    private final ExternalAccessGrantRepository grantRepository;
    private final ExternalAccessGrantCapabilityRepository capabilityRepository;
    private final ProjectAccessPort projectAccessPort;
    private final CurrentActorService currentActorService;
    private final SecureTokenService secureTokenService;
    private final ExternalAccessProperties properties;

    public ExternalAccessGrantService(
            ExternalAccessGrantRepository grantRepository,
            ExternalAccessGrantCapabilityRepository capabilityRepository,
            ProjectAccessPort projectAccessPort,
            CurrentActorService currentActorService,
            SecureTokenService secureTokenService,
            ExternalAccessProperties properties) {
        this.grantRepository = grantRepository;
        this.capabilityRepository = capabilityRepository;
        this.projectAccessPort = projectAccessPort;
        this.currentActorService = currentActorService;
        this.secureTokenService = secureTokenService;
        this.properties = properties;
    }

    @Transactional
    public ExternalAccessDtos.GrantCreatedResponse create(
            UUID tenantId, UUID projectId, ExternalAccessDtos.CreateGrantRequest request, Jwt jwt) {
        var project = projectAccessPort.requireProject(tenantId, projectId);
        if (project.status() == ProjectStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived projects cannot create external grants");
        }

        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        Instant now = Instant.now();
        validateExpiry(now, request.expiresAt());
        Set<ExternalAccessCapability> capabilities = Set.copyOf(request.capabilities());
        validateCapabilities(capabilities);

        String invitationToken = secureTokenService.generateToken();
        ExternalAccessGrant grant =
                grantRepository.saveAndFlush(
                        new ExternalAccessGrant(
                                tenantId,
                                projectId,
                                actorUserId,
                                request.guestName().trim(),
                                request.guestEmail().trim().toLowerCase(Locale.ROOT),
                                secureTokenService.hashToken(invitationToken),
                                request.expiresAt()));

        capabilityRepository.saveAll(
                capabilities.stream()
                        .map(
                                capability ->
                                        new ExternalAccessGrantCapability(
                                                tenantId, projectId, grant.getId(), capability))
                        .toList());

        return new ExternalAccessDtos.GrantCreatedResponse(
                map(grant, capabilities, now), invitationToken);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExternalAccessDtos.GrantResponse> list(
            UUID tenantId, UUID projectId, Pageable pageable) {
        projectAccessPort.requireProject(tenantId, projectId);
        Page<ExternalAccessGrant> page =
                grantRepository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(
                        tenantId, projectId, pageable);
        Instant now = Instant.now();
        var content =
                page.getContent().stream()
                        .map(
                                grant ->
                                        map(
                                                grant,
                                                capabilities(tenantId, projectId, grant.getId()),
                                                now))
                        .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    @Transactional
    public ExternalAccessDtos.GrantResponse revoke(
            UUID tenantId, UUID projectId, UUID grantId, Jwt jwt) {
        projectAccessPort.requireProject(tenantId, projectId);
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        ExternalAccessGrant grant =
                grantRepository
                        .findForUpdate(tenantId, projectId, grantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "External access grant not found"));
        Instant now = Instant.now();
        grant.revoke(actorUserId, now);
        grantRepository.saveAndFlush(grant);
        return map(grant, capabilities(tenantId, projectId, grantId), now);
    }

    private void validateExpiry(Instant now, Instant expiresAt) {
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("External access expiry must be in the future");
        }
        int maxGrantDays = properties.getMaxGrantDays();
        if (maxGrantDays <= 0 || expiresAt.isAfter(now.plus(maxGrantDays, ChronoUnit.DAYS))) {
            throw new IllegalArgumentException(
                    "External access expiry exceeds the configured maximum");
        }
    }

    private void validateCapabilities(Set<ExternalAccessCapability> capabilities) {
        if (!capabilities.contains(ExternalAccessCapability.PROJECT_READ)) {
            throw new IllegalArgumentException("External grants must include PROJECT_READ");
        }
        if (capabilities.contains(ExternalAccessCapability.TASK_COMMENT_CREATE)
                && !capabilities.contains(ExternalAccessCapability.TASK_READ)) {
            throw new IllegalArgumentException("TASK_COMMENT_CREATE requires TASK_READ");
        }
        if (capabilities.contains(ExternalAccessCapability.APPROVAL_REVIEW)
                && !capabilities.contains(ExternalAccessCapability.TASK_READ)) {
            throw new IllegalArgumentException("APPROVAL_REVIEW requires TASK_READ");
        }
        if (capabilities.size() > 4) {
            throw new IllegalArgumentException("External grant capability set is too large");
        }
    }

    private Set<ExternalAccessCapability> capabilities(
            UUID tenantId, UUID projectId, UUID grantId) {
        return capabilityRepository
                .findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
                        tenantId, projectId, grantId)
                .stream()
                .map(ExternalAccessGrantCapability::getCapability)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private ExternalAccessDtos.GrantResponse map(
            ExternalAccessGrant grant, Set<ExternalAccessCapability> capabilities, Instant now) {
        return new ExternalAccessDtos.GrantResponse(
                grant.getId(),
                grant.getProjectId(),
                grant.getGuestName(),
                grant.getGuestEmail(),
                capabilities,
                state(grant, now),
                grant.getExpiresAt(),
                grant.getAcceptedAt(),
                grant.getRevokedAt(),
                grant.getCreatedByUserId(),
                grant.getRevokedByUserId(),
                grant.getCreatedAt(),
                grant.getUpdatedAt());
    }

    private ExternalAccessGrantState state(ExternalAccessGrant grant, Instant now) {
        if (grant.getRevokedAt() != null) {
            return ExternalAccessGrantState.REVOKED;
        }
        if (!grant.getExpiresAt().isAfter(now)) {
            return ExternalAccessGrantState.EXPIRED;
        }
        return grant.getAcceptedAt() == null
                ? ExternalAccessGrantState.INVITED
                : ExternalAccessGrantState.ACCEPTED;
    }
}
