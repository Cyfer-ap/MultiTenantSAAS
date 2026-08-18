package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.entity.TaskAttachment;
import com.chacha.multitenantsaas.entity.TaskAttachmentStatus;
import com.chacha.multitenantsaas.repository.TaskAttachmentRepository;
import com.chacha.multitenantsaas.storage.ObjectStorageService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "r2")
public class TaskAttachmentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TaskAttachmentCleanupService.class);
    private static final Duration PENDING_MAX_AGE = Duration.ofHours(1);

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final ObjectStorageService objectStorageService;

    public TaskAttachmentCleanupService(
            TaskAttachmentRepository taskAttachmentRepository,
            ObjectStorageService objectStorageService) {
        this.taskAttachmentRepository = taskAttachmentRepository;
        this.objectStorageService = objectStorageService;
    }

    @Scheduled(
            initialDelayString = "${app.storage.cleanup.initial-delay-ms:60000}",
            fixedDelayString = "${app.storage.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanup() {
        cleanupStalePendingAttachments();
        retryDeferredStorageDeletes();
    }

    private void cleanupStalePendingAttachments() {
        Instant cutoff = Instant.now().minus(PENDING_MAX_AGE);
        List<TaskAttachment> stale =
                taskAttachmentRepository.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        TaskAttachmentStatus.PENDING, cutoff);

        for (TaskAttachment attachment : stale) {
            try {
                objectStorageService.deleteObject(attachment.getObjectKey());
                attachment.markDeleted();
                attachment.markStorageDeleted();
                taskAttachmentRepository.save(attachment);
            } catch (RuntimeException exception) {
                log.warn(
                        "Could not clean stale pending attachment {}. It will be retried.",
                        attachment.getId(),
                        exception);
            }
        }
    }

    private void retryDeferredStorageDeletes() {
        List<TaskAttachment> deferred =
                taskAttachmentRepository
                        .findTop100ByStatusAndStorageDeletedAtIsNullOrderByDeletedAtAsc(
                                TaskAttachmentStatus.DELETED);

        for (TaskAttachment attachment : deferred) {
            try {
                objectStorageService.deleteObject(attachment.getObjectKey());
                attachment.markStorageDeleted();
                taskAttachmentRepository.save(attachment);
            } catch (RuntimeException exception) {
                log.warn(
                        "Could not finish storage cleanup for attachment {}. It will be retried.",
                        attachment.getId(),
                        exception);
            }
        }
    }
}
