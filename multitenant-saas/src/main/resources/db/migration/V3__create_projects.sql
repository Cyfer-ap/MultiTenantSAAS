CREATE TABLE projects (
                          id UUID NOT NULL,
                          tenant_id UUID NOT NULL,
                          created_by_user_id UUID NOT NULL,

                          name VARCHAR(150) NOT NULL,
                          description VARCHAR(2000),
                          status VARCHAR(30) NOT NULL,

                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                          CONSTRAINT pk_projects
                              PRIMARY KEY (id),

                          CONSTRAINT fk_project_tenant
                              FOREIGN KEY (tenant_id)
                                  REFERENCES tenants (id),

                          CONSTRAINT fk_project_created_by_user
                              FOREIGN KEY (created_by_user_id)
                                  REFERENCES app_users (id)
);

CREATE INDEX idx_project_tenant
    ON projects (tenant_id);

CREATE INDEX idx_project_tenant_status
    ON projects (tenant_id, status);

CREATE INDEX idx_project_tenant_created_at
    ON projects (tenant_id, created_at);