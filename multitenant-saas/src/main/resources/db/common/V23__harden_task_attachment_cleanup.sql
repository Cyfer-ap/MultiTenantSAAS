ALTER TABLE task_attachments
    ADD COLUMN storage_deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_task_attachment_cleanup
    ON task_attachments (status, created_at);
