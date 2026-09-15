package com.chacha.multitenantsaas.whiteboards;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.util.HashMap;
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
public class WhiteboardService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WhiteboardRepository whiteboardRepository;
    private final WhiteboardNodeRepository nodeRepository;
    private final WhiteboardEdgeRepository edgeRepository;
    private final ProjectRepository projectRepository;
    private final CurrentActorService currentActorService;
    private final TaskCreationPort taskCreationPort;
    private final WhiteboardDocumentValidator documentValidator;

    public WhiteboardService(
            WhiteboardRepository whiteboardRepository,
            WhiteboardNodeRepository nodeRepository,
            WhiteboardEdgeRepository edgeRepository,
            ProjectRepository projectRepository,
            CurrentActorService currentActorService,
            TaskCreationPort taskCreationPort,
            WhiteboardDocumentValidator documentValidator) {
        this.whiteboardRepository = whiteboardRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.projectRepository = projectRepository;
        this.currentActorService = currentActorService;
        this.taskCreationPort = taskCreationPort;
        this.documentValidator = documentValidator;
    }

    @Transactional(readOnly = true)
    public PageResponse<WhiteboardDtos.SummaryResponse> list(
            UUID tenantId, UUID projectId, Pageable pageable) {
        Page<Whiteboard> page =
                whiteboardRepository.findByTenantIdAndProjectIdOrderByNameAsc(
                        tenantId, projectId, bounded(pageable));
        List<WhiteboardDtos.SummaryResponse> content =
                page.getContent().stream().map(this::mapSummary).toList();
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
    public WhiteboardDtos.Response get(UUID tenantId, UUID projectId, UUID boardId) {
        return map(requireScoped(tenantId, projectId, boardId));
    }

    @Transactional
    public WhiteboardDtos.Response create(
            UUID tenantId,
            UUID projectId,
            WhiteboardDtos.CreateRequest request,
            Jwt jwt) {
        requireProject(tenantId, projectId);
        documentValidator.validate(request.nodes(), request.edges());
        String normalizedName = normalizeName(request.name());
        if (whiteboardRepository.existsByTenantIdAndProjectIdAndNormalizedName(
                tenantId, projectId, normalizedName)) {
            throw new IllegalArgumentException("A whiteboard with this name already exists");
        }

        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        Whiteboard board =
                whiteboardRepository.saveAndFlush(
                        new Whiteboard(
                                tenantId,
                                projectId,
                                actorUserId,
                                request.name().trim(),
                                normalizedName));
        replaceDocument(board, request.nodes(), request.edges());
        return map(board);
    }

    @Transactional
    public WhiteboardDtos.Response update(
            UUID tenantId,
            UUID projectId,
            UUID boardId,
            WhiteboardDtos.UpdateRequest request) {
        Whiteboard board = requireScoped(tenantId, projectId, boardId);
        assertVersion(board, request.expectedVersion());
        documentValidator.validate(request.nodes(), request.edges());
        String normalizedName = normalizeName(request.name());
        if (whiteboardRepository.existsByTenantIdAndProjectIdAndNormalizedNameAndIdNot(
                tenantId, projectId, normalizedName, boardId)) {
            throw new IllegalArgumentException("A whiteboard with this name already exists");
        }

        Map<String, UUID> linkedTasks = currentLinkedTasks(tenantId, projectId, boardId);
        for (WhiteboardDtos.NodeRequest node : request.nodes()) {
            if (linkedTasks.containsKey(node.key()) && node.type() == WhiteboardNodeType.SHAPE) {
                throw new IllegalArgumentException(
                        "A task-linked whiteboard node cannot be converted into a shape");
            }
        }

        board.updateDocument(request.name().trim(), normalizedName);
        whiteboardRepository.saveAndFlush(board);
        replaceDocument(board, request.nodes(), request.edges(), linkedTasks);
        return map(board);
    }

    @Transactional
    public void delete(
            UUID tenantId, UUID projectId, UUID boardId, long expectedVersion) {
        Whiteboard board = requireScoped(tenantId, projectId, boardId);
        assertVersion(board, expectedVersion);
        whiteboardRepository.delete(board);
        whiteboardRepository.flush();
    }

    @Transactional
    public WhiteboardDtos.ConvertToTaskResponse convertNodeToTask(
            UUID tenantId,
            UUID projectId,
            UUID boardId,
            String nodeKey,
            WhiteboardDtos.ConvertToTaskRequest request,
            Jwt jwt) {
        Whiteboard board = requireScoped(tenantId, projectId, boardId);
        assertVersion(board, request.expectedVersion());
        WhiteboardNode node =
                nodeRepository
                        .findByTenantIdAndProjectIdAndBoardIdAndNodeKey(
                                tenantId, projectId, boardId, nodeKey)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Whiteboard node not found: " + nodeKey));
        if (node.getNodeType() == WhiteboardNodeType.SHAPE) {
            throw new IllegalArgumentException("Shape nodes cannot be converted to tasks");
        }
        if (node.getLinkedTaskId() != null) {
            throw new IllegalStateException("Whiteboard node is already linked to a task");
        }

        UUID actorUserId = currentActorService.getRequiredActiveActor(tenantId, jwt).getId();
        String description = normalizeDescription(request.description());
        if (description == null) {
            description = normalizeDescription(node.getContent());
        }
        TaskCreationResult created =
                taskCreationPort.createTask(
                        new TaskCreationCommand(
                                tenantId,
                                projectId,
                                actorUserId,
                                request.assigneeUserId(),
                                request.title().trim(),
                                description,
                                request.priority(),
                                request.dueAt(),
                                "Task created from whiteboard '" + board.getName() + "'"));

        node.linkTask(created.taskId());
        nodeRepository.saveAndFlush(node);
        board.touch();
        whiteboardRepository.saveAndFlush(board);

        return new WhiteboardDtos.ConvertToTaskResponse(
                boardId, nodeKey, created.taskId(), board.getVersion(), created.createdAt());
    }

    private void replaceDocument(
            Whiteboard board,
            List<WhiteboardDtos.NodeRequest> nodeRequests,
            List<WhiteboardDtos.EdgeRequest> edgeRequests) {
        replaceDocument(board, nodeRequests, edgeRequests, Map.of());
    }

    private void replaceDocument(
            Whiteboard board,
            List<WhiteboardDtos.NodeRequest> nodeRequests,
            List<WhiteboardDtos.EdgeRequest> edgeRequests,
            Map<String, UUID> linkedTasks) {
        edgeRepository.deleteByTenantIdAndProjectIdAndBoardId(
                board.getTenantId(), board.getProjectId(), board.getId());
        edgeRepository.flush();
        nodeRepository.deleteByTenantIdAndProjectIdAndBoardId(
                board.getTenantId(), board.getProjectId(), board.getId());
        nodeRepository.flush();

        List<WhiteboardNode> nodes =
                nodeRequests.stream()
                        .map(
                                request ->
                                        new WhiteboardNode(
                                                board.getTenantId(),
                                                board.getProjectId(),
                                                board.getId(),
                                                request.key(),
                                                request.type(),
                                                normalizeContent(request.content()),
                                                linkedTasks.get(request.key()),
                                                request.x(),
                                                request.y(),
                                                request.width(),
                                                request.height(),
                                                request.zIndex()))
                        .toList();
        nodeRepository.saveAllAndFlush(nodes);

        List<WhiteboardEdge> edges =
                edgeRequests.stream()
                        .map(
                                request ->
                                        new WhiteboardEdge(
                                                board.getTenantId(),
                                                board.getProjectId(),
                                                board.getId(),
                                                request.sourceKey(),
                                                request.targetKey(),
                                                normalizeLabel(request.label())))
                        .toList();
        edgeRepository.saveAllAndFlush(edges);
    }

    private Map<String, UUID> currentLinkedTasks(UUID tenantId, UUID projectId, UUID boardId) {
        Map<String, UUID> result = new HashMap<>();
        for (WhiteboardNode node :
                nodeRepository.findByTenantIdAndProjectIdAndBoardIdOrderByZIndexAscNodeKeyAsc(
                        tenantId, projectId, boardId)) {
            if (node.getLinkedTaskId() != null) {
                result.put(node.getNodeKey(), node.getLinkedTaskId());
            }
        }
        return result;
    }

    private WhiteboardDtos.Response map(Whiteboard board) {
        List<WhiteboardDtos.NodeResponse> nodes =
                nodeRepository
                        .findByTenantIdAndProjectIdAndBoardIdOrderByZIndexAscNodeKeyAsc(
                                board.getTenantId(), board.getProjectId(), board.getId())
                        .stream()
                        .map(
                                node ->
                                        new WhiteboardDtos.NodeResponse(
                                                node.getId(),
                                                node.getNodeKey(),
                                                node.getNodeType(),
                                                node.getContent(),
                                                node.getLinkedTaskId(),
                                                node.getPositionX(),
                                                node.getPositionY(),
                                                node.getWidth(),
                                                node.getHeight(),
                                                node.getZIndex()))
                        .toList();
        List<WhiteboardDtos.EdgeResponse> edges =
                edgeRepository
                        .findByTenantIdAndProjectIdAndBoardIdOrderBySourceNodeKeyAscTargetNodeKeyAsc(
                                board.getTenantId(), board.getProjectId(), board.getId())
                        .stream()
                        .map(
                                edge ->
                                        new WhiteboardDtos.EdgeResponse(
                                                edge.getId(),
                                                edge.getSourceNodeKey(),
                                                edge.getTargetNodeKey(),
                                                edge.getLabel()))
                        .toList();
        return new WhiteboardDtos.Response(
                board.getId(),
                board.getTenantId(),
                board.getProjectId(),
                board.getCreatedByUserId(),
                board.getName(),
                board.getVersion(),
                nodes,
                edges,
                board.getCreatedAt(),
                board.getUpdatedAt());
    }

    private WhiteboardDtos.SummaryResponse mapSummary(Whiteboard board) {
        return new WhiteboardDtos.SummaryResponse(
                board.getId(),
                board.getTenantId(),
                board.getProjectId(),
                board.getCreatedByUserId(),
                board.getName(),
                board.getVersion(),
                board.getCreatedAt(),
                board.getUpdatedAt());
    }

    private Whiteboard requireScoped(UUID tenantId, UUID projectId, UUID boardId) {
        return whiteboardRepository
                .findByTenantIdAndProjectIdAndId(tenantId, projectId, boardId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Whiteboard not found: " + boardId));
    }

    private void requireProject(UUID tenantId, UUID projectId) {
        if (projectRepository.findByTenant_IdAndId(tenantId, projectId).isEmpty()) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
    }

    private void assertVersion(Whiteboard board, long expectedVersion) {
        if (board.getVersion() != expectedVersion) {
            throw new WhiteboardVersionConflictException(expectedVersion, board.getVersion());
        }
    }

    private Pageable bounded(Pageable pageable) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size);
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeContent(String value) {
        return value == null ? null : value;
    }

    private String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
