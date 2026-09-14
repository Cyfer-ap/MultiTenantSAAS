ALTER TABLE projects
    ADD CONSTRAINT uk_projects_tenant_id
        UNIQUE (tenant_id, id);

ALTER TABLE project_tasks
    ADD COLUMN parent_task_id UUID;

ALTER TABLE project_tasks
    ADD CONSTRAINT ck_project_task_parent_not_self
        CHECK (parent_task_id IS NULL OR parent_task_id <> id);

ALTER TABLE project_tasks
    ADD CONSTRAINT fk_project_task_parent
        FOREIGN KEY (tenant_id, project_id, parent_task_id)
            REFERENCES project_tasks (tenant_id, project_id, id);

CREATE INDEX idx_project_task_parent
    ON project_tasks (tenant_id, project_id, parent_task_id);

CREATE TABLE task_dependencies (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    blocking_task_id UUID NOT NULL,
    dependent_task_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_task_dependencies PRIMARY KEY (id),
    CONSTRAINT uk_task_dependency_edge
        UNIQUE (tenant_id, project_id, blocking_task_id, dependent_task_id),
    CONSTRAINT ck_task_dependency_not_self
        CHECK (blocking_task_id <> dependent_task_id),
    CONSTRAINT fk_task_dependency_blocking
        FOREIGN KEY (tenant_id, project_id, blocking_task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT fk_task_dependency_dependent
        FOREIGN KEY (tenant_id, project_id, dependent_task_id)
            REFERENCES project_tasks (tenant_id, project_id, id)
);

CREATE INDEX idx_task_dependency_blocking
    ON task_dependencies (tenant_id, project_id, blocking_task_id);

CREATE INDEX idx_task_dependency_dependent
    ON task_dependencies (tenant_id, project_id, dependent_task_id);

CREATE TABLE project_task_labels (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    name VARCHAR(60) NOT NULL,
    normalized_name VARCHAR(60) NOT NULL,
    color VARCHAR(7),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_project_task_labels PRIMARY KEY (id),
    CONSTRAINT uk_project_task_label_name
        UNIQUE (tenant_id, project_id, normalized_name),
    CONSTRAINT uk_project_task_label_scope_id
        UNIQUE (tenant_id, project_id, id),
    CONSTRAINT fk_project_task_label_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id),
    CONSTRAINT fk_project_task_label_project
        FOREIGN KEY (tenant_id, project_id)
            REFERENCES projects (tenant_id, id),
    CONSTRAINT ck_project_task_label_color
        CHECK (color IS NULL OR color LIKE '#______')
);

CREATE INDEX idx_project_task_labels_project
    ON project_task_labels (tenant_id, project_id, name);

CREATE TABLE project_task_label_assignments (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    label_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_project_task_label_assignments PRIMARY KEY (id),
    CONSTRAINT uk_project_task_label_assignment
        UNIQUE (tenant_id, project_id, task_id, label_id),
    CONSTRAINT fk_project_task_label_assignment_task
        FOREIGN KEY (tenant_id, project_id, task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT fk_project_task_label_assignment_label
        FOREIGN KEY (tenant_id, project_id, label_id)
            REFERENCES project_task_labels (tenant_id, project_id, id)
            ON DELETE CASCADE
);

CREATE INDEX idx_project_task_label_assignment_task
    ON project_task_label_assignments (tenant_id, project_id, task_id);

CREATE INDEX idx_project_task_label_assignment_label
    ON project_task_label_assignments (tenant_id, project_id, label_id);
