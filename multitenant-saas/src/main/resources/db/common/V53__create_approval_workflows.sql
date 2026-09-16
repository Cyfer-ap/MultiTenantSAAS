ALTER TABLE workflow_edges DROP CONSTRAINT ck_workflow_edge_branch;
ALTER TABLE workflow_edges
    ADD CONSTRAINT ck_workflow_edge_branch
    CHECK (branch_type IN ('DEFAULT', 'TRUE', 'FALSE', 'APPROVED', 'REJECTED'));

ALTER TABLE workflow_executions DROP CONSTRAINT ck_workflow_execution_status;
ALTER TABLE workflow_executions
    ADD CONSTRAINT ck_workflow_execution_status
    CHECK (status IN ('RUNNING', 'WAITING_APPROVAL', 'SUCCEEDED', 'FAILED', 'SKIPPED'));

CREATE TABLE approval_definitions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    definition_version INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_approval_definitions PRIMARY KEY (id),
    CONSTRAINT uk_approval_definition_name UNIQUE (tenant_id, project_id, normalized_name),
    CONSTRAINT uk_approval_definition_scope_id UNIQUE (tenant_id, project_id, id),
    CONSTRAINT fk_approval_definition_project
        FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id),
    CONSTRAINT fk_approval_definition_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_approval_definition_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED')),
    CONSTRAINT ck_approval_definition_version CHECK (definition_version >= 1)
);

CREATE INDEX idx_approval_definitions_project_status_name
    ON approval_definitions (tenant_id, project_id, status, name);

CREATE TABLE approval_stages (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    definition_id UUID NOT NULL,
    stage_key VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    position_index INTEGER NOT NULL,
    allow_requester_approval BOOLEAN NOT NULL,

    CONSTRAINT pk_approval_stages PRIMARY KEY (id),
    CONSTRAINT uk_approval_stage_key UNIQUE (tenant_id, project_id, definition_id, stage_key),
    CONSTRAINT uk_approval_stage_position UNIQUE (tenant_id, project_id, definition_id, position_index),
    CONSTRAINT uk_approval_stage_scope_id UNIQUE (tenant_id, project_id, definition_id, id),
    CONSTRAINT fk_approval_stage_definition
        FOREIGN KEY (tenant_id, project_id, definition_id)
            REFERENCES approval_definitions (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT ck_approval_stage_position CHECK (position_index BETWEEN 0 AND 9)
);

CREATE TABLE approval_stage_reviewers (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    definition_id UUID NOT NULL,
    stage_id UUID NOT NULL,
    reviewer_user_id UUID NOT NULL,

    CONSTRAINT pk_approval_stage_reviewers PRIMARY KEY (id),
    CONSTRAINT uk_approval_stage_reviewer
        UNIQUE (tenant_id, project_id, definition_id, stage_id, reviewer_user_id),
    CONSTRAINT fk_approval_stage_reviewer_stage
        FOREIGN KEY (tenant_id, project_id, definition_id, stage_id)
            REFERENCES approval_stages (tenant_id, project_id, definition_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_approval_stage_reviewer_user
        FOREIGN KEY (tenant_id, reviewer_user_id) REFERENCES app_users (tenant_id, id)
);

CREATE TABLE approval_requests (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    definition_id UUID NOT NULL,
    definition_version INTEGER NOT NULL,
    workflow_id UUID NOT NULL,
    workflow_version INTEGER NOT NULL,
    workflow_execution_id UUID NOT NULL,
    workflow_node_key VARCHAR(64) NOT NULL,
    task_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_stage_index INTEGER NOT NULL,
    approved_next_node_key VARCHAR(64),
    rejected_next_node_key VARCHAR(64),
    row_version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_approval_requests PRIMARY KEY (id),
    CONSTRAINT uk_approval_request_scope_id UNIQUE (tenant_id, project_id, id),
    CONSTRAINT uk_approval_request_execution_node
        UNIQUE (tenant_id, workflow_execution_id, workflow_node_key),
    CONSTRAINT fk_approval_request_definition
        FOREIGN KEY (tenant_id, project_id, definition_id)
            REFERENCES approval_definitions (tenant_id, project_id, id),
    CONSTRAINT fk_approval_request_workflow
        FOREIGN KEY (tenant_id, workflow_id) REFERENCES workflow_definitions (tenant_id, id),
    CONSTRAINT fk_approval_request_execution
        FOREIGN KEY (workflow_execution_id) REFERENCES workflow_executions (id),
    CONSTRAINT fk_approval_request_task
        FOREIGN KEY (tenant_id, project_id, task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT fk_approval_request_actor
        FOREIGN KEY (tenant_id, actor_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_approval_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_approval_request_definition_version CHECK (definition_version >= 1),
    CONSTRAINT ck_approval_request_workflow_version CHECK (workflow_version >= 1),
    CONSTRAINT ck_approval_request_stage_index CHECK (current_stage_index BETWEEN 0 AND 9)
);

CREATE INDEX idx_approval_requests_project_status_created
    ON approval_requests (tenant_id, project_id, status, created_at DESC);

CREATE TABLE approval_request_stages (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    request_id UUID NOT NULL,
    stage_key VARCHAR(64) NOT NULL,
    stage_name VARCHAR(120) NOT NULL,
    position_index INTEGER NOT NULL,
    allow_requester_approval BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    decided_by_user_id UUID,
    decision_comment VARCHAR(1000),
    decided_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_approval_request_stages PRIMARY KEY (id),
    CONSTRAINT uk_approval_request_stage_position
        UNIQUE (tenant_id, project_id, request_id, position_index),
    CONSTRAINT uk_approval_request_stage_scope_id
        UNIQUE (tenant_id, project_id, request_id, id),
    CONSTRAINT fk_approval_request_stage_request
        FOREIGN KEY (tenant_id, project_id, request_id)
            REFERENCES approval_requests (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_approval_request_stage_decider
        FOREIGN KEY (tenant_id, decided_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_approval_request_stage_position CHECK (position_index BETWEEN 0 AND 9),
    CONSTRAINT ck_approval_request_stage_status
        CHECK (status IN ('WAITING', 'PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_approval_request_stages_request
    ON approval_request_stages (tenant_id, project_id, request_id, position_index);

CREATE TABLE approval_request_stage_reviewers (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    request_id UUID NOT NULL,
    request_stage_id UUID NOT NULL,
    reviewer_user_id UUID NOT NULL,

    CONSTRAINT pk_approval_request_stage_reviewers PRIMARY KEY (id),
    CONSTRAINT uk_approval_request_stage_reviewer
        UNIQUE (tenant_id, project_id, request_id, request_stage_id, reviewer_user_id),
    CONSTRAINT fk_approval_request_stage_reviewer_stage
        FOREIGN KEY (tenant_id, project_id, request_id, request_stage_id)
            REFERENCES approval_request_stages (tenant_id, project_id, request_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_approval_request_stage_reviewer_user
        FOREIGN KEY (tenant_id, reviewer_user_id) REFERENCES app_users (tenant_id, id)
);

CREATE INDEX idx_approval_request_reviewer_inbox
    ON approval_request_stage_reviewers (tenant_id, project_id, reviewer_user_id, request_id);
