ALTER TABLE task_comments
    ADD COLUMN parent_comment_id UUID;

ALTER TABLE task_comments
    ADD COLUMN reply_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE task_comments
    ADD COLUMN pinned_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE task_comments
    ADD COLUMN pinned_by_user_id UUID;

ALTER TABLE task_comments
    ADD CONSTRAINT uk_task_comments_thread_scope
        UNIQUE (tenant_id, project_id, task_id, id);

ALTER TABLE task_comments
    ADD CONSTRAINT fk_task_comment_parent
        FOREIGN KEY (tenant_id, project_id, task_id, parent_comment_id)
            REFERENCES task_comments (tenant_id, project_id, task_id, id);

ALTER TABLE task_comments
    ADD CONSTRAINT fk_task_comment_pinned_by
        FOREIGN KEY (tenant_id, pinned_by_user_id)
            REFERENCES app_users (tenant_id, id);

ALTER TABLE task_comments
    ADD CONSTRAINT ck_task_comment_reply_count
        CHECK (reply_count >= 0);

ALTER TABLE task_comments
    ADD CONSTRAINT ck_task_comment_thread_depth
        CHECK (parent_comment_id IS NULL OR reply_count = 0);

ALTER TABLE task_comments
    ADD CONSTRAINT ck_task_comment_pin_pair
        CHECK (
            (pinned_at IS NULL AND pinned_by_user_id IS NULL)
            OR (pinned_at IS NOT NULL AND pinned_by_user_id IS NOT NULL)
        );

ALTER TABLE task_comments
    ADD CONSTRAINT ck_task_comment_reply_not_pinned
        CHECK (parent_comment_id IS NULL OR pinned_at IS NULL);

CREATE INDEX idx_task_comment_parent_created
    ON task_comments (tenant_id, project_id, task_id, parent_comment_id, created_at);

CREATE INDEX idx_task_comment_pinned
    ON task_comments (tenant_id, project_id, task_id, pinned_at DESC);
