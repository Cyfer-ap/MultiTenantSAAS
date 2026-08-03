ALTER TABLE app_users
    ADD CONSTRAINT uk_app_user_tenant_id
        UNIQUE (tenant_id, id);