CREATE TABLE notifications (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    type VARCHAR(60) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    target_url VARCHAR(1000),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uk_notifications_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_notification_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id),
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (tenant_id, recipient_user_id)
            REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_notification_target_url
        CHECK (target_url IS NULL OR (target_url LIKE '/%' AND target_url NOT LIKE '//%'))
);

CREATE INDEX idx_notification_recipient_created
    ON notifications (tenant_id, recipient_user_id, created_at DESC);

CREATE INDEX idx_notification_recipient_unread
    ON notifications (tenant_id, recipient_user_id, read_at, created_at DESC);
