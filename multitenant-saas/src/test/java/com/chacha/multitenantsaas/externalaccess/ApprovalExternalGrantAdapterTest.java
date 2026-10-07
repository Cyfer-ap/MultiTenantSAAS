package com.chacha.multitenantsaas.externalaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.approvals.ApprovalExternalGrantSnapshot;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalExternalGrantAdapterTest {

    @Mock private ExternalAccessGrantRepository grantRepository;
    @Mock private ExternalAccessGrantCapabilityRepository capabilityRepository;

    private ApprovalExternalGrantAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ApprovalExternalGrantAdapter(grantRepository, capabilityRepository);
    }

    @Test
    void requiresExactTenantProjectAndGrantScope() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID grantId = UUID.randomUUID();

        when(grantRepository.findByTenantIdAndProjectIdAndId(tenantId, projectId, grantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.requireApprovalReviewGrant(tenantId, projectId, grantId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("External access grant not found");
    }

    @Test
    void rejectsRevokedGrantEvenWhenApprovalCapabilityWasGranted() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ExternalAccessGrant grant =
                new ExternalAccessGrant(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        "Client",
                        "client@example.com",
                        "hash",
                        Instant.now().plusSeconds(3600));
        grant.revoke(UUID.randomUUID(), Instant.now());

        when(grantRepository.findByTenantIdAndProjectIdAndId(tenantId, projectId, grant.getId()))
                .thenReturn(Optional.of(grant));

        assertThatThrownBy(
                        () ->
                                adapter.requireApprovalReviewGrant(
                                        tenantId, projectId, grant.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External access grant is no longer active");
    }

    @Test
    void rejectsGrantWithoutExplicitApprovalReviewCapability() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ExternalAccessGrant grant =
                new ExternalAccessGrant(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        "Client",
                        "client@example.com",
                        "hash",
                        Instant.now().plusSeconds(3600));

        when(grantRepository.findByTenantIdAndProjectIdAndId(tenantId, projectId, grant.getId()))
                .thenReturn(Optional.of(grant));
        when(capabilityRepository.findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
                        tenantId, projectId, grant.getId()))
                .thenReturn(
                        List.of(
                                new ExternalAccessGrantCapability(
                                        tenantId,
                                        projectId,
                                        grant.getId(),
                                        ExternalAccessCapability.TASK_READ)));

        assertThatThrownBy(
                        () ->
                                adapter.requireApprovalReviewGrant(
                                        tenantId, projectId, grant.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("External access grant does not allow approval review");
    }

    @Test
    void returnsBoundedGuestIdentityForActiveApprovalGrant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ExternalAccessGrant grant =
                new ExternalAccessGrant(
                        tenantId,
                        projectId,
                        UUID.randomUUID(),
                        "Client Reviewer",
                        "client@example.com",
                        "hash",
                        Instant.now().plusSeconds(3600));

        when(grantRepository.findByTenantIdAndProjectIdAndId(tenantId, projectId, grant.getId()))
                .thenReturn(Optional.of(grant));
        when(capabilityRepository.findByTenantIdAndProjectIdAndGrantIdOrderByCapabilityAsc(
                        tenantId, projectId, grant.getId()))
                .thenReturn(
                        List.of(
                                new ExternalAccessGrantCapability(
                                        tenantId,
                                        projectId,
                                        grant.getId(),
                                        ExternalAccessCapability.APPROVAL_REVIEW)));

        ApprovalExternalGrantSnapshot snapshot =
                adapter.requireApprovalReviewGrant(tenantId, projectId, grant.getId());

        assertThat(snapshot.grantId()).isEqualTo(grant.getId());
        assertThat(snapshot.guestName()).isEqualTo("Client Reviewer");
        assertThat(snapshot.guestEmail()).isEqualTo("client@example.com");
    }
}
