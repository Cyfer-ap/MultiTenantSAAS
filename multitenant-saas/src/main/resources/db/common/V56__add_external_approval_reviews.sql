ALTER TABLE external_access_grant_capabilities
    DROP CONSTRAINT ck_external_access_capability;

ALTER TABLE external_access_grant_capabilities
    ADD CONSTRAINT ck_external_access_capability
        CHECK (
            capability IN (
                'PROJECT_READ',
                'TASK_READ',
                'TASK_COMMENT_CREATE',
                'APPROVAL_REVIEW'
            )
        );

ALTER TABLE approval_request_stages
    ADD COLUMN decision_actor_type VARCHAR(30);

ALTER TABLE approval_request_stages
    ADD COLUMN external_decided_by_grant_id UUID;

ALTER TABLE approval_request_stages
    ADD COLUMN external_decided_by_name VARCHAR(150);

ALTER TABLE approval_request_stages
    ADD COLUMN external_decided_by_email VARCHAR(150);

UPDATE approval_request_stages
SET decision_actor_type = 'TENANT_USER'
WHERE decided_by_user_id IS NOT NULL;

ALTER TABLE approval_request_stages
    ADD CONSTRAINT fk_approval_request_stage_external_decider
        FOREIGN KEY (tenant_id, project_id, external_decided_by_grant_id)
            REFERENCES external_access_grants (tenant_id, project_id, id);

ALTER TABLE approval_request_stages
    ADD CONSTRAINT ck_approval_request_stage_decision_actor
        CHECK (
            (
                status IN ('WAITING', 'PENDING')
                AND decision_actor_type IS NULL
                AND decided_by_user_id IS NULL
                AND external_decided_by_grant_id IS NULL
                AND external_decided_by_name IS NULL
                AND external_decided_by_email IS NULL
                AND decided_at IS NULL
            )
            OR
            (
                status IN ('APPROVED', 'REJECTED')
                AND decision_actor_type = 'TENANT_USER'
                AND decided_by_user_id IS NOT NULL
                AND external_decided_by_grant_id IS NULL
                AND external_decided_by_name IS NULL
                AND external_decided_by_email IS NULL
                AND decided_at IS NOT NULL
            )
            OR
            (
                status IN ('APPROVED', 'REJECTED')
                AND decision_actor_type = 'EXTERNAL_GUEST'
                AND decided_by_user_id IS NULL
                AND external_decided_by_grant_id IS NOT NULL
                AND external_decided_by_name IS NOT NULL
                AND external_decided_by_email IS NOT NULL
                AND decided_at IS NOT NULL
            )
        );

CREATE TABLE approval_request_stage_external_reviewers (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    request_id UUID NOT NULL,
    request_stage_id UUID NOT NULL,
    external_access_grant_id UUID NOT NULL,
    guest_name_snapshot VARCHAR(150) NOT NULL,
    guest_email_snapshot VARCHAR(150) NOT NULL,
    assigned_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_approval_request_stage_external_reviewers PRIMARY KEY (id),
    CONSTRAINT uk_approval_request_stage_external_reviewer
        UNIQUE (
            tenant_id,
            project_id,
            request_id,
            request_stage_id,
            external_access_grant_id
        ),
    CONSTRAINT fk_approval_external_reviewer_stage
        FOREIGN KEY (tenant_id, project_id, request_id, request_stage_id)
            REFERENCES approval_request_stages (tenant_id, project_id, request_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_approval_external_reviewer_grant
        FOREIGN KEY (tenant_id, project_id, external_access_grant_id)
            REFERENCES external_access_grants (tenant_id, project_id, id),
    CONSTRAINT fk_approval_external_reviewer_assigner
        FOREIGN KEY (tenant_id, assigned_by_user_id)
            REFERENCES app_users (tenant_id, id)
);

CREATE INDEX idx_approval_external_reviewer_grant
    ON approval_request_stage_external_reviewers (
        tenant_id,
        project_id,
        external_access_grant_id,
        request_id
    );
