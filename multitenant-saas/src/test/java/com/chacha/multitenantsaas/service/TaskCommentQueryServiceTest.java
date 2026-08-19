package com.chacha.multitenantsaas.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.exception.ResourceNotFoundException;
import com.chacha.multitenantsaas.repository.TaskCommentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCommentQueryServiceTest {

    @Mock private TaskCommentRepository taskCommentRepository;

    @Test
    void scopesDeepLinkLookupByTenantProjectTaskAndComment() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        TaskCommentQueryService service = new TaskCommentQueryService(taskCommentRepository);

        when(taskCommentRepository.findByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, commentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getComment(tenantId, projectId, taskId, commentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task comment not found with id: " + commentId);

        verify(taskCommentRepository)
                .findByTenant_IdAndProject_IdAndTask_IdAndId(
                        tenantId, projectId, taskId, commentId);
    }
}
