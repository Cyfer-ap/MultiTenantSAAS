CREATE TABLE notification_deliveries (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    notification_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    lease_token UUID,
    last_error VARCHAR(1000),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_notification_deliveries PRIMARY KEY (id),
    CONSTRAINT fk_notification_delivery_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_notification_delivery_notification
        FOREIGN KEY (tenant_id, notification_id)
            REFERENCES notifications (tenant_id, id),
    CONSTRAINT uk_notification_delivery_channel
        UNIQUE (tenant_id, notification_id, channel),
    CONSTRAINT ck_notification_delivery_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_notification_delivery_ready
    ON notification_deliveries (status, next_attempt_at, created_at);

CREATE INDEX idx_notification_delivery_processing
    ON notification_deliveries (status, processing_started_at);
