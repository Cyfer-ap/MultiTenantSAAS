package com.chacha.multitenantsaas.approvals;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.DuplicateResourceException;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.projects.access.ProjectAccessPort;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalDefinitionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ApprovalDefinitionRepository definitionRepository;
    private final ApprovalStageRepository stageRepository;
    private final ApprovalStageReviewerRepository reviewerRepository;
    private final ApprovalReviewerEligibilityPort reviewerEligibilityPort;
    private final ProjectAccessPort projectAccessPort;
    private final CurrentActorService currentActorService;

    public ApprovalDefinitionService(
            ApprovalDefinitionRepository definitionRepository,
            ApprovalStageRepository stageRepository,
            ApprovalStageReviewerRepository reviewerRepository,
            ApprovalReviewerEligibilityPort reviewerEligibilityPort,
            ProjectAccessPort projectAccessPort,
            CurrentActorService currentActorService) {
        this.definitionRepository = definitionRepository;
        this.stageRepository = stageRepository;
        this.reviewerRepository = reviewerRepository;
        this.reviewerEligibilityPort = reviewerEligibilityPort;
        this.projectAccessPort = projectAccessPort;
        this.currentActorService = currentActorService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ApprovalDtos.DefinitionSummary> list(
            UUID tenantId, UUID projectId, Pageable pageable) {
        Page<ApprovalDefinition> page =
                definitionRepository.findByTenantIdAndProjectIdOrderByNameAsc(
                        tenantId, projectId, bounded(pageable));
        List<ApprovalDtos.DefinitionSummary> content =
                page.getContent().stream()
                        .map(
                                definition ->
                                        new ApprovalDtos.DefinitionSummary(
                                                definition.getId(),
                                                definition.getName(),
                                                definition.getStatus(),
                                                definition.getDefinitionVersion(),
                                                stageRepository
                                                        .findByTenantIdAndProjectIdAndDefinitionIdOrderByPositionIndexAsc(
                                                                tenantId,
                                                                projectId,
                                                                definition.getId())
                                                        .size(),
                                                definition.getUpdatedAt()))
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

    @Transactional(readOnly = true)
    public ApprovalDtos.DefinitionResponse get(UUID tenantId, UUID projectId, UUID definitionId) {
        return map(require(tenantId, projectId, definitionId));
    }

    @Transactional
    public ApprovalDtos.DefinitionResponse create(
            UUID tenantId, UUID projectId, ApprovalDtos.UpsertRequest request, Jwt jwt) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        validateStages(tenantId, projectId, request.stages());
        String normalizedName = normalizeName(request.name());
        if (definitionRepository.existsByTenantIdAndProjectIdAndNormalizedName(
                tenantId, projectId, normalizedName)) {
            throw duplicateName();
        }
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        ApprovalDefinition definition =
                definitionRepository.saveAndFlush(
                        new ApprovalDefinition(
                                tenantId,
                                projectId,
                                actorUserId,
                                request.name().trim(),
                                normalizedName,
                                normalizeDescription(request.description())));
        replaceStages(definition, request.stages());
        return map(definition);
    }

    @Transactional
    public ApprovalDtos.DefinitionResponse update(
            UUID tenantId,
            UUID projectId,
            UUID definitionId,
            ApprovalDtos.UpsertRequest request) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        ApprovalDefinition definition = require(tenantId, projectId, definitionId);
        if (definition.getStatus() == ApprovalStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Active approval definitions must be paused before editing");
        }
        validateStages(tenantId, projectId, request.stages());
        String normalizedName = normalizeName(request.name());
        if (definitionRepository.existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
                tenantId, projectId, normalizedName, definitionId)) {
            throw duplicateName();
        }
        definition.update(
                request.name().trim(),
                normalizedName,
                normalizeDescription(request.description()));
        definitionRepository.saveAndFlush(definition);
        replaceStages(definition, request.stages());
        return map(definition);
    }

    @Transactional
    public ApprovalDtos.DefinitionResponse activate(
            UUID tenantId, UUID projectId, UUID definitionId) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        ApprovalDefinition definition = require(tenantId, projectId, definitionId);
        List<ApprovalStage> stages = stages(definition);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Approval definition must contain at least one stage");
        }
        for (ApprovalStage stage : stages) {
            List<UUID> reviewerIds = reviewerIds(definition, stage);
            validateReviewerEligibility(tenantId, projectId, reviewerIds);
        }
        definition.activate();
        return map(definitionRepository.saveAndFlush(definition));
    }

    @Transactional
    public ApprovalDtos.DefinitionResponse pause(UUID tenantId, UUID projectId, UUID definitionId) {
        projectAccessPort.requireProject(tenantId, projectId).requireMutable();
        ApprovalDefinition definition = require(tenantId, projectId, definitionId);
        definition.pause();
        return map(definitionRepository.saveAndFlush(definition));
    }

    private void replaceStages(
            ApprovalDefinition definition, List<ApprovalDtos.StageRequest> requests) {
        reviewerRepository.deleteByTenantIdAndProjectIdAndDefinitionId(
                definition.getTenantId(), definition.getProjectId(), definition.getId());
        reviewerRepository.flush();
        stageRepository.deleteByTenantIdAndProjectIdAndDefinitionId(
                definition.getTenantId(), definition.getProjectId(), definition.getId());
        stageRepository.flush();

        for (int index = 0; index < requests.size(); index++) {
            ApprovalDtos.StageRequest request = requests.get(index);
            ApprovalStage stage =
                    stageRepository.saveAndFlush(
                            new ApprovalStage(
                                    definition.getTenantId(),
                                    definition.getProjectId(),
                                    definition.getId(),
                                    request.key().trim(),
                                    request.name().trim(),
                                    index,
                                    request.allowRequesterApproval()));
            List<ApprovalStageReviewer> reviewers =
                    request.reviewerUserIds().stream()
                            .distinct()
                            .map(
                                    userId ->
                                            new ApprovalStageReviewer(
                                                    definition.getTenantId(),
                                                    definition.getProjectId(),
                                                    definition.getId(),
                                                    stage.getId(),
                                                    userId))
                            .toList();
            reviewerRepository.saveAll(reviewers);
        }
    }

    private void validateStages(
            UUID tenantId, UUID projectId, List<ApprovalDtos.StageRequest> requests) {
        Set<String> keys = new HashSet<>();
        for (ApprovalDtos.StageRequest stage : requests) {
            String normalizedKey = stage.key().trim().toLowerCase(Locale.ROOT);
            if (!keys.add(normalizedKey)) {
                throw new IllegalArgumentException("Approval stage keys must be unique");
            }
            if (new HashSet<>(stage.reviewerUserIds()).size() != stage.reviewerUserIds().size()) {
                throw new IllegalArgumentException("Approval stage reviewer IDs must be unique");
            }
            validateReviewerEligibility(tenantId, projectId, stage.reviewerUserIds());
        }
    }

    private void validateReviewerEligibility(
            UUID tenantId, UUID projectId, List<UUID> reviewerIds) {
        for (UUID reviewerId : reviewerIds) {
            if (!reviewerEligibilityPort.isEligible(tenantId, projectId, reviewerId)) {
                throw new IllegalArgumentException(
                        "Reviewer must be an active member of this project: " + reviewerId);
            }
        }
    }

    private ApprovalDtos.DefinitionResponse map(ApprovalDefinition definition) {
        List<ApprovalDtos.StageResponse> stageResponses = new ArrayList<>();
        for (ApprovalStage stage : stages(definition)) {
            stageResponses.add(
                    new ApprovalDtos.StageResponse(
                            stage.getId(),
                            stage.getStageKey(),
                            stage.getName(),
                            stage.getPositionIndex(),
                            stage.isAllowRequesterApproval(),
                            reviewerIds(definition, stage)));
        }
        return new ApprovalDtos.DefinitionResponse(
                definition.getId(),
                definition.getTenantId(),
                definition.getProjectId(),
                definition.getCreatedByUserId(),
                definition.getName(),
                definition.getDescription(),
                definition.getStatus(),
                definition.getDefinitionVersion(),
                stageResponses,
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    private List<ApprovalStage> stages(ApprovalDefinition definition) {
        return stageRepository.findByTenantIdAndProjectIdAndDefinitionIdOrderByPositionIndexAsc(
                definition.getTenantId(), definition.getProjectId(), definition.getId());
    }

    private List<UUID> reviewerIds(ApprovalDefinition definition, ApprovalStage stage) {
        return reviewerRepository
                .findByTenantIdAndProjectIdAndDefinitionIdAndStageIdOrderByReviewerUserIdAsc(
                        definition.getTenantId(),
                        definition.getProjectId(),
                        definition.getId(),
                        stage.getId())
                .stream()
                .map(ApprovalStageReviewer::getReviewerUserId)
                .toList();
    }

    private ApprovalDefinition require(UUID tenantId, UUID projectId, UUID id) {
        return definitionRepository
                .findByTenantIdAndProjectIdAndId(tenantId, projectId, id)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Approval definition not found: " + id));
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(), Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)));
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private DuplicateResourceException duplicateName() {
        return new DuplicateResourceException(
                "An approval definition with this name already exists in the project");
    }
}
