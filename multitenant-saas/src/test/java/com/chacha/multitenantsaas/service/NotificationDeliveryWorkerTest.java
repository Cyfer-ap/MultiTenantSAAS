package com.chacha.multitenantsaas.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;
import com.chacha.multitenantsaas.entity.NotificationType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryWorkerTest {

    @Mock private NotificationDeliveryService deliveryService;
    @Mock private NotificationDeliveryHandler handler;

    private NotificationDeliveryWorker worker;
    private NotificationDeliveryTask task;

    @BeforeEach
    void setUp() {
        when(handler.channel()).thenReturn(NotificationDeliveryChannel.EMAIL);
        worker = new NotificationDeliveryWorker(deliveryService, List.of(handler));
        task =
                new NotificationDeliveryTask(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "recipient@example.test",
                        NotificationDeliveryChannel.EMAIL,
                        NotificationType.TASK_ASSIGNED,
                        "Task assigned",
                        "A task was assigned to you.",
                        "/projects/project-1?task=task-1");
    }

    @Test
    void marksSuccessfulHandlerDeliveryAsSent() {
        when(deliveryService.claimBatch(any())).thenReturn(List.of(task));

        worker.processBatch();

        verify(handler).deliver(task);
        verify(deliveryService).markSent(eq(task.deliveryId()), eq(task.leaseToken()), any());
        verify(deliveryService, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    void recordsHandlerFailureWithoutEscapingTheWorker() {
        when(deliveryService.claimBatch(any())).thenReturn(List.of(task));
        org.mockito.Mockito.doThrow(new IllegalStateException("provider unavailable"))
                .when(handler)
                .deliver(task);

        worker.processBatch();

        verify(deliveryService)
                .markFailed(
                        eq(task.deliveryId()),
                        eq(task.leaseToken()),
                        any(),
                        eq("provider unavailable"));
        verify(deliveryService, never()).markSent(any(), any(), any());
    }
}
