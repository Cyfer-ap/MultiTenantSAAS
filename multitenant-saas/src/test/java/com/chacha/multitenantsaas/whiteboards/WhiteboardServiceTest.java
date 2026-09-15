package com.chacha.multitenantsaas.whiteboards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import com.chacha.multitenantsaas.projects.access.ProjectAccessPort;
import com.chacha.multitenantsaas.projects.access.ProjectAccessSnapshot;
import com.chacha.multitenantsaas.service.CurrentActorService;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationCommand;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationPort;
import com.chacha.multitenantsaas.tasks.creation.TaskCreationResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

class WhiteboardServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID boardId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private WhiteboardRepository whiteboardRepository;
    private WhiteboardNodeRepository nodeRepository;
    private WhiteboardEdgeRepository edgeRepository;
    private ProjectAccessPort projectAccessPort;
    private CurrentActorService currentActorService;
    private TaskCreationPort taskCreationPort;
    private WhiteboardService service;

    @BeforeEach
    void setUp() {
        whiteboardRepository = mock(WhiteboardRepository.class);
        nodeRepository = mock(WhiteboardNodeRepository.class);
        edgeRepository = mock(WhiteboardEdgeRepository.class);
        projectAccessPort = mock(ProjectAccessPort.class);
        currentActorService = mock(CurrentActorService.class);
        taskCreationPort = mock(TaskCreationPort.class);
        service =
                new WhiteboardService(
                        whiteboardRepository,
                        nodeRepository,
                        edgeRepository,
                        projectAccessPort,
                        currentActorService,
                        taskCreationPort,
                        new WhiteboardDocumentValidator());
    }

    @Test
    void convertsStickyThroughTaskCreationPortAndLinksResult() {
        Whiteboard board = boardWithVersion(3L);
        WhiteboardNode node =
                new WhiteboardNode(
                        tenantId,
                        projectId,
                        boardId,
                        "idea-1",
                        WhiteboardNodeType.STICKY,
                        "Original sticky context",
                        null,
                        10,
                        20,
                        220,
                        140,
                        0);
        Jwt jwt = mock(Jwt.class);
        AppUser actor = mock(AppUser.class);
        UUID taskId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-16T00:00:00Z");

        when(projectAccessPort.requireProject(tenantId, projectId))
                .thenReturn(new ProjectAccessSnapshot(projectId, ProjectStatus.ACTIVE));
        when(whiteboardRepository.findByTenantIdAndProjectIdAndId(tenantId, projectId, boardId))
                .thenReturn(Optional.of(board));
        when(nodeRepository.findByTenantIdAndProjectIdAndBoardIdAndNodeKey(
                        tenantId, projectId, boardId, "idea-1"))
                .thenReturn(Optional.of(node));
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(actor.getId()).thenReturn(actorId);
        when(taskCreationPort.createTask(any(TaskCreationCommand.class)))
                .thenReturn(new TaskCreationResult(taskId, createdAt));
        when(nodeRepository.saveAndFlush(node)).thenReturn(node);
        when(whiteboardRepository.saveAndFlush(board))
                .thenAnswer(
                        invocation -> {
                            ReflectionTestUtils.setField(board, "version", 4L);
                            return board;
                        });

        var response =
                service.convertNodeToTask(
                        tenantId,
                        projectId,
                        boardId,
                        "idea-1",
                        new WhiteboardDtos.ConvertToTaskRequest(
                                3L,
                                "Create API retry task",
                                null,
                                null,
                                ProjectTaskPriority.HIGH,
                                null),
                        jwt);

        ArgumentCaptor<TaskCreationCommand> commandCaptor =
                ArgumentCaptor.forClass(TaskCreationCommand.class);
        verify(taskCreationPort).createTask(commandCaptor.capture());
        TaskCreationCommand command = commandCaptor.getValue();
        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.projectId()).isEqualTo(projectId);
        assertThat(command.creatorUserId()).isEqualTo(actorId);
        assertThat(command.title()).isEqualTo("Create API retry task");
        assertThat(command.description()).isEqualTo("Original sticky context");
        assertThat(command.priority()).isEqualTo(ProjectTaskPriority.HIGH);
        assertThat(node.getLinkedTaskId()).isEqualTo(taskId);
        assertThat(response.taskId()).isEqualTo(taskId);
        assertThat(response.boardVersion()).isEqualTo(4L);
    }

    @Test
    void staleVersionStopsConversionBeforeTaskCreation() {
        Whiteboard board = boardWithVersion(5L);
        when(projectAccessPort.requireProject(tenantId, projectId))
                .thenReturn(new ProjectAccessSnapshot(projectId, ProjectStatus.ACTIVE));
        when(whiteboardRepository.findByTenantIdAndProjectIdAndId(tenantId, projectId, boardId))
                .thenReturn(Optional.of(board));

        assertThatThrownBy(
                        () ->
                                service.convertNodeToTask(
                                        tenantId,
                                        projectId,
                                        boardId,
                                        "idea-1",
                                        new WhiteboardDtos.ConvertToTaskRequest(
                                                4L,
                                                "Task",
                                                null,
                                                null,
                                                ProjectTaskPriority.MEDIUM,
                                                null),
                                        mock(Jwt.class)))
                .isInstanceOf(WhiteboardVersionConflictException.class)
                .hasMessageContaining("expected 4")
                .hasMessageContaining("current version is 5");

        verify(taskCreationPort, never()).createTask(any(TaskCreationCommand.class));
    }

    @Test
    void archivedProjectRejectsMutationBeforeBoardLookup() {
        when(projectAccessPort.requireProject(tenantId, projectId))
                .thenReturn(new ProjectAccessSnapshot(projectId, ProjectStatus.ARCHIVED));

        assertThatThrownBy(
                        () ->
                                service.delete(tenantId, projectId, boardId, 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived projects cannot be modified");

        verify(whiteboardRepository, never())
                .findByTenantIdAndProjectIdAndId(tenantId, projectId, boardId);
    }

    private Whiteboard boardWithVersion(long version) {
        Whiteboard board = new Whiteboard(tenantId, projectId, actorId, "Planning", "planning");
        ReflectionTestUtils.setField(board, "id", boardId);
        ReflectionTestUtils.setField(board, "version", version);
        return board;
    }
}
