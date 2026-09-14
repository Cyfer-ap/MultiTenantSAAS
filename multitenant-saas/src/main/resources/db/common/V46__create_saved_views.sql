CREATE TABLE saved_views (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    target VARCHAR(32) NOT NULL,
    context_id UUID,
    definition_json VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_saved_views PRIMARY KEY (id),
    CONSTRAINT fk_saved_view_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id),
    CONSTRAINT fk_saved_view_user
        FOREIGN KEY (tenant_id, user_id)
            REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_saved_view_target
        CHECK (target IN ('MY_WORK', 'PROJECT_TASKS')),
    CONSTRAINT ck_saved_view_context
        CHECK (
            (target = 'MY_WORK' AND context_id IS NULL)
            OR (target = 'PROJECT_TASKS' AND context_id IS NOT NULL)
        )
);

CREATE INDEX idx_saved_views_scope
    ON saved_views (tenant_id, user_id, target, context_id, name);
