package com.chacha.multitenantsaas.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chacha.multitenantsaas.entity.TaskAttachment;
import com.chacha.multitenantsaas.entity.TaskAttachmentStatus;
import com.chacha.multitenantsaas.repository.TaskAttachmentRepository;
import com.chacha.multitenantsaas.storage.ObjectStorageService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskAttachmentCleanupServiceTest {

    @Mock private TaskAttachmentRepository taskAttachmentRepository;
    @Mock private ObjectStorageService objectStorageService;
    @Mock private TaskAttachment stalePending;
    @Mock private TaskAttachment deferredDelete;

    @Test
    void removesStalePendingAndRetriesDeferredStorageDeletion() {
        when(stalePending.getId()).thenReturn(UUID.randomUUID());
        when(stalePending.getObjectKey()).thenReturn("tenants/t/tasks/task/attachments/pending");
        when(deferredDelete.getId()).thenReturn(UUID.randomUUID());
        when(deferredDelete.getObjectKey()).thenReturn("tenants/t/tasks/task/attachments/deleted");
        when(taskAttachmentRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        any(TaskAttachmentStatus.class), any(Instant.class)))
                .thenReturn(List.of(stalePending));
        when(taskAttachmentRepository.findTop100ByStatusAndStorageDeletedAtIsNullOrderByDeletedAtAsc(
                        TaskAttachmentStatus.DELETED))
                .thenReturn(List.of(deferredDelete));

        TaskAttachmentCleanupService service =
                new TaskAttachmentCleanupService(taskAttachmentRepository, objectStorageService);
        service.cleanup();

        verify(objectStorageService).deleteObject(stalePending.getObjectKey());
        verify(stalePending).markDeleted();
        verify(stalePending).markStorageDeleted();
        verify(taskAttachmentRepository).save(stalePending);

        verify(objectStorageService).deleteObject(deferredDelete.getObjectKey());
        verify(deferredDelete).markStorageDeleted();
        verify(taskAttachmentRepository).save(deferredDelete);
    }
}
