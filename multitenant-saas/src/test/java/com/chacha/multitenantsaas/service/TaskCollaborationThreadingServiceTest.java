package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.TaskCommentCreateRequest;
import com.chacha.multitenantsaas.dto.TaskCommentResponse;
import com.chacha.multitenantsaas.entity.AppUser;
import com.chacha.multitenantsaas.entity.Project;
import com.chacha.multitenantsaas.entity.ProjectStatus;
import com.chacha.multitenantsaas.entity.ProjectTask;
import com.chacha.multitenantsaas.entity.ProjectTaskStatus;
import com.chacha.multitenantsaas.entity.TaskActivityType;
import com.chacha.multitenantsaas.entity.TaskComment;
import com.chacha.multitenantsaas.entity.Tenant;
import com.chacha.multitenantsaas.repository.AppUserRepository;
import com.chacha.multitenantsaas.repository.ProjectMemberRepository;
import com.chacha.multitenantsaas.repository.ProjectRepository;
import com.chacha.multitenantsaas.repository.ProjectTaskRepository;
import com.chacha.multitenantsaas.repository.TaskCommentRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class TaskCollaborationThreadingServiceTest {

    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectTaskRepository projectTaskRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentActorService currentActorService;
    @Mock private TaskActivityService taskActivityService;
    @Mock private AuditLogService auditLogService;
    @Mock private TaskAttachmentService taskAttachmentService;
    @Mock private Jwt jwt;
    @Mock private Project project;
    @Mock private ProjectTask task;
    @Mock private Tenant tenant;
    @Mock private AppUser actor;
    @Mock private AppUser parentAuthor;
    @Mock private TaskComment parent;

    private TaskCollaborationService service;

    @BeforeEach
    void setUp() {
        service =
                new TaskCollaborationService(
                        taskCommentRepository,
                        projectRepository,
                        projectTaskRepository,
                        projectMemberRepository,
                        appUserRepository,
                        currentActorService,
                        taskActivityService,
                        auditLogService,
                        taskAttachmentService);
    }

    @Test
    void createsOneLevelReplyAndIncrementsParentCount() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        stubWritableTask(tenantId, projectId, taskId);
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(taskCommentRepository.findForUpdateByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, parentId))
                .thenReturn(Optional.of(parent));
        when(parent.getId()).thenReturn(parentId);
        when(parent.getAuthorUser()).thenReturn(parentAuthor);
        when(project.getTenant()).thenReturn(tenant);
        when(task.getId()).thenReturn(taskId);
        when(actor.getId()).thenReturn(actorId);
        when(actor.getFullName()).thenReturn("Reply Author");
        when(actor.getEmail()).thenReturn("reply.author@example.test");
        when(taskCommentRepository.save(any(TaskComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskCommentResponse response =
                service.createReply(
                        tenantId,
                        projectId,
                        taskId,
                        parentId,
                        new TaskCommentCreateRequest("  Reply body  ", Set.of()),
                        jwt);

        ArgumentCaptor<TaskComment> savedComment = ArgumentCaptor.forClass(TaskComment.class);
        verify(parent).incrementReplyCount();
        verify(taskCommentRepository, org.mockito.Mockito.times(2)).save(savedComment.capture());
        TaskComment reply = savedComment.getAllValues().get(1);
        assertThat(reply.getParentComment()).isSameAs(parent);
        assertThat(reply.getBody()).isEqualTo("Reply body");
        assertThat(response.parentCommentId()).isEqualTo(parentId);
        verify(taskActivityService)
                .record(task, actor, TaskActivityType.COMMENT_REPLIED, "Replied to a comment");
    }

    @Test
    void rejectsNestedReplies() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        stubWritableTask(tenantId, projectId, taskId);
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(taskCommentRepository.findForUpdateByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, parentId))
                .thenReturn(Optional.of(parent));
        when(parent.getParentComment()).thenReturn(org.mockito.Mockito.mock(TaskComment.class));

        assertThatThrownBy(
                        () ->
                                service.createReply(
                                        tenantId,
                                        projectId,
                                        taskId,
                                        parentId,
                                        new TaskCommentCreateRequest("Nested", Set.of()),
                                        jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Replies cannot contain nested replies");
    }

    @Test
    void enforcesPinnedCommentLimit() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        stubWritableTask(tenantId, projectId, taskId);
        when(currentActorService.getRequiredActiveActor(tenantId, jwt)).thenReturn(actor);
        when(taskCommentRepository.findForUpdateByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, commentId))
                .thenReturn(Optional.of(parent));
        when(taskCommentRepository
                        .countByTenant_IdAndProject_IdAndTask_IdAndParentCommentIsNullAndDeletedFalseAndPinnedAtIsNotNull(
                                tenantId, projectId, taskId))
                .thenReturn(5L);

        assertThatThrownBy(
                        () -> service.pinComment(tenantId, projectId, taskId, commentId, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A task can have at most 5 pinned comments");
        verify(parent, never()).pin(any(AppUser.class));
    }

    private void stubWritableTask(UUID tenantId, UUID projectId, UUID taskId) {
        when(projectRepository.findByTenant_IdAndId(tenantId, projectId))
                .thenReturn(Optional.of(project));
        when(projectTaskRepository.findByProject_Tenant_IdAndProject_IdAndId(
                        tenantId, projectId, taskId))
                .thenReturn(Optional.of(task));
        when(project.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        when(task.getStatus()).thenReturn(ProjectTaskStatus.TODO);
    }
}
