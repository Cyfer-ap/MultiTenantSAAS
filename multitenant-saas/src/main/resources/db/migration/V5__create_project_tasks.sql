CREATE TABLE project_tasks (
                               id UUID NOT NULL,
                               tenant_id UUID NOT NULL,
                               project_id UUID NOT NULL,
                               created_by_user_id UUID NOT NULL,
                               assignee_user_id UUID,

                               title VARCHAR(200) NOT NULL,
                               description VARCHAR(4000),

                               status VARCHAR(30) NOT NULL,
                               priority VARCHAR(30) NOT NULL,

                               due_at TIMESTAMP WITH TIME ZONE,
                               completed_at TIMESTAMP WITH TIME ZONE,

                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                               CONSTRAINT pk_project_tasks
                                   PRIMARY KEY (id),

                               CONSTRAINT fk_project_task_tenant
                                   FOREIGN KEY (tenant_id)
                                       REFERENCES tenants (id),

                               CONSTRAINT fk_project_task_project
                                   FOREIGN KEY (project_id)
                                       REFERENCES projects (id),

                               CONSTRAINT fk_project_task_created_by
                                   FOREIGN KEY (created_by_user_id)
                                       REFERENCES app_users (id),

                               CONSTRAINT fk_project_task_assignee
                                   FOREIGN KEY (assignee_user_id)
                                       REFERENCES app_users (id)
);

CREATE INDEX idx_project_task_tenant
    ON project_tasks (tenant_id);

CREATE INDEX idx_project_task_project
    ON project_tasks (project_id);

CREATE INDEX idx_project_task_assignee
    ON project_tasks (assignee_user_id);

CREATE INDEX idx_project_task_project_status
    ON project_tasks (project_id, status);

CREATE INDEX idx_project_task_project_priority
    ON project_tasks (project_id, priority);

CREATE INDEX idx_project_task_due_at
    ON project_tasks (due_at);