CREATE TABLE form_definitions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    workflow_id UUID,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    definition_version INTEGER NOT NULL,
    task_title_field_key VARCHAR(64) NOT NULL,
    task_description_field_key VARCHAR(64),
    task_due_date_field_key VARCHAR(64),
    task_priority VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_form_definitions PRIMARY KEY (id),
    CONSTRAINT uk_form_definition_name UNIQUE (tenant_id, project_id, normalized_name),
    CONSTRAINT uk_form_definition_scope_id UNIQUE (tenant_id, project_id, id),
    CONSTRAINT fk_form_definition_project
        FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id),
    CONSTRAINT fk_form_definition_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT fk_form_definition_workflow
        FOREIGN KEY (tenant_id, workflow_id) REFERENCES workflow_definitions (tenant_id, id),
    CONSTRAINT ck_form_definition_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED')),
    CONSTRAINT ck_form_definition_version CHECK (definition_version >= 1),
    CONSTRAINT ck_form_definition_priority
        CHECK (task_priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX idx_form_definitions_project_status_name
    ON form_definitions (tenant_id, project_id, status, name);

CREATE TABLE form_fields (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    form_id UUID NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    label VARCHAR(120) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    required BOOLEAN NOT NULL,
    options_json VARCHAR(8000),
    position_index INTEGER NOT NULL,

    CONSTRAINT pk_form_fields PRIMARY KEY (id),
    CONSTRAINT uk_form_field_key UNIQUE (tenant_id, project_id, form_id, field_key),
    CONSTRAINT uk_form_field_position UNIQUE (tenant_id, project_id, form_id, position_index),
    CONSTRAINT fk_form_field_definition
        FOREIGN KEY (tenant_id, project_id, form_id)
            REFERENCES form_definitions (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT ck_form_field_type
        CHECK (field_type IN ('TEXT', 'TEXTAREA', 'NUMBER', 'DATE', 'BOOLEAN', 'SELECT')),
    CONSTRAINT ck_form_field_position CHECK (position_index BETWEEN 0 AND 29)
);

CREATE INDEX idx_form_fields_definition
    ON form_fields (tenant_id, project_id, form_id, position_index);

CREATE TABLE form_submissions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    form_id UUID NOT NULL,
    definition_version INTEGER NOT NULL,
    submitted_by_user_id UUID NOT NULL,
    payload_json VARCHAR(12000) NOT NULL,
    created_task_id UUID NOT NULL,
    validation_context VARCHAR(1000) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_form_submissions PRIMARY KEY (id),
    CONSTRAINT fk_form_submission_definition
        FOREIGN KEY (tenant_id, project_id, form_id)
            REFERENCES form_definitions (tenant_id, project_id, id),
    CONSTRAINT fk_form_submission_actor
        FOREIGN KEY (tenant_id, submitted_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT fk_form_submission_task
        FOREIGN KEY (tenant_id, project_id, created_task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT ck_form_submission_version CHECK (definition_version >= 1)
);

CREATE INDEX idx_form_submissions_history
    ON form_submissions (tenant_id, project_id, form_id, submitted_at DESC);
