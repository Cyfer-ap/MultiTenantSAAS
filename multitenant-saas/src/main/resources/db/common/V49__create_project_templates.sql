CREATE TABLE project_templates (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) NOT NULL,
    project_name_seed VARCHAR(150) NOT NULL,
    project_description VARCHAR(2000),
    initial_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_project_templates PRIMARY KEY (id),
    CONSTRAINT uk_project_template_name UNIQUE (tenant_id, normalized_name),
    CONSTRAINT uk_project_template_scope_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_project_template_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_project_template_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_project_template_initial_status
        CHECK (initial_status IN ('PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'))
);

CREATE INDEX idx_project_templates_tenant_name
    ON project_templates (tenant_id, name);

CREATE TABLE project_template_tasks (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    template_id UUID NOT NULL,
    position_index INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    priority VARCHAR(30) NOT NULL,
    due_offset_minutes BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_project_template_tasks PRIMARY KEY (id),
    CONSTRAINT uk_project_template_task_position
        UNIQUE (tenant_id, template_id, position_index),
    CONSTRAINT fk_project_template_task_template
        FOREIGN KEY (tenant_id, template_id)
            REFERENCES project_templates (tenant_id, id)
            ON DELETE CASCADE,
    CONSTRAINT ck_project_template_task_position
        CHECK (position_index BETWEEN 0 AND 49),
    CONSTRAINT ck_project_template_task_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT ck_project_template_task_due_offset
        CHECK (due_offset_minutes IS NULL OR due_offset_minutes BETWEEN 0 AND 525600)
);

CREATE INDEX idx_project_template_tasks_template
    ON project_template_tasks (tenant_id, template_id, position_index);
