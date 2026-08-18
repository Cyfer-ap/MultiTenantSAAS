ALTER TABLE task_comments
    ADD CONSTRAINT uk_task_comments_tenant_project_task_id
        UNIQUE (tenant_id, project_id, task_id, id);

CREATE TABLE task_attachments (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    comment_id UUID,
    uploader_user_id UUID NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    expected_size_bytes BIGINT NOT NULL,
    actual_size_bytes BIGINT,
    etag VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_task_attachments PRIMARY KEY (id),
    CONSTRAINT uk_task_attachments_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_task_attachments_object_key UNIQUE (object_key),
    CONSTRAINT fk_task_attachment_task
        FOREIGN KEY (tenant_id, project_id, task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT fk_task_attachment_comment
        FOREIGN KEY (tenant_id, project_id, task_id, comment_id)
            REFERENCES task_comments (tenant_id, project_id, task_id, id),
    CONSTRAINT fk_task_attachment_uploader
        FOREIGN KEY (tenant_id, uploader_user_id)
            REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_task_attachment_expected_size
        CHECK (expected_size_bytes > 0 AND expected_size_bytes <= 26214400),
    CONSTRAINT ck_task_attachment_actual_size
        CHECK (actual_size_bytes IS NULL OR actual_size_bytes >= 0),
    CONSTRAINT ck_task_attachment_status
        CHECK (status IN ('PENDING', 'AVAILABLE', 'DELETED'))
);

CREATE INDEX idx_task_attachment_task_created
    ON task_attachments (tenant_id, project_id, task_id, created_at DESC);

CREATE INDEX idx_task_attachment_comment
    ON task_attachments (tenant_id, project_id, task_id, comment_id);

CREATE INDEX idx_task_attachment_uploader
    ON task_attachments (tenant_id, uploader_user_id);
