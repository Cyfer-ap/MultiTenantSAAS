package com.chacha.multitenantsaas.workflows;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final TypeReference<Map<String, String>> CONFIGURATION_TYPE =
            new TypeReference<>() {};

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowGraphValidator graphValidator;
    private final CurrentActorService currentActorService;
    private final ObjectMapper objectMapper;

    public WorkflowService(
            WorkflowDefinitionRepository definitionRepository,
            WorkflowNodeRepository nodeRepository,
            WorkflowEdgeRepository edgeRepository,
            WorkflowGraphValidator graphValidator,
            CurrentActorService currentActorService,
            ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.graphValidator = graphValidator;
        this.currentActorService = currentActorService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkflowDtos.Response> list(UUID tenantId, Pageable pageable) {
        Page<WorkflowDefinition> page =
                definitionRepository.findByTenantIdOrderByNameAsc(tenantId, bounded(pageable));
        List<WorkflowDtos.Response> content = page.getContent().stream().map(this::map).toList();
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
    public WorkflowDtos.Response get(UUID tenantId, UUID workflowId) {
        return map(requireScoped(tenantId, workflowId));
    }

    @Transactional
    public WorkflowDtos.Response create(
            UUID tenantId, WorkflowDtos.UpsertRequest request, Jwt jwt) {
        graphValidator.validate(request.nodes(), request.edges());
        String normalizedName = normalizeName(request.name());
        if (definitionRepository.existsByTenantIdAndNormalizedName(tenantId, normalizedName)) {
            throw new IllegalArgumentException("A workflow with this name already exists");
        }
        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        WorkflowDefinition definition =
                definitionRepository.saveAndFlush(
                        new WorkflowDefinition(
                                tenantId,
                                actorUserId,
                                request.name().trim(),
                                normalizedName,
                                normalizeDescription(request.description())));
        replaceGraph(definition, request.nodes(), request.edges());
        return map(definition);
    }

    @Transactional
    public WorkflowDtos.Response update(
            UUID tenantId, UUID workflowId, WorkflowDtos.UpsertRequest request) {
        WorkflowDefinition definition = requireScoped(tenantId, workflowId);
        graphValidator.validate(request.nodes(), request.edges());
        String normalizedName = normalizeName(request.name());
        if (definitionRepository.existsByTenantIdAndNormalizedNameAndIdNot(
                tenantId, normalizedName, workflowId)) {
            throw new IllegalArgumentException("A workflow with this name already exists");
        }
        definition.updateDefinition(
                request.name().trim(),
                normalizedName,
                normalizeDescription(request.description()));
        definitionRepository.saveAndFlush(definition);
        replaceGraph(definition, request.nodes(), request.edges());
        return map(definition);
    }

    @Transactional
    public WorkflowDtos.Response activate(UUID tenantId, UUID workflowId) {
        WorkflowDefinition definition = requireScoped(tenantId, workflowId);
        validateStoredGraph(definition);
        definition.activate();
        return map(definitionRepository.save(definition));
    }

    @Transactional
    public WorkflowDtos.Response pause(UUID tenantId, UUID workflowId) {
        WorkflowDefinition definition = requireScoped(tenantId, workflowId);
        definition.pause();
        return map(definitionRepository.save(definition));
    }

    private void validateStoredGraph(WorkflowDefinition definition) {
        List<WorkflowDtos.NodeRequest> nodes =
                nodeRepository
                        .findByTenantIdAndWorkflowIdOrderByNodeKeyAsc(
                                definition.getTenantId(), definition.getId())
                        .stream()
                        .map(
                                node ->
                                        new WorkflowDtos.NodeRequest(
                                                node.getNodeKey(),
                                                node.getNodeType(),
                                                node.getOperation(),
                                                parseConfiguration(node.getConfigurationJson()),
                                                node.getPositionX(),
                                                node.getPositionY()))
                        .toList();
        List<WorkflowDtos.EdgeRequest> edges =
                edgeRepository
                        .findByTenantIdAndWorkflowIdOrderBySourceNodeKeyAscBranchTypeAsc(
                                definition.getTenantId(), definition.getId())
                        .stream()
                        .map(
                                edge ->
                                        new WorkflowDtos.EdgeRequest(
                                                edge.getSourceNodeKey(),
                                                edge.getTargetNodeKey(),
                                                edge.getBranchType()))
                        .toList();
        graphValidator.validate(nodes, edges);
    }

    private void replaceGraph(
            WorkflowDefinition definition,
            List<WorkflowDtos.NodeRequest> nodeRequests,
            List<WorkflowDtos.EdgeRequest> edgeRequests) {
        edgeRepository.deleteByTenantIdAndWorkflowId(definition.getTenantId(), definition.getId());
        edgeRepository.flush();
        nodeRepository.deleteByTenantIdAndWorkflowId(definition.getTenantId(), definition.getId());
        nodeRepository.flush();

        List<WorkflowNode> nodes =
                nodeRequests.stream()
                        .map(
                                request ->
                                        new WorkflowNode(
                                                definition.getTenantId(),
                                                definition.getId(),
                                                request.key(),
                                                request.type(),
                                                request.operation(),
                                                serializeConfiguration(
                                                        graphValidator.normalizeConfiguration(
                                                                request.operation(),
                                                                request.configuration())),
                                                request.x(),
                                                request.y()))
                        .toList();
        nodeRepository.saveAllAndFlush(nodes);

        List<WorkflowEdge> edges =
                edgeRequests.stream()
                        .map(
                                request ->
                                        new WorkflowEdge(
                                                definition.getTenantId(),
                                                definition.getId(),
                                                request.sourceKey(),
                                                request.targetKey(),
                                                request.branch()))
                        .toList();
        edgeRepository.saveAll(edges);
    }

    private WorkflowDtos.Response map(WorkflowDefinition definition) {
        List<WorkflowDtos.NodeResponse> nodes =
                nodeRepository
                        .findByTenantIdAndWorkflowIdOrderByNodeKeyAsc(
                                definition.getTenantId(), definition.getId())
                        .stream()
                        .map(
                                node ->
                                        new WorkflowDtos.NodeResponse(
                                                node.getId(),
                                                node.getNodeKey(),
                                                node.getNodeType(),
                                                node.getOperation(),
                                                parseConfiguration(node.getConfigurationJson()),
                                                node.getPositionX(),
                                                node.getPositionY()))
                        .toList();
        List<WorkflowDtos.EdgeResponse> edges =
                edgeRepository
                        .findByTenantIdAndWorkflowIdOrderBySourceNodeKeyAscBranchTypeAsc(
                                definition.getTenantId(), definition.getId())
                        .stream()
                        .map(
                                edge ->
                                        new WorkflowDtos.EdgeResponse(
                                                edge.getId(),
                                                edge.getSourceNodeKey(),
                                                edge.getTargetNodeKey(),
                                                edge.getBranchType()))
                        .toList();
        return new WorkflowDtos.Response(
                definition.getId(),
                definition.getTenantId(),
                definition.getCreatedByUserId(),
                definition.getName(),
                definition.getDescription(),
                definition.getStatus(),
                definition.getDefinitionVersion(),
                nodes,
                edges,
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }

    private WorkflowDefinition requireScoped(UUID tenantId, UUID workflowId) {
        return definitionRepository
                .findByTenantIdAndId(tenantId, workflowId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Workflow not found: " + workflowId));
    }

    private Pageable bounded(Pageable pageable) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size);
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String serializeConfiguration(Map<String, String> configuration) {
        try {
            return objectMapper.writeValueAsString(configuration);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize workflow configuration", exception);
        }
    }

    private Map<String, String> parseConfiguration(String configurationJson) {
        if (configurationJson == null || configurationJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configurationJson, CONFIGURATION_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored workflow configuration is invalid", exception);
        }
    }
}
