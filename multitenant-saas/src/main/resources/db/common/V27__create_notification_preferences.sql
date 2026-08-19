CREATE TABLE notification_preferences (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    type VARCHAR(60) NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_notification_preferences PRIMARY KEY (id),
    CONSTRAINT uk_notification_preferences_recipient_type
        UNIQUE (tenant_id, recipient_user_id, type),
    CONSTRAINT fk_notification_preference_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id),
    CONSTRAINT fk_notification_preference_recipient
        FOREIGN KEY (tenant_id, recipient_user_id)
            REFERENCES app_users (tenant_id, id)
);

CREATE INDEX idx_notification_preference_recipient
    ON notification_preferences (tenant_id, recipient_user_id);
