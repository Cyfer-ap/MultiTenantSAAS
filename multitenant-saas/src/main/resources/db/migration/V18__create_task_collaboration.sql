ALTER TABLE project_tasks
    ADD CONSTRAINT uk_project_tasks_tenant_project_id
        UNIQUE (tenant_id, project_id, id);

CREATE TABLE task_comments (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    body VARCHAR(4000),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_task_comments PRIMARY KEY (id),
    CONSTRAINT uk_task_comments_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_task_comment_task
        FOREIGN KEY (tenant_id, project_id, task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT fk_task_comment_author
        FOREIGN KEY (tenant_id, author_user_id)
            REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_task_comment_deleted_body
        CHECK ((deleted = FALSE AND body IS NOT NULL) OR deleted = TRUE)
);

CREATE INDEX idx_task_comment_task_created
    ON task_comments (tenant_id, project_id, task_id, created_at DESC);

CREATE INDEX idx_task_comment_author
    ON task_comments (tenant_id, author_user_id);

CREATE TABLE task_comment_mentions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    comment_id UUID NOT NULL,
    mentioned_user_id UUID NOT NULL,

    CONSTRAINT pk_task_comment_mentions PRIMARY KEY (id),
    CONSTRAINT uk_task_comment_mention UNIQUE (comment_id, mentioned_user_id),
    CONSTRAINT fk_task_comment_mention_comment
        FOREIGN KEY (tenant_id, comment_id)
            REFERENCES task_comments (tenant_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_task_comment_mention_user
        FOREIGN KEY (tenant_id, mentioned_user_id)
            REFERENCES app_users (tenant_id, id)
);

CREATE INDEX idx_task_comment_mention_user
    ON task_comment_mentions (tenant_id, mentioned_user_id);

CREATE TABLE task_activities (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    activity_type VARCHAR(40) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_task_activities PRIMARY KEY (id),
    CONSTRAINT fk_task_activity_task
        FOREIGN KEY (tenant_id, project_id, task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT fk_task_activity_actor
        FOREIGN KEY (tenant_id, actor_user_id)
            REFERENCES app_users (tenant_id, id)
);

CREATE INDEX idx_task_activity_task_created
    ON task_activities (tenant_id, project_id, task_id, created_at DESC);

INSERT INTO task_activities (
    id,
    tenant_id,
    project_id,
    task_id,
    actor_user_id,
    activity_type,
    summary,
    created_at
)
SELECT
    id,
    tenant_id,
    project_id,
    id,
    created_by_user_id,
    'TASK_CREATED',
    'Task created',
    created_at
FROM project_tasks;
