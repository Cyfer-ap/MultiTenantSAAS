CREATE TABLE tenant_subscription_history (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    tenant_name_snapshot VARCHAR(200) NOT NULL,
    plan_id UUID NOT NULL,
    plan_code_snapshot VARCHAR(60) NOT NULL,
    plan_name_snapshot VARCHAR(150) NOT NULL,
    plan_description_snapshot VARCHAR(500),
    billing_interval_snapshot VARCHAR(20) NOT NULL,
    price_snapshot DECIMAL(19, 2) NOT NULL,
    currency_snapshot VARCHAR(3) NOT NULL,
    max_users_snapshot INTEGER,
    max_projects_snapshot INTEGER,
    max_storage_mb_snapshot BIGINT,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN NOT NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    billing_provider VARCHAR(32),
    provider_subscription_id VARCHAR(255),
    provider_event_created_at TIMESTAMP WITH TIME ZONE,
    event_type VARCHAR(40) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_subscription_history_status
        CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_subscription_history_interval
        CHECK (billing_interval_snapshot IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscription_history_event_type
        CHECK (event_type IN (
            'MIGRATED_CURRENT_STATE',
            'STARTED',
            'PLAN_CHANGED',
            'LIFECYCLE_UPDATED',
            'PROVIDER_SYNCHRONIZED',
            'PROVIDER_RECONCILED'
        ))
);

CREATE INDEX idx_subscription_history_tenant_recorded
    ON tenant_subscription_history (tenant_id, recorded_at);

CREATE INDEX idx_subscription_history_subscription_recorded
    ON tenant_subscription_history (subscription_id, recorded_at);

CREATE INDEX idx_subscription_history_provider_ref
    ON tenant_subscription_history (billing_provider, provider_subscription_id);
