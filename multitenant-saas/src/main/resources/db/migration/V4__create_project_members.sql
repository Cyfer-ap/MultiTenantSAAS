CREATE TABLE project_members (
                                 id UUID NOT NULL,
                                 project_id UUID NOT NULL,
                                 user_id UUID NOT NULL,
                                 assigned_by_user_id UUID NOT NULL,

                                 role VARCHAR(30) NOT NULL,

                                 assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT pk_project_members
                                     PRIMARY KEY (id),

                                 CONSTRAINT uk_project_member
                                     UNIQUE (project_id, user_id),

                                 CONSTRAINT fk_project_member_project
                                     FOREIGN KEY (project_id)
                                         REFERENCES projects (id),

                                 CONSTRAINT fk_project_member_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES app_users (id),

                                 CONSTRAINT fk_project_member_assigned_by
                                     FOREIGN KEY (assigned_by_user_id)
                                         REFERENCES app_users (id)
);

CREATE INDEX idx_project_member_project
    ON project_members (project_id);

CREATE INDEX idx_project_member_user
    ON project_members (user_id);

CREATE INDEX idx_project_member_project_role
    ON project_members (project_id, role);

-- Backfill existing projects:
-- the project creator becomes the initial project lead.
INSERT INTO project_members (
    id,
    project_id,
    user_id,
    assigned_by_user_id,
    role,
    assigned_at,
    updated_at
)
SELECT
    project.id,
    project.id,
    project.created_by_user_id,
    project.created_by_user_id,
    'PROJECT_LEAD',
    project.created_at,
    project.updated_at
FROM projects project;