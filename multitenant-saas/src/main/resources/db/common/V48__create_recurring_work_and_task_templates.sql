CREATE TABLE recurring_task_definitions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    assignee_user_id UUID,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    priority VARCHAR(30) NOT NULL,
    cadence VARCHAR(20) NOT NULL,
    interval_count INTEGER NOT NULL,
    zone_id VARCHAR(80) NOT NULL,
    next_occurrence_at TIMESTAMP WITH TIME ZONE NOT NULL,
    due_offset_minutes BIGINT,
    end_at TIMESTAMP WITH TIME ZONE,
    max_occurrences INTEGER,
    generated_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    last_error VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_recurring_task_definitions PRIMARY KEY (id),
    CONSTRAINT uk_recurring_task_definition_scope_id UNIQUE (tenant_id, project_id, id),
    CONSTRAINT fk_recurring_task_definition_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_recurring_task_definition_project
        FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id),
    CONSTRAINT fk_recurring_task_definition_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT fk_recurring_task_definition_assignee
        FOREIGN KEY (tenant_id, assignee_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_recurring_task_definition_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_recurring_task_definition_cadence
        CHECK (cadence IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_recurring_task_definition_interval
        CHECK (interval_count BETWEEN 1 AND 52),
    CONSTRAINT ck_recurring_task_definition_due_offset
        CHECK (due_offset_minutes IS NULL OR due_offset_minutes BETWEEN 0 AND 525600),
    CONSTRAINT ck_recurring_task_definition_max_occurrences
        CHECK (max_occurrences IS NULL OR max_occurrences BETWEEN 1 AND 10000),
    CONSTRAINT ck_recurring_task_definition_generated_count
        CHECK (generated_count >= 0),
    CONSTRAINT ck_recurring_task_definition_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT ck_recurring_task_definition_end
        CHECK (end_at IS NULL OR end_at >= next_occurrence_at OR status = 'ENDED')
);

CREATE INDEX idx_recurring_task_definition_project
    ON recurring_task_definitions (tenant_id, project_id, created_at DESC);

CREATE INDEX idx_recurring_task_definition_due
    ON recurring_task_definitions (status, next_occurrence_at);

CREATE TABLE recurring_task_occurrences (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    definition_id UUID NOT NULL,
    scheduled_for TIMESTAMP WITH TIME ZONE NOT NULL,
    task_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_recurring_task_occurrences PRIMARY KEY (id),
    CONSTRAINT uk_recurring_task_occurrence UNIQUE (definition_id, scheduled_for),
    CONSTRAINT fk_recurring_task_occurrence_definition
        FOREIGN KEY (tenant_id, project_id, definition_id)
            REFERENCES recurring_task_definitions (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_recurring_task_occurrence_task
        FOREIGN KEY (tenant_id, project_id, task_id)
            REFERENCES project_tasks (tenant_id, project_id, id)
);

CREATE INDEX idx_recurring_task_occurrence_definition
    ON recurring_task_occurrences (tenant_id, project_id, definition_id, scheduled_for DESC);

CREATE TABLE project_task_templates (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    assignee_user_id UUID,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,
    task_title VARCHAR(200) NOT NULL,
    task_description VARCHAR(4000),
    priority VARCHAR(30) NOT NULL,
    due_offset_minutes BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_project_task_templates PRIMARY KEY (id),
    CONSTRAINT uk_project_task_template_name
        UNIQUE (tenant_id, project_id, normalized_name),
    CONSTRAINT uk_project_task_template_scope_id
        UNIQUE (tenant_id, project_id, id),
    CONSTRAINT fk_project_task_template_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_project_task_template_project
        FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id),
    CONSTRAINT fk_project_task_template_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT fk_project_task_template_assignee
        FOREIGN KEY (tenant_id, assignee_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_project_task_template_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_project_task_template_due_offset
        CHECK (due_offset_minutes IS NULL OR due_offset_minutes BETWEEN 0 AND 525600)
);

CREATE INDEX idx_project_task_templates_project
    ON project_task_templates (tenant_id, project_id, name);
