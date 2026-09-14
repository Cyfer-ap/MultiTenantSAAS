package com.chacha.multitenantsaas.workflows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.PageResponse;
import com.chacha.multitenantsaas.service.CurrentActorService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceExecutionHistoryTest {

    @Mock private WorkflowDefinitionRepository definitionRepository;
    @Mock private WorkflowNodeRepository nodeRepository;
    @Mock private WorkflowEdgeRepository edgeRepository;
    @Mock private WorkflowExecutionRepository executionRepository;
    @Mock private WorkflowGraphValidator graphValidator;
    @Mock private CurrentActorService currentActorService;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService =
                new WorkflowService(
                        definitionRepository,
                        nodeRepository,
                        edgeRepository,
                        executionRepository,
                        graphValidator,
                        currentActorService,
                        new ObjectMapper());
    }

    @Test
    void returnsTenantScopedExecutionHistoryNewestFirst() {
        UUID tenantId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        WorkflowExecution execution =
                new WorkflowExecution(
                        tenantId,
                        workflowId,
                        3,
                        "event-1",
                        WorkflowOperation.TRIGGER_TASK_CREATED,
                        "TASK",
                        taskId);
        execution.succeed("Applied ACTION_SET_TASK_STATUS");
        PageRequest request = PageRequest.of(0, 25);

        when(executionRepository.findByTenantIdOrderByStartedAtDesc(tenantId, request))
                .thenReturn(new PageImpl<>(List.of(execution), request, 1));

        PageResponse<WorkflowDtos.ExecutionResponse> result =
                workflowService.executionHistory(tenantId, request);

        assertThat(result.content()).hasSize(1);
        WorkflowDtos.ExecutionResponse item = result.content().getFirst();
        assertThat(item.workflowId()).isEqualTo(workflowId);
        assertThat(item.workflowVersion()).isEqualTo(3);
        assertThat(item.triggerOperation()).isEqualTo(WorkflowOperation.TRIGGER_TASK_CREATED);
        assertThat(item.sourceEntityType()).isEqualTo("TASK");
        assertThat(item.sourceEntityId()).isEqualTo(taskId);
        assertThat(item.status()).isEqualTo(WorkflowExecutionStatus.SUCCEEDED);
        assertThat(item.explanation()).contains("ACTION_SET_TASK_STATUS");
        verify(executionRepository).findByTenantIdOrderByStartedAtDesc(tenantId, request);
    }
}
