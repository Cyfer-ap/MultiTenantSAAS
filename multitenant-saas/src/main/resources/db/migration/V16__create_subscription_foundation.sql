CREATE TABLE subscription_plans (
    id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    billing_interval VARCHAR(20) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    max_users INTEGER,
    max_projects INTEGER,
    max_storage_mb BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_subscription_plans
        PRIMARY KEY (id),

    CONSTRAINT uk_subscription_plan_code
        UNIQUE (code),

    CONSTRAINT ck_subscription_plan_price
        CHECK (price >= 0),

    CONSTRAINT ck_subscription_plan_max_users
        CHECK (max_users IS NULL OR max_users >= 0),

    CONSTRAINT ck_subscription_plan_max_projects
        CHECK (max_projects IS NULL OR max_projects >= 0),

    CONSTRAINT ck_subscription_plan_max_storage
        CHECK (max_storage_mb IS NULL OR max_storage_mb >= 0),

    CONSTRAINT ck_subscription_plan_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    CONSTRAINT ck_subscription_plan_interval
        CHECK (billing_interval IN ('MONTHLY', 'YEARLY'))
);

CREATE INDEX idx_subscription_plan_status_price
    ON subscription_plans (status, price);

CREATE TABLE tenant_subscriptions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_tenant_subscriptions
        PRIMARY KEY (id),

    CONSTRAINT uk_tenant_subscription_tenant
        UNIQUE (tenant_id),

    CONSTRAINT fk_tenant_subscription_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants (id),

    CONSTRAINT fk_tenant_subscription_plan
        FOREIGN KEY (plan_id)
        REFERENCES subscription_plans (id),

    CONSTRAINT ck_tenant_subscription_status
        CHECK (
            status IN (
                'TRIALING',
                'ACTIVE',
                'PAST_DUE',
                'CANCELLED',
                'EXPIRED'
            )
        ),

    CONSTRAINT ck_tenant_subscription_period
        CHECK (current_period_end > current_period_start),

    CONSTRAINT ck_tenant_subscription_trial
        CHECK (
            trial_ends_at IS NULL
            OR (
                trial_ends_at >= current_period_start
                AND trial_ends_at <= current_period_end
            )
        )
);

CREATE INDEX idx_tenant_subscription_plan_status
    ON tenant_subscriptions (plan_id, status);

CREATE INDEX idx_tenant_subscription_period_end
    ON tenant_subscriptions (status, current_period_end);
