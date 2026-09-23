ALTER TABLE external_access_grant_capabilities
    DROP CONSTRAINT ck_external_access_capability;

ALTER TABLE external_access_grant_capabilities
    ADD CONSTRAINT ck_external_access_capability
        CHECK (capability IN ('PROJECT_READ', 'TASK_READ', 'TASK_COMMENT_CREATE'));

ALTER TABLE task_comments
    ADD COLUMN author_type VARCHAR(30) NOT NULL DEFAULT 'TENANT_USER';

ALTER TABLE task_comments
    ALTER COLUMN author_user_id DROP NOT NULL;

ALTER TABLE task_comments
    ADD COLUMN external_access_grant_id UUID;

ALTER TABLE task_comments
    ADD COLUMN external_guest_name VARCHAR(150);

ALTER TABLE task_comments
    ADD COLUMN external_guest_email VARCHAR(150);

ALTER TABLE task_comments
    ADD CONSTRAINT ck_task_comment_author_source
        CHECK (
            (
                author_type = 'TENANT_USER'
                AND author_user_id IS NOT NULL
                AND external_access_grant_id IS NULL
                AND external_guest_name IS NULL
                AND external_guest_email IS NULL
            )
            OR
            (
                author_type = 'EXTERNAL_GUEST'
                AND author_user_id IS NULL
                AND external_access_grant_id IS NOT NULL
                AND external_guest_name IS NOT NULL
                AND external_guest_email IS NOT NULL
            )
        );

CREATE INDEX idx_task_comment_external_grant_created
    ON task_comments (
        tenant_id,
        project_id,
        external_access_grant_id,
        created_at
    )
    WHERE external_access_grant_id IS NOT NULL;
