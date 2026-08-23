CREATE TABLE subscription_plan_usage_limits (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL,
    metric_code VARCHAR(64) NOT NULL,
    period_limit BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_subscription_plan_usage_limit_plan
        FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE,
    CONSTRAINT uk_subscription_plan_usage_limit_metric
        UNIQUE (plan_id, metric_code),
    CONSTRAINT ck_subscription_plan_usage_limit_non_negative
        CHECK (period_limit >= 0)
);

CREATE INDEX idx_subscription_plan_usage_limit_plan
    ON subscription_plan_usage_limits(plan_id);
