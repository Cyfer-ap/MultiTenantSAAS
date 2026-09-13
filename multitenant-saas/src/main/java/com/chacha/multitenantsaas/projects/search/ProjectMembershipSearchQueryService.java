package com.chacha.multitenantsaas.projects.search;

import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMembershipSearchQueryService {

    private static final int MAX_PROJECT_MEMBERSHIPS = 1000;

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectMembershipSearchQueryService(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public Set<UUID> findProjectIdsForUser(UUID tenantId, UUID userId) {
        return new LinkedHashSet<>(
                projectMemberRepository
                        .findProjectIdsByTenantAndUser(
                                tenantId, userId, PageRequest.of(0, MAX_PROJECT_MEMBERSHIPS))
                        .getContent());
    }
}
