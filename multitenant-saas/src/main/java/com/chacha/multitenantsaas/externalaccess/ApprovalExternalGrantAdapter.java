package com.chacha.multitenantsaas.externalaccess;

import com.chacha.multitenantsaas.approvals.ApprovalExternalGrantPort;
import com.chacha.multitenantsaas.approvals.ApprovalExternalGrantSnapshot;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApprovalExternalGrantAdapter implements ApprovalExternalGrantPort {

    private final ExternalAccessGrantRepository grantRepository;
    private final ExternalAccessGrantCapabilityRepository capabilityRepository;

    public ApprovalExternalGrantAdapter(
            ExternalAccessGrantRepository grantRepository,
            ExternalAccessGrantCapabilityRepository capabilityRepository) {
        this.grantRepository = grantRepository;
        this.capabilityRepository = capabilityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalExternalGrantSnapshot requireApprovalReviewGrant(
            UUID tenantId, UUID projectId, UUID grantId) {
        ExternalAccessGrant grant =
                grantRepository
                        .findByTenantIdAndProjectIdAndId(tenantId, projectId, grantId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "External access grant not found"));
        if (!grant.isAvailable(Instant.now())) {
            throw new IllegalArgumentException("External access grant is no longer active");
        }

        boolean allowed =
                capabilityRepository
                        .findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
                                tenantId, projectId, grantId)
                        .stream()
                        .map(ExternalAccessGrantCapability::getCapability)
                        .anyMatch(ExternalAccessCapability.APPROVAL_REVIEW::equals);
        if (!allowed) {
            throw new IllegalArgumentException(
                    "External access grant does not allow approval review");
        }

        return new ApprovalExternalGrantSnapshot(
                grant.getId(), grant.getGuestName(), grant.getGuestEmail());
    }
}
