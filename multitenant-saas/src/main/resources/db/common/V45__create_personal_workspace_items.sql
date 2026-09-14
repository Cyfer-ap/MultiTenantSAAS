CREATE TABLE personal_workspace_items (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    resource_id UUID NOT NULL,
    favorite_at TIMESTAMP WITH TIME ZONE,
    last_viewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_personal_workspace_items PRIMARY KEY (id),
    CONSTRAINT uk_personal_workspace_item_resource
        UNIQUE (tenant_id, user_id, resource_type, resource_id),
    CONSTRAINT fk_personal_workspace_item_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id),
    CONSTRAINT fk_personal_workspace_item_user
        FOREIGN KEY (tenant_id, user_id)
            REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_personal_workspace_item_type
        CHECK (resource_type IN ('PROJECT', 'TASK')),
    CONSTRAINT ck_personal_workspace_item_signal
        CHECK (favorite_at IS NOT NULL OR last_viewed_at IS NOT NULL)
);

CREATE INDEX idx_personal_workspace_favorites
    ON personal_workspace_items (tenant_id, user_id, favorite_at DESC);

CREATE INDEX idx_personal_workspace_recent
    ON personal_workspace_items (tenant_id, user_id, last_viewed_at DESC);
